package tsdaggregator;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

import java.io.IOException;

public class HttpPostListener implements AggregationListener {
	String _Uri;
	static Logger _Logger = Logger.getLogger(HttpPostListener.class);
    static HttpClient _client;
    static ClientConnectionManager _connectionManager;
    static {
        _connectionManager = new PoolingClientConnectionManager();
        _client = new DefaultHttpClient(_connectionManager);
        HttpParams params = _client.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
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
            StringEntity entity = new StringEntity(aggregateJson, ContentType.APPLICATION_JSON);
            method.setEntity(entity);
            HttpEntity responseEntity = null;
			try {
				_Logger.info("Posting to " + _Uri + " value '" + aggregateJson + "'");
				HttpResponse result = _client.execute(method);
                responseEntity = result.getEntity();
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
			} finally {
                if (responseEntity != null) {
                    try { responseEntity.getContent().close(); } catch (Exception ignored) {}
                }
            }
		}		
	}

	@Override
	public void close() {
		return;
	}

}
