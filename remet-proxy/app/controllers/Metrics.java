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

import com.fasterxml.jackson.databind.JsonNode;

import models.StreamContext;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.BodyParser.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

/**
 * Real time metrics proxy (ReMetProxy) streaming metrics Play Framework 
 * controller. The controller offers two APIs:
 * 
 * <ol>
 * <li>GET /stream - Retrieve the aggregated metrics as a stream.</li>
 * <li>POST /report - Store new aggregated metrics for streaming.</li>
 * </ol>
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class Metrics extends Controller {

    /**
     * Retrieve a stream of metric aggregations.
     * 
     * @return Stream of serialized metric aggregations.
     */
    public static WebSocket<JsonNode> stream() {
        return new WebSocket<JsonNode>() {
            // Called when the Websocket Handshake is done.
            @Override
            public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
                try {
                    StreamContext.connect(in, out);
                    // CHECKSTYLE.OFF: Catch all exceptions
                } catch (final Throwable t) {
                    // CHECKSTYLE.ON
                    Logger.error("Exception connect stream; in=" + in + ", out=" + out, t);
                }
            }
        };
    }

    /**
     * Store new aggregated metrics for streaming to clients.
     * 
     * @return Empty response with success/error code.
     */
    @BodyParser.Of(Json.class)
    public static Result report() {
        StreamContext.reportMetrics(request().body().asJson());
        return ok();
    }
}
