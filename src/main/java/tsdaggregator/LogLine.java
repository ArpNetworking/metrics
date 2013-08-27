/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tsdaggregator;

import org.joda.time.DateTime;

import java.util.Map;

/**
 *
 * @author brandarp
 */
public interface LogLine {

    DateTime getTime();

    Map<String, CounterVariable> getVariables();

}
