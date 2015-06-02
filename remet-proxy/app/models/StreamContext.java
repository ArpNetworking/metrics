/**
 * Copyright 2014 Brandon Arp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import akka.dispatch.ExecutionContexts;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import models.messages.Command;
import models.messages.Connect;
import models.messages.LogFileAppeared;
import models.messages.LogFileDisappeared;
import models.messages.LogLine;
import models.messages.LogsList;
import models.messages.LogsListRequest;
import models.messages.MetricReport;
import models.messages.MetricsList;
import models.messages.MetricsListRequest;
import models.messages.NewLog;
import models.messages.NewMetric;
import models.messages.Quit;
import play.libs.F.Callback0;
import play.mvc.WebSocket;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Actor responsible for holding the set of connected websockets and publishing
 * metrics to them.
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Mohammed Kamel (mkamel at groupon dot com)
 */
public class StreamContext extends UntypedActor {

    /**
     * Public constructor.
     *
     * @param metricsFactory Instance of <code>MetricsFactory</code>.
     */
    @Inject
    public StreamContext(final MetricsFactory metricsFactory) {
        _metricsFactory = metricsFactory;
        _metrics = metricsFactory.create();
        _instrument = context().system().scheduler().schedule(
                new FiniteDuration(0, TimeUnit.SECONDS), // Initial delay
                new FiniteDuration(500, TimeUnit.MILLISECONDS), // Interval
                getSelf(),
                "instrument",
                ExecutionContexts.global(),
                getSelf());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        LOGGER.trace()
                .setMessage("Received message")
                .addData("actor", self())
                .addData("data", message)
                .log();

        if ("instrument".equals(message)) {
            periodicInstrumentation();
        } else if (message instanceof Connect) {
            executeConnect((Connect) message);
        } else if (message instanceof MetricReport) {
            executeMetricReport((MetricReport) message);
        } else if (message instanceof LogLine) {
            executeLogLine((LogLine) message);
        } else if (message instanceof Quit) {
            executeQuit((Quit) message);
        } else if (message instanceof MetricsListRequest) {
            executeMetricsListRequest();
        } else if (message instanceof LogsListRequest) {
            executeLogsListRequest();
        } else if (message instanceof LogFileAppeared) {
            executeLogAdded((LogFileAppeared) message);
        } else if (message instanceof LogFileDisappeared) {
            executeLogRemoved((LogFileDisappeared) message);
        } else {
            _metrics.incrementCounter(UNKNOWN_COUNTER);
            LOGGER.warn()
                    .setMessage("Unsupported message")
                    .addData("actor", self())
                    .addData("data", message)
                    .log();
            unhandled(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        _instrument.cancel();
        super.postStop();
    }

    private void executeLogRemoved(final LogFileDisappeared message) {
        _metrics.incrementCounter(LOG_REMOVED_COUNTER);
        if (_logs.contains(message.getFile())) {
            _logs.remove(message.getFile());
            broadcast(message);
        }
    }

    private void executeLogAdded(final LogFileAppeared message) {
        _metrics.incrementCounter(LOG_ADDED_COUNTER);
        if (!_logs.contains(message.getFile())) {
            _logs.add(message.getFile());
            notifyNewLog(message.getFile());
        }
    }

    private void executeLogsListRequest() {
        _metrics.incrementCounter(METRICS_LIST_COUNTER);
        getSender().tell(new LogsList(_logs), getSelf());
    }

    private void executeLogLine(final LogLine message) {
        _metrics.incrementCounter(LOG_LINE_COUNTER);
        registerLog(message.getFile());
        broadcast(message);
    }

    private void executeConnect(final Connect message) {
        _metrics.incrementCounter(CONNECT_COUNTER);
        final WebSocket.Out<JsonNode> outputChannel = message.getOutputChannel();

        // Add the connection to the pool to receive future metric reports
        final ActorRef context = context().actorOf(ConnectionContext.props(_metricsFactory, message));
        _members.put(message.getOutputChannel(), context);

        message.getInputChannel().onClose(new Callback0() {
            @Override
            public void invoke() {
                LOGGER.info()
                        .setMessage("Connection closed")
                        .addData("actor", self())
                        .addData("channel", outputChannel)
                        .log();
                // Send a Quit message
                getSelf().tell(new Quit(outputChannel), ActorRef.noSender());
            }
        });

        message.getInputChannel().onMessage(m -> context.tell(new Command(m), ActorRef.noSender()));

        LOGGER.info()
                .setMessage("Connection opened")
                .addData("actor", self())
                .addData("context", context)
                .addData("channel", outputChannel)
                .log();
    }

    private void executeMetricReport(final MetricReport message) {
        _metrics.incrementCounter(METRIC_REPORT_COUNTER);

        // Ensure the metric is in the registry
        registerMetric(message.getService(), message.getMetric(), message.getStatistic());

        // Transmit the report to all members
        broadcast(message);
    }

    private void executeQuit(final Quit message) {
        _metrics.incrementCounter(QUIT_COUNTER);

        // Remove the connection from the pool
        _members.remove(message.getChannel()).tell(PoisonPill.getInstance(), getSelf());
    }

    private void executeMetricsListRequest() {
        _metrics.incrementCounter(METRICS_LIST_REQUEST);

        // Transmit a list of all registered metrics
        getSender().tell(new MetricsList(_serviceMetrics), getSelf());
    }

    private void registerLog(final Path logPath) {
        if (!_logs.contains(logPath)) {
            _logs.add(logPath);
            notifyNewLog(logPath);
        }
    }

    private void notifyNewLog(final Path logPath) {
        broadcast(new NewLog(logPath));
    }

    private void broadcast(final Object message) {
        for (final ActorRef ref : _members.values()) {
            ref.tell(message, getSelf());
        }
    }

    private void registerMetric(final String service, final String metric, final String statistic) {
        if (!_serviceMetrics.containsKey(service)) {
            _serviceMetrics.put(service, Maps.<String, Set<String>>newHashMap());
        }
        final Map<String, Set<String>> serviceMap = _serviceMetrics.get(service);

        if (!serviceMap.containsKey(metric)) {
            serviceMap.put(metric, Sets.<String>newHashSet());
        }
        final Set<String> statistics = serviceMap.get(metric);

        if (!statistics.contains(statistic)) {
            statistics.add(statistic);
            notifyNewMetric(service, metric, statistic);
        }
    }

    private void notifyNewMetric(final String service, final String metric, final String statistic) {
        final NewMetric newMetric = new NewMetric(service, metric, statistic);
        broadcast(newMetric);
    }

    private void periodicInstrumentation() {
        _metrics.close();
        _metrics = _metricsFactory.create();
    }


    private final Cancellable _instrument;
    private final Set<Path> _logs = Sets.newTreeSet();
    private final MetricsFactory _metricsFactory;
    private final Map<WebSocket.Out<JsonNode>, ActorRef> _members = Maps.newHashMap();
    private final Map<String, Map<String, Set<String>>> _serviceMetrics = Maps.newHashMap();

    private Metrics _metrics;

    private static final String METRIC_PREFIX = "Actors/StreamContext/";
    private static final String METRICS_LIST_REQUEST = METRIC_PREFIX + "MetricsListRequest";
    private static final String QUIT_COUNTER = METRIC_PREFIX + "Quit";
    private static final String METRIC_REPORT_COUNTER = METRIC_PREFIX + "MetricReport";
    private static final String CONNECT_COUNTER = METRIC_PREFIX + "Connect";
    private static final String LOG_LINE_COUNTER = METRIC_PREFIX + "LogLine";
    private static final String METRICS_LIST_COUNTER = METRIC_PREFIX + "MetricsList";
    private static final String LOG_ADDED_COUNTER = METRIC_PREFIX + "LogAdded";
    private static final String LOG_REMOVED_COUNTER = METRIC_PREFIX + "LogRemoved";
    private static final String UNKNOWN_COUNTER = METRIC_PREFIX + "UNKNOWN";
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamContext.class);
}
