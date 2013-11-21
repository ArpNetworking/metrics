package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
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

import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * Publishes to and http endpoint.
 *
 * @author barp
 */
public class HttpPostPublisher implements AggregationPublisher {
    private final String _uri;
    private static final Logger LOGGER = Logger.getLogger(HttpPostPublisher.class);
    @Nonnull
    private static final HttpClient CLIENT;
    @Nonnull
    private static final ClientConnectionManager CONNECTION_MANAGER;

    static {
        CONNECTION_MANAGER = new PoolingClientConnectionManager();
        CLIENT = new DefaultHttpClient(CONNECTION_MANAGER);
        HttpParams params = CLIENT.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
    }

    public HttpPostPublisher(String uri) {
        _uri = uri;

    }

    @Override
    public void recordAggregation(@Nonnull AggregatedData[] data) {
        if (data.length > 0) {
            StringBuilder aggregateJson = new StringBuilder();
            aggregateJson.append("[");
            for (AggregatedData d : data) {

                aggregateJson.append("{\"value\":\"").append(d.getValue())
                        .append("\",").append("\"counter\":\"").append(d.getMetric()).append("\",")
                        .append("\"service\":\"").append(d.getService()).append("\",").append("\"host\":\"")
                        .append(d.getHost()).append("\",").append("\"period\":\"").append(d.getPeriod())
                        .append("\",").append("\"periodStart\":\"").append(d.getPeriodStart()).append("\",")
                        .append("\"statistic\":\"").append(d.getStatistic().getName()).append("\"").append("}");
                aggregateJson.append(',');
            }
            //Strip off the trailing comma
            aggregateJson.delete(aggregateJson.length() - 1, aggregateJson.length());
            aggregateJson.append("]");

            HttpPost method = new HttpPost(_uri);
            StringEntity entity = new StringEntity(aggregateJson.toString(), ContentType.APPLICATION_JSON);
            method.setEntity(entity);
            HttpEntity responseEntity = null;
            try {
                LOGGER.debug("Posting to " + _uri + " value '" + aggregateJson + "'");
                HttpResponse result = CLIENT.execute(method);
                responseEntity = result.getEntity();
                if (result.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    LOGGER.debug("Post response ok");
                } else {
                    LOGGER.warn("post to " + _uri + "was not accepted, status: " + result + ", body: " +
                            IOUtils.toString(result.getEntity().getContent(), "UTF-8") + "\nrequest: " + aggregateJson);
                }
            } catch (IOException e) {
                LOGGER.error("Error posting to HttpPublisher", e);
            } finally {
                if (responseEntity != null) {
                    try {
                        responseEntity.getContent().close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void close() {
    }

}
