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
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import models.messages.Command;
import models.messages.Connect;
import models.messages.MetricReport;
import models.messages.MetricsList;
import models.messages.MetricsListRequest;
import models.messages.NewMetric;
import models.messages.Quit;
import org.joda.time.DateTime;
import play.Logger;
import play.libs.Akka;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Actor responsible for holding the set of connected websockets and publishing
 * metrics to them.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StreamContext extends UntypedActor {

    /**
     * Method to register a client that has connected.
     *
     * @param in Incoming <code>WebSocket</code>.
     * @param out Outgoing <code>WebSocket</code>.
     * @throws Exception Thrown by <code>Await</code>.
     */
    public static void connect(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) throws Exception {

        Logger.info("Connection on creation: " + out);
        // Send the Join message to the room
        final Object result = Await.result(
                Patterns.ask(
                        DEFAULT_CONTEXT,
                        new Connect(out),
                        1000),
                Duration.apply(
                        1,
                        TimeUnit.SECONDS
                        )
                );

        if (result instanceof ActorRef) {
            //We were passed the reference to the child actor
            final ActorRef child = (ActorRef) result;

            in.onMessage(new Callback<JsonNode>() {
                @Override
                public void invoke(final JsonNode event) {
                    // Send the command to the child actor
                    child.tell(new Command(event), ActorRef.noSender());
                }
            });

            in.onClose(new Callback0() {
                @Override
                public void invoke() {
                    Logger.debug(String.format("Connection closed from channel %s", out));
                    // Send a Quit message
                    DEFAULT_CONTEXT.tell(new Quit(out), ActorRef.noSender());
                }
            });
        }
    }

    /**
     * Method to notify this actor that a MetricReport is ready to be sent to
     * interested clients.
     *
     * @param node Instance of <code>JsonNode</code> describing new metrics.
     */
    public static void reportMetrics(final JsonNode node) {
        Logger.info("got a metrics report");

        //TODO(barp): Map with a POJO mapper [MAI-184]
        final ArrayNode list = (ArrayNode) node;
        for (final JsonNode objNode : list) {
            final ObjectNode obj = (ObjectNode) objNode;
            final String service = obj.get("service").asText();
            final String host = obj.get("host").asText();
            final String statistic = obj.get("statistic").asText();
            final String metric = obj.get("metric").asText();
            final double value = obj.get("value").asDouble();
            final String periodStart = obj.get("periodStart").asText();
            final DateTime startTime = DateTime.parse(periodStart);

            DEFAULT_CONTEXT.tell(new MetricReport(service, host, statistic, metric, value, startTime), ActorRef.noSender());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        Logger.debug(String.format("received message %s", message));
        if (message instanceof Connect) {
            final Connect connect = (Connect) message;
            final ActorRef context = context().actorOf(ConnectionContext.props(connect.getChannel()));
            _members.put(connect.getChannel(), context);
            Logger.info(String.format("adding new channel to streaming context; channel=%s", connect.getChannel()));
            getSender().tell(context, getSelf());
        } else if (message instanceof MetricReport) {
            final MetricReport report = (MetricReport) message;
            registerMetric(report.getService(), report.getMetric(), report.getStatistic());
            broadcast(message);
        } else if (message instanceof Quit) {
            Logger.info(String.format("removing channel from streaming context; channel=%s", ((Quit) message).getChannel()));
            _members.remove(((Quit) message).getChannel()).tell(PoisonPill.getInstance(), getSelf());
        } else if (message instanceof MetricsListRequest) {
            getSender().tell(new MetricsList(_serviceMetrics), getSelf());
        } else {
            Logger.warn(String.format("Got an unexpected message; message=%s", message));
            unhandled(message);
        }
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

    private final Map<WebSocket.Out<JsonNode>, ActorRef> _members = Maps.newHashMap();
    private final Map<String, Map<String, Set<String>>> _serviceMetrics = Maps.newHashMap();

    private static final ActorRef DEFAULT_CONTEXT = Akka.system().actorOf(Props.create(StreamContext.class));

}
