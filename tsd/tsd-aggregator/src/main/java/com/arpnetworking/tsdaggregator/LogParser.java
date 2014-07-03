package com.arpnetworking.tsdaggregator;

import com.google.common.base.Optional;

import javax.annotation.Nonnull;

/**
 * Interface describing a log parser class.
 *
 * @author barp
 */
public interface LogParser {
    @Nonnull
    Optional<LogLine> parseLogLine(String line);
}
