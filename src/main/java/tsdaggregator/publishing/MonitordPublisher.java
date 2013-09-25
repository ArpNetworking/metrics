package tsdaggregator.publishing;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.joda.time.format.ISOPeriodFormat;
import tsdaggregator.AggregatedData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MonitordPublisher implements AggregationPublisher {
	String _Uri;
    String _Cluster;
    String _Host;
	HttpClient _Client;
	static Logger _Logger = Logger.getLogger(MonitordPublisher.class);

	public MonitordPublisher(String uri, String cluster, String host) {
		this(uri, cluster, host, buildDefaultClient());
	}

	private static HttpClient buildDefaultClient() {
		ClientConnectionManager connectionManager = new PoolingClientConnectionManager();
		HttpClient client = new DefaultHttpClient(connectionManager);
		HttpParams params = client.getParams();
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
		return client;
	}

	public MonitordPublisher(String uri, String cluster, String host, HttpClient client) {
		_Uri = uri;
		_Cluster = cluster;
		_Host = host;
		_Client = client;
	}
	
	@Override
	public void recordAggregation(AggregatedData[] data) {
		if (data.length > 0) {
            HashMap<String, ArrayList<AggregatedData>> aggMap = new HashMap<>();
			for (AggregatedData d : data) {
                ArrayList<AggregatedData> mapped = aggMap.get(d.getMetric());
                if (mapped == null) {
                    mapped = new ArrayList<>();
                    aggMap.put(d.getMetric(), mapped);
                }
                mapped.add(d);
            }

            for (Map.Entry<String, ArrayList<AggregatedData>> entry : aggMap.entrySet()) {
                //All aggregated data values for a metric should have the same metric metadata
                AggregatedData d = entry.getValue().get(0);

                //Skip periods < 60 seconds
                if (d.getPeriod().toStandardSeconds().getSeconds() < 60) {
                    continue;
                }

                StringBuilder postValue = new StringBuilder();
                String combinedMetricName = new StringBuilder().append(d.getService()).append("_")
                        .append(d.getPeriod().toString(ISOPeriodFormat.standard())).append("_")
                        .append(d.getMetric()).toString();
                postValue.append("run_every=").append(d.getPeriod().toStandardSeconds().getSeconds())
                        .append("&path=").append(_Cluster).append("/").append(_Host).append("&monitor=")
                        .append(combinedMetricName)
                        .append("&status=0&output=").append(combinedMetricName).append("%7C");

                for (AggregatedData dataVal : entry.getValue()) {
                    postValue.append(dataVal.getStatistic().getName()).append("%3D").append(dataVal.getValue()).append("%3B");
                }
                //Strip off the trailing semicolon escape
                postValue.setLength(postValue.length() - 3);

                HttpPost method = new HttpPost(_Uri);
                StringEntity entity = new StringEntity(postValue.toString(), ContentType.APPLICATION_FORM_URLENCODED);
                method.setEntity(entity);
                HttpEntity responseEntity = null;
                try {
                    _Logger.info("Posting to " + _Uri + " value '" + postValue.toString() + "'");
                    HttpResponse result = _Client.execute(method);
                    responseEntity = result.getEntity();
                    if (result.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        _Logger.info("Post response ok");
                    }
                    else {
                        _Logger.warn("post was not accepted, status: " + result + ", body: " + IOUtils.toString(responseEntity.getContent(), "UTF-8"));
                    }
                } catch (IOException e) {
                    _Logger.error("Error on reporting", e);
                } finally {
					if (responseEntity != null) {
						try {
							responseEntity.getContent().close();
							_Logger.debug("closed content stream");
						} catch (Exception ignored) {
							_Logger.warn("error closing content stream");
						}
					} else {
						_Logger.debug("responseEntity is null");
					}
                }
            }
		}
	}

	@Override
	public void close() {
		return;
	}

}
