package tsdaggregator;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class HttpPostListener implements AggregationListener {
	String _Uri;
	static Logger _Logger = Logger.getLogger(HttpPostListener.class);
    static HttpClient client = new DefaultHttpClient();
    static {
        HttpParams params = client.getParams();
        params.setLongParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000l);
    }
	public HttpPostListener(String uri) {
		_Uri = uri;
		
	}
	
	@Override
	public void recordAggregation(AggregatedData[] data) {
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

			HttpPost method = new HttpPost(_Uri);
            StringEntity entity = null;
            try {
                entity = new StringEntity(aggregateJson, "application/json", "utf8");
            } catch (UnsupportedEncodingException e) {
                _Logger.error("Error on creating POST body", e);
            }
            method.setEntity(entity);
			try {
				_Logger.info("Posting to " + _Uri + " value '" + aggregateJson + "'");
				HttpResponse result = client.execute(method);
				if (result.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					_Logger.info("Post response ok");
				}
                else {
                    _Logger.warn("post was not accepted, status: " + result + ", body: " + IOUtils.toString(result.getEntity().getContent(), "UTF-8"));
                }
			} catch (ClientProtocolException e) {
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
