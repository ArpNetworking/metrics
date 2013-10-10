package com.arpnetworking.tsdaggregator;

import com.google.common.base.Optional;

/**
 * Interface describing a log parser class.
 *
 * @author barp
 */
public interface LogParser {
    Optional<LogLine> parseLogLine(String line);
}
