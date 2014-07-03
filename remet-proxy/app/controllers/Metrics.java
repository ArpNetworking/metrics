package controllers;

import models.StreamContext;
import org.codehaus.jackson.JsonNode;
import play.mvc.BodyParser.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 10/8/12
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class Metrics extends Controller {
    public static WebSocket<JsonNode> stream() {
        return new WebSocket<JsonNode>() {

            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    StreamContext.connect(in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    @BodyParser.Of(Json.class)
    public static Result report() {
        StreamContext.reportMetrics(request().body().asJson());
        return ok();
    }
}
