package tsdaggregator;

/**
 * Description goes here
 *
 * @author barp
 */
public interface LogParser {
	LogLine parseLogLine(String line);
}
