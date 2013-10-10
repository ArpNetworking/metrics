package com.arpnetworking.tsdaggregator;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.log4j.Logger;

/**
 * Log tailer implementation class.
 *
 * @author barp
 */
public class LogTailerListener extends TailerListenerAdapter {
    private final LineProcessor _processor;
    private static final Logger LOGGER = Logger.getLogger(LogTailerListener.class);

    public LogTailerListener(LineProcessor processor) {
        LOGGER.debug("Created LogTailerListener");
        _processor = processor;
    }

    @Override
    public void handle(String line) {
        LOGGER.debug("Line read by LogTailerListener");
        LOGGER.debug(line);
        _processor.invoke(line);
    }
}
