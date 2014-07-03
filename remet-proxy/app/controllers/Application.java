package controllers;

import com.typesafe.config.ConfigValue;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import play.Configuration;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Map;

public class Application extends Controller {
  
    public static Result index() {
        return ok(views.html.index.render("Your new application is ready."));
    }

    public static Result config() {
        JsonNode node = getConfigNode(play.Configuration.root());
        return ok(node);
    }

    private static JsonNode getConfigNode(Object element) {
        if (element instanceof Configuration) {
            Configuration config = (Configuration)element;
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
                put(node, entry.getKey(), entry.getValue());
            }
            return node;
        } else if (element instanceof String) {
            String config = (String)element;
            return JsonNodeFactory.instance.textNode(config);
        } else if (element instanceof Integer) {
            Integer integer = (Integer)element;
            return JsonNodeFactory.instance.numberNode(integer);
        } else if (element instanceof Double) {
            Double d = (Double)element;
            return JsonNodeFactory.instance.numberNode(d);
        } else if (element instanceof ArrayList) {
            ArrayNode arr = JsonNodeFactory.instance.arrayNode();
            ArrayList list = (ArrayList)element;
            for (Object o : list) {
                arr.add(getConfigNode(o));
            }
            return arr;
        }
        return JsonNodeFactory.instance.textNode("UNKNOWN TYPE: " + element.getClass().getCanonicalName());
    }

    public static Result healthCheck() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("status", "success");
        result.put("data", "ok");
        return ok(result);
    }

    private static void put(ObjectNode node, String remaining, ConfigValue value) {
        int dotIndex = remaining.indexOf(".");
        boolean leaf = dotIndex == -1;
        if (leaf) {
            node.put(remaining, getConfigNode(value.unwrapped()));
        } else {
            String firstChunk = remaining.substring(0, dotIndex);
            remaining = remaining.substring(dotIndex + 1);
            ObjectNode child = (ObjectNode)node.get(firstChunk);
            if (child == null || child.isNull()) {
                child = JsonNodeFactory.instance.objectNode();
                node.put(firstChunk, child);
            }
            put(child, remaining, value);
        }
    }
}