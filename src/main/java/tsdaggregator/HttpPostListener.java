package tsdaggregator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
            StringRequestEntity entity = null;
            try {
                entity = new StringRequestEntity(aggregateJson, "application/json", "utf8");
            } catch (UnsupportedEncodingException e) {
                _Logger.error("Error on creating POST body", e);
            }
            method.setRequestEntity(entity);
			try {
				_Logger.info("Posting to " + _Uri + " value '" + aggregateJson + "'");
				int result = client.executeMethod(method);
				if (result == HttpStatus.SC_OK) {
					String response = method.getResponseBodyAsString();
					_Logger.info("Post response ok");
				}
                else {
                    _Logger.warn("post was not accepted, status: " + result + ", body: " + method.getResponseBodyAsString());
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
