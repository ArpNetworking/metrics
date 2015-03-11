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
package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import models.messages.Connect;
import models.messages.MetricReport;
import models.protocol.MessageProcessorsFactory;
import models.protocol.v1.ProcessorsV1Factory;
import models.protocol.v2.ProcessorsV2Factory;
import org.joda.time.DateTime;
import play.mvc.BodyParser;
import play.mvc.BodyParser.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

import javax.inject.Named;

/**
 * Real time metrics proxy (ReMetProxy) streaming metrics Play Framework
 * controller. The controller offers two APIs:
 *
 * <ol>
 * <li>GET /telemetry/v1/stream - Retrieve the aggregated metrics as a stream.</li>
 * <li>POST /telemetry/v1/report - Store new aggregated metrics for streaming.</li>
 * </ol>
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TelemetryController extends Controller {

    /**
     * Public constructor.
     *
     * @param streamContext Actor reference to a <code>StreamContext</code> actor instance.
     */
    @Inject
    public TelemetryController(@Named("StreamContext") final ActorRef streamContext) {
        _streamContext = streamContext;
    }

    /**
     * Retrieve a stream of metric aggregations and log messages.
     *
     * @return Stream of serialized metric aggregations.
     */
    public WebSocket<JsonNode> streamV1() {
        return stream(V1_FACTORY);
    }

    /**
     * Retrieve a stream of metric aggregations and log messages.
     *
     * @return Stream of serialized metric aggregations.
     */
    public WebSocket<JsonNode> streamV2() {
        return stream(V2_FACTORY);
    }

    private WebSocket<JsonNode> stream(final MessageProcessorsFactory factory) {
        return new WebSocket<JsonNode>() {
            // Called when the Websocket Handshake is done.
            @Override
            public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
                _streamContext.tell(new Connect(in, out, factory), ActorRef.noSender());
            }
        };
    }

    /**
     * Store new aggregated metrics for streaming to clients.
     *
     * @return Empty response with success/error code.
     */
    @BodyParser.Of(Json.class)
    public Result report() {
        // TODO(barp): Map with a POJO mapper [MAI-184]

        final ArrayNode list = (ArrayNode) request().body().asJson();
        for (final JsonNode objNode : list) {
            final ObjectNode obj = (ObjectNode) objNode;
            final String service = obj.get("service").asText();
            final String host = obj.get("host").asText();
            final String statistic = obj.get("statistic").asText();
            final String metric = obj.get("metric").asText();
            final double value = obj.get("value").asDouble();
            final String periodStart = obj.get("periodStart").asText();
            final DateTime startTime = DateTime.parse(periodStart);

            _streamContext.tell(
                    new MetricReport(
                            service,
                            host,
                            statistic,
                            metric,
                            value,
                            startTime),
                    ActorRef.noSender());
        }

        return ok();
    }

    private final ActorRef _streamContext;
    private static final ProcessorsV1Factory V1_FACTORY = new ProcessorsV1Factory();
    private static final ProcessorsV2Factory V2_FACTORY = new ProcessorsV2Factory();
}
