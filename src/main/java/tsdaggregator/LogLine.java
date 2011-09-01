/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator;

import java.util.ArrayList;
import java.util.Map;
import org.joda.time.DateTime;

/**
 *
 * @author brandarp
 */
public interface LogLine {

    DateTime getTime();

    Map<String, ArrayList<Double>> getVariables();

    void parseLogLine(String line);
    
}
