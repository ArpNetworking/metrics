/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tsdaggregator;

import java.io.*;
import java.util.*;

/**
 *
 * @author brandarp
 */
public class TsdAggregator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        HashMap<String, TSData> aggregations = new HashMap<String, TSData>();
        try {
			FileReader fileReader = new FileReader(args[0]);
			BufferedReader reader = new BufferedReader(fileReader);
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				LineData data = new LineData();
				data.parseLogLine(line);
				for (Map.Entry<String, Double> entry : data.getVariables().entrySet()) {
					TSData tsdata = aggregations.get(entry.getKey());
					tsdata.addMetric(entry.getValue(), data.getTime());
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
