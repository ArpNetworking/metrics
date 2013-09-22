package tsdaggregator;

import com.google.common.base.Optional;

/**
 * Description goes here
 *
 * @author barp
 */
public interface LogParser {
	Optional<LogLine> parseLogLine(String line);
}
