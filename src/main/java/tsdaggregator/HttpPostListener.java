package tsdaggregator;

import java.io.IOException;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import org.apache.log4j.Logger;
public class HttpPostListener implements AggregationListener {
	String _Uri;
	static Logger _Logger = Logger.getLogger(HttpPostListener.class);
	public HttpPostListener(String uri) {
		_Uri = uri;
		
	}
	
	@Override
	public void recordAggregation(AggregatedData[] data) {
		HttpClient client = new HttpClient();
		HttpClientParams params = new HttpClientParams();
		params.setConnectionManagerTimeout(3000);
		client.setParams(params);
		if (data.length > 0) {
			String aggregateJson = "[";
			for (AggregatedData d : data) {
			
				String jsonVal = "{\"value\":\"" + d.getValue().toString() + "\"," +
	                 "\"counter\":\"" + d.getMetric() + "\"," +
	                 "\"service\":\"" + d.getService() + "\"," +
	                 "\"host\":\"" + d.getHost() + "\"," +
	                 "\"period\":\"" + d.getPeriod() + "\"," +
	                 "\"periodStart\":\"" + d.getPeriodStart() + "\"," +
	                 "\"statistic\":\"" + d.getStatistic().getName() + "\"" +
	                 "}";
				aggregateJson += jsonVal + ',';
			}
			//Strip off the trailing comma
			aggregateJson = aggregateJson.substring(0, aggregateJson.length() - 1);
			aggregateJson += "]";
			
			PostMethod method = new PostMethod(_Uri);
			method.addParameter("data", aggregateJson);
			try {
				_Logger.info("Posting to " + _Uri + " value '" + aggregateJson + "'");
				int result = client.executeMethod(method);
				if (result == HttpStatus.SC_OK) {
					String response = method.getResponseBodyAsString();
					_Logger.info("Post response: " + response);
				}
			} catch (HttpException e) {
				_Logger.error("Error on reporting", e);
			} catch (IOException e) {
				_Logger.error("Error on reporting", e);
			}
		}		
	}

	@Override
	public void close() {
		return;
	}

}
