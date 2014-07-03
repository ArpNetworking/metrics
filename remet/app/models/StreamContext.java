package models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import play.Logger;
import play.libs.Akka;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A chat room is an Actor.
 */
public class StreamContext extends UntypedActor {

    // Default room.
    static ActorRef defaultContext = Akka.system().actorOf(new Props(StreamContext.class));


    /**
     * Join the default room.
     */
    public static void connect(WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) throws Exception{

        // Send the Join message to the room
        String result = (String) Await.result(ask(defaultContext, new Connect(out), 1000), Duration.apply(1, SECONDS));

        if("OK".equals(result)) {

            // For each event received on the socket,
            in.onMessage(new Callback<JsonNode>() {
                public void invoke(JsonNode event) {
                    // Send the command
                    defaultContext.tell(new Command(out, event));
                }
            });

            // When the socket is closed.
            in.onClose(new Callback0() {
                public void invoke() {
                    // Send a Quit message to the room.
                    defaultContext.tell(new Quit(out));
                }
            });
        }

    }

    public static void reportMetrics(JsonNode node) {
        Logger.info("got a metrics report");
        defaultContext.tell(new MetricReport(node));
    }

        // Members of this room.
    Set<WebSocket.Out<JsonNode>> members = new HashSet<WebSocket.Out<JsonNode>>();
    Map<String, Map<String, Set<String>>> serviceMetrics = new HashMap<String, Map<String, Set<String>>>();

    public void onReceive(Object message) throws Exception {
        Logger.info("received message " + message);
        if(message instanceof Connect) {
            // Received a Join message
            Connect connect = (Connect)message;
            members.add(connect.channel);
            Logger.info("adding new channel to streaming context");
            getSender().tell("OK");
        } else if(message instanceof Command)  {
            // Received a Command message

            Command commandMessage = (Command)message;
            ObjectNode cmd = (ObjectNode)commandMessage.command;

            String commandString = cmd.get("command").asText();
            if (commandString.equals("getMetrics")) {
                Logger.info("channel has requested the metrics list");
                ObjectNode returnNode = JsonNodeFactory.instance.objectNode();

                ArrayNode services = JsonNodeFactory.instance.arrayNode();
                for (Map.Entry<String, Map<String, Set<String>>> service : serviceMetrics.entrySet()) {
                    ObjectNode serviceObject = JsonNodeFactory.instance.objectNode();
                    serviceObject.put("name", service.getKey());
                    ArrayNode metrics = JsonNodeFactory.instance.arrayNode();
                    for (Map.Entry<String, Set<String>> metric : service.getValue().entrySet()) {
                        ObjectNode metricObject = JsonNodeFactory.instance.objectNode();
                        metricObject.put("name", metric.getKey());
                        ArrayNode stats = JsonNodeFactory.instance.arrayNode();
                        for (String statistic : metric.getValue()) {
                            ObjectNode statsObject = JsonNodeFactory.instance.objectNode();
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
                returnNode.put("metrics", services);
                ObjectNode command = Json.newObject();
                command.put("command", "metricsList");
                command.put("data", returnNode);
                commandMessage.channel.write(command);
            } else if (commandString.equals("heartbeat")) {
                ObjectNode ret = Json.newObject();
                ret.put("response", "ok");
                commandMessage.channel.write(ret);
            } else {

            }
        } else if (message instanceof MetricReport) {
            MetricReport report = (MetricReport)message;
            ArrayNode list = (ArrayNode)report.report;
            for (JsonNode node : list) {
                ObjectNode obj = (ObjectNode)node;
                String service = obj.get("service").asText();
                String host = obj.get("host").asText();
                String statistic = obj.get("statistic").asText();
                String metric = obj.get("counter").asText();
                Double value = obj.get("value").asDouble();
                String periodStart = obj.get("periodStart").asText();
                DateTime startTime = DateTime.parse(periodStart);
                registerMetric(service, metric, statistic);
                emitAggregation(host, service, metric, statistic, startTime.getMillis(), value);
            }
        } else if(message instanceof Quit)  {
            // Received a Quit message
            Quit quit = (Quit)message;

            members.remove(quit.channel);
            Logger.info("removing channel from streaming context");
        } else {
            unhandled(message);
        }
    }

    private void registerMetric(String service, String metric, String statistic) {
        if (!serviceMetrics.containsKey(service)) {
            serviceMetrics.put(service, new HashMap<String, Set<String>>());
        }
        Map<String, Set<String>> serviceMap = serviceMetrics.get(service);

        if (!serviceMap.containsKey(metric)) {
            serviceMap.put(metric, new HashSet<String>());
        }
        Set<String> metricMap = serviceMap.get(metric);

        if (!metricMap.contains(statistic)) {
            metricMap.add(statistic);
            notifyNewMetric(service, metric, statistic);
        }
    }

    private void notifyNewMetric(String service, String metric, String statistic) {
        ObjectNode n = Json.newObject();
        n.put("service", service);
        n.put("metric", metric);
        n.put("statistic", statistic);
        sendToAll("newMetric", n);
    }

    // Send a Json event to all members
    public void emitAggregation(String server, String service, String metric, String statistic, Long timestamp, Double data) {
        ObjectNode event = Json.newObject();
        event.put("server", server);
        event.put("service", service);
        event.put("metric", metric);
        event.put("timestamp", timestamp);
        event.put("statistic", statistic);
        event.put("data", data);

        sendToAll("report", event);
    }

    private void sendToAll(String name, JsonNode node) {
        ObjectNode command = Json.newObject();
        command.put("command", name);
        command.put("data", node);
        for(WebSocket.Out<JsonNode> channel: members) {
            try {
                channel.write(command);
            } catch (Throwable e) {
                members.remove(channel);
            }
        }
    }

    // -- Messages

    public static class Connect {

        final WebSocket.Out<JsonNode> channel;

        public Connect(WebSocket.Out<JsonNode> channel) {
            this.channel = channel;
        }

    }

    public static class Command {

        final WebSocket.Out<JsonNode> channel;
        final JsonNode command;

        public Command(WebSocket.Out<JsonNode> channel, JsonNode command) {
            this.channel = channel;
            this.command = command;
        }
    }

    public static class MetricReport {

        final JsonNode report;

        public MetricReport(JsonNode report) {
            this.report = report;
        }
    }

    public static class Quit {
        final WebSocket.Out<JsonNode> channel;
        public Quit(WebSocket.Out<JsonNode> channel) {
            this.channel = channel;
        }

    }

}
