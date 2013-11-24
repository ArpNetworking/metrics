package com.arpnetworking.tsdaggregator;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Parses config files and creates a config from it.
 *
 * @author barp
 */
public class ConfigFileParser {
    private final HostResolver _hostResolver;

    ConfigFileParser(HostResolver hostResolver) {
        _hostResolver = hostResolver;
    }

    @Nonnull
    public Configuration parse(String fileName) throws ConfigException {
        @Nonnull ObjectMapper mapper = new ObjectMapper();
        @Nullable FileInputStream stream = null;
        JsonNode node;
        try {
            stream = new FileInputStream(fileName);
            node = mapper.readTree(stream);
        } catch (FileNotFoundException e) {
            throw new ConfigException("could not find config file", e);
        } catch (JsonProcessingException e) {
            throw new ConfigException("config file was not valid json", e);
        } catch (IOException e) {
            throw new ConfigException("unknown exception while reading config file", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {}
            }
        }

        if (!node.isObject()) {
            throw new ConfigException("expected config file to be a json object");
        }

        JsonNode argsNode = node.get("args");
        if (!argsNode.isArray()) {
            throw new ConfigException("expected a top level \"args\" array");
        }

        @Nonnull ArrayNode argsArray = (ArrayNode) argsNode;
        @Nonnull ArrayList<String> args = new ArrayList<>();
        for (int x = 0; x < argsArray.size(); x++) {
            args.add(argsArray.get(x).getTextValue());
        }

        @Nonnull CommandLineParser parser = new CommandLineParser(_hostResolver);
        return parser.parse(args.toArray(new String[args.size()]));
    }
}
