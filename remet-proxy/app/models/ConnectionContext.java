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

import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.ExecutionContexts;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.messages.Command;
import models.messages.Connect;
import models.protocol.MessageProcessorsFactory;
import models.protocol.MessagesProcessor;
import play.libs.Akka;
import play.mvc.WebSocket;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Actor class to hold the state for a single connection.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConnectionContext extends UntypedActor {
    /**
     * Public constructor.
     *
     * @param metricsFactory Instance of <code>MetricsFactory</code>.
     * @param connection Websocket connection to bind to.
     * @param processorsFactory Factory for producing the protocol's <code>MessagesProcessor</code>
     */
    public ConnectionContext(
            final MetricsFactory metricsFactory,
            final WebSocket.Out<JsonNode> connection,
            final MessageProcessorsFactory processorsFactory) {
        _metricsFactory = metricsFactory;
        _connection = connection;
        _instrument = Akka.system().scheduler().schedule(
                new FiniteDuration(0, TimeUnit.SECONDS), // Initial delay
                new FiniteDuration(500, TimeUnit.MILLISECONDS), // Interval
                getSelf(),
                "instrument",
                ExecutionContexts.global(),
                getSelf());
        _messageProcessors = processorsFactory.create(this);
        _metrics = createMetrics();
    }

    /**
     * Factory for creating a <code>Props</code> with strong typing.
     *
     * @param metricsFactory Instance of <code>MetricsFactory</code>.
     * @param connectMessage Connect message.
     * @return a new Props object to create a <code>ConnectionContext</code>.
     */
    public static Props props(
            final MetricsFactory metricsFactory,
            final Connect connectMessage
            ) {
        return Props.create(
                ConnectionContext.class,
                metricsFactory,
                connectMessage.getOutputChannel(),
                connectMessage.getMessageProcessorsFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        LOGGER.trace()
                .setMessage("Received message")
                .addData("actor", self().toString())
                .addData("data", message)
                .addData("channel", _connection)
                .log();
        if ("instrument".equals(message)) {
            periodicInstrumentation();
            return;
        }

        boolean messageProcessed = false;
        for (final MessagesProcessor messagesProcessor : _messageProcessors) {
            messageProcessed = messagesProcessor.handleMessage(message);
            if (messageProcessed) {
                break;
            }
        }
        if (!messageProcessed) {
            _metrics.incrementCounter(UNKNOWN_COUNTER);
            if (message instanceof Command) {
                _metrics.incrementCounter(UNKONOWN_COMMAND_COUNTER);
                LOGGER.warn()
                        .setMessage("Unsupported command")
                        .addData("actor", self().toString())
                        .addData("data", message)
                        .log();
                unhandled(message);
            } else {
                _metrics.incrementCounter("Actors/Connection/UNKNOWN");
                LOGGER.warn()
                        .setMessage("Unsupported message")
                        .addData("actor", self().toString())
                        .addData("data", message)
                        .log();
                unhandled(message);
            }
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

    /**
     * Sends a command object to the connected client.
     *
     * @param command The command.
     * @param data The data for the command.
     */
    public void sendCommand(final String command, final ObjectNode data) {
        //TODO(barp): Map with a POJO mapper [MAI-184]
        final ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("command", command);
        node.set("data", data);
        _connection.write(node);
    }

    private Metrics createMetrics() {
        final Metrics metrics = _metricsFactory.create();
        metrics.resetCounter(UNKONOWN_COMMAND_COUNTER);
        metrics.resetCounter(UNKNOWN_COUNTER);
        for (final MessagesProcessor messageProcessor : _messageProcessors) {
            messageProcessor.initializeMetrics(metrics);
        }
        return metrics;
    }

    private void periodicInstrumentation() {
        _metrics.close();
        _metrics = createMetrics();
    }

    public WebSocket.Out<JsonNode> getConnection() {
        return _connection;
    }

    private final MetricsFactory _metricsFactory;
    private final Cancellable _instrument;
    private final WebSocket.Out<JsonNode> _connection;
    private final List<MessagesProcessor> _messageProcessors;

    private Metrics _metrics;

    private static final String METRICS_PREFIX = "Actors/Connection/";
    private static final String UNKONOWN_COMMAND_COUNTER = METRICS_PREFIX + "Command/UNKNOWN";
    private static final String UNKNOWN_COUNTER = METRICS_PREFIX + "UNKNOWN";

    private static final ObjectNode OK_RESPONSE = JsonNodeFactory.instance.objectNode().put("response", "ok");
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionContext.class);
}
