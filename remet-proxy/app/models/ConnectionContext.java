/**
 * Copyright 2014 Groupon.com
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

import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import models.messages.Command;
import models.messages.MetricReport;
import models.messages.MetricsList;
import models.messages.MetricsListRequest;
import models.messages.NewMetric;
import play.Logger;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.Map;
import java.util.Set;

/**
 * Actor class to hold the state for a single connection.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConnectionContext extends UntypedActor {

    /**
     * Public constructor.
     *
     * @param connection client connection
     */
    public ConnectionContext(final WebSocket.Out<JsonNode> connection) {
        _connection = connection;
    }

    /**
     * Factory for creating a <code>Props</code> with strong typing.
     *
     * @param connection connection to bind to
     * @return a new Props object
     */
    public static Props props(final WebSocket.Out<JsonNode> connection) {
        return Props.create(ConnectionContext.class, connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (Logger.isDebugEnabled()) {
            Logger.debug(String.format("received message on channel; message=%s, channel=%s", message, _connection));
        }
        if (message instanceof Command) {
            //TODO(barp): Map with a POJO mapper [MAI-184]
            final Command command = (Command) message;
            final ObjectNode commandNode = (ObjectNode) command.getCommand();
            final String commandString = commandNode.get("command").asText();
            if (Command.COMMAND_GET_METRICS.equals(commandString)) {
                getContext().parent().tell(new MetricsListRequest(), getSelf());
            } else if (Command.COMMAND_HEARTBEAT.equals(commandString)) {
                _connection.write(OK_RESPONSE);
            } else if (Command.COMMAND_SUBSCRIBE.equals(commandString)) {
                final String service = commandNode.get("service").asText();
                final String metric = commandNode.get("metric").asText();
                final String statistic = commandNode.get("statistic").asText();
                subscribe(service, metric, statistic);
            } else if (Command.COMMAND_UNSUBSCRIBE.equals(commandString)) {
                final String service = commandNode.get("service").asText();
                final String metric = commandNode.get("metric").asText();
                final String statistic = commandNode.get("statistic").asText();
                unsubscribe(service, metric, statistic);
            } else {
                Logger.warn("channel command unsupported; command=" + commandString + ", channel=" + _connection);
            }
        } else if (message instanceof NewMetric) {
            //TODO(barp): Map with a POJO mapper [MAI-184]
            final NewMetric newMetric = (NewMetric) message;
            processNewMetric(newMetric);
        } else if (message instanceof MetricReport) {
            final MetricReport report = (MetricReport) message;
            processMetricReport(report);
        } else if (message instanceof MetricsList) {
            final MetricsList metricsList = (MetricsList) message;
            processMetricsList(metricsList);
        } else {
            Logger.warn("Got an unexpected message; message=" + message);
            unhandled(message);
        }
    }

    private void processNewMetric(final NewMetric newMetric) {
        final ObjectNode n = Json.newObject();
        n.put("service", newMetric.getService());
        n.put("metric", newMetric.getMetric());
        n.put("statistic", newMetric.getStatistic());
        sendCommand("newMetric", n);
    }

    private void processMetricReport(final MetricReport report) {
        final Map<String, Set<String>> metrics = _subscriptions.get(report.getService());
        if (metrics == null) {
            Logger.debug(String.format("Not sending MetricReport, service [%s] not found in _subscriptions", report.getService()));
            return;
        }
        final Set<String> stats = metrics.get(report.getMetric());
        if (stats == null) {
            Logger.debug(String.format("Not sending MetricReport, metric [%s] not found in _subscriptions", report.getMetric()));
            return;
        }
        if (!stats.contains(report.getStatistic())) {
            Logger.debug(String.format("Not sending MetricReport, statistic [%s] not found in _subscriptions", report.getStatistic()));
            return;
        }

        //TODO(barp): Map with a POJO mapper [MAI-184]
        final ObjectNode event = Json.newObject();
        event.put("server", report.getHost());
        event.put("service", report.getService());
        event.put("metric", report.getMetric());
        event.put("timestamp", report.getPeriodStart().getMillis());
        event.put("statistic", report.getStatistic());
        event.put("data", report.getValue());

        sendCommand("report", event);
    }

    private void processMetricsList(final MetricsList metricsList) {
        //TODO(barp): Map with a POJO mapper [MAI-184]
        final ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
        final ArrayNode services = JsonNodeFactory.instance.arrayNode();
        for (final Map.Entry<String, Map<String, Set<String>>> service : metricsList.getMetrics().entrySet()) {
            final ObjectNode serviceObject = JsonNodeFactory.instance.objectNode();
            serviceObject.put("name", service.getKey());
            final ArrayNode metrics = JsonNodeFactory.instance.arrayNode();
            for (final Map.Entry<String, Set<String>> metric : service.getValue().entrySet()) {
                final ObjectNode metricObject = JsonNodeFactory.instance.objectNode();
                metricObject.put("name", metric.getKey());
                final ArrayNode stats = JsonNodeFactory.instance.arrayNode();
                for (final String statistic : metric.getValue()) {
                    final ObjectNode statsObject = JsonNodeFactory.instance.objectNode();
                    statsObject.put("name", statistic);
                    statsObject.put("children", JsonNodeFactory.instance.arrayNode());
                    stats.add(statsObject);
                }
                metricObject.put("children", stats);
                metrics.add(metricObject);
            }
            serviceObject.put("children", metrics);
            services.add(serviceObject);
        }
        dataNode.put("metrics", services);
        sendCommand("metricsList", dataNode);
    }

    private void subscribe(final String service, final String metric, final String statistic) {
        if (!_subscriptions.containsKey(service)) {
            _subscriptions.put(service, Maps.<String, Set<String>>newHashMap());
        }

        final Map<String, Set<String>> metrics = _subscriptions.get(service);
        if (!metrics.containsKey(metric)) {
            metrics.put(metric, Sets.<String>newHashSet());
        }

        final Set<String> statistics = metrics.get(metric);
        if (!statistics.contains(statistic)) {
            statistics.add(statistic);
        }
    }

    private void unsubscribe(final String service, final String metric, final String statistic) {
        if (!_subscriptions.containsKey(service)) {
            return;
        }

        final Map<String, Set<String>> metrics = _subscriptions.get(service);
        if (!metrics.containsKey(metric)) {
            return;
        }

        final Set<String> statistics = metrics.get(metric);
        if (statistics.contains(statistic)) {
            statistics.remove(statistic);
        }
    }

    private void sendCommand(final String command, final ObjectNode data) {
        //TODO(barp): Map with a POJO mapper [MAI-184]
        final ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("command", command);
        node.put("data", data);
        _connection.write(node);
    }

    private final WebSocket.Out<JsonNode> _connection;
    private final Map<String, Map<String, Set<String>>> _subscriptions = Maps.newHashMap();

    private static final ObjectNode OK_RESPONSE = JsonNodeFactory.instance.objectNode().put("response", "ok");

}


