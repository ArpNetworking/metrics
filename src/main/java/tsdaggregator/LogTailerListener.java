package tsdaggregator;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: barp
 * Date: 9/14/12
 * Time: 10:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogTailerListener extends TailerListenerAdapter {
    final LineProcessor _processor;
    static final Logger _Logger = Logger.getLogger(LogTailerListener.class);

    public LogTailerListener(LineProcessor processor) {
        _Logger.debug("Created LogTailerListener");
        _processor = processor;
    }

    @Override
    public void handle(String line) {
        _Logger.debug("Line read by LogTailerListener");
        _Logger.debug(line);
        _processor.invoke(line);
    }
}
