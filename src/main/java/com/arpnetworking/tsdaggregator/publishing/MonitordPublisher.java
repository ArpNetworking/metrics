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
import org.joda.time.format.ISOPeriodFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Publishes aggregations to Monitord.
 *
 * @author barp
 */
public class MonitordPublisher implements AggregationPublisher {
    private final String _uri;
    private final String _cluster;
    private final String _host;
    private final HttpClient _client;
    private static final Logger LOGGER = Logger.getLogger(MonitordPublisher.class);

    public MonitordPublisher(String uri, String cluster, String host) {
        this(uri, cluster, host, buildDefaultClient());
    }

    @Nonnull
    private static HttpClient buildDefaultClient() {
        @Nonnull ClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        @Nonnull HttpClient client = new DefaultHttpClient(connectionManager);
        HttpParams params = client.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
        return client;
    }

    public MonitordPublisher(String uri, String cluster, String host, HttpClient client) {
        _uri = uri;
        _cluster = cluster;
        _host = host;
        _client = client;
    }

    @Override
    public void recordAggregation(@Nonnull AggregatedData[] data) {
        if (data.length == 0) {
            return;
        }
        @Nonnull HashMap<String, ArrayList<AggregatedData>> aggMap = new HashMap<>();
        //Build the map for the aggregations
        for (@Nonnull AggregatedData d : data) {
            ArrayList<AggregatedData> mapped = aggMap.get(d.getMetric());
            if (mapped == null) {
                mapped = new ArrayList<>();
                aggMap.put(d.getMetric(), mapped);
            }
            mapped.add(d);
        }

        for (@Nonnull Map.Entry<String, ArrayList<AggregatedData>> entry : aggMap.entrySet()) {
            //All aggregated data values for a metric should have the same metric metadata
            AggregatedData d = entry.getValue().get(0);

            //Skip periods < 60 seconds
            if (d.getPeriod().toStandardSeconds().getSeconds() < 60) {
                continue;
            }

            @Nonnull StringBuilder postValue = new StringBuilder();
            @Nonnull String combinedMetricName =
                    d.getService() + "_" + d.getPeriod().toString(ISOPeriodFormat.standard()) + "_" + d.getMetric();
            postValue.append("run_every=").append(d.getPeriod().toStandardSeconds().getSeconds())
                    .append("&path=").append(_cluster).append("/").append(_host).append("&monitor=")
                    .append(combinedMetricName)
                    .append("&status=0&output=").append(combinedMetricName).append("%7C");

            for (@Nonnull AggregatedData dataVal : entry.getValue()) {
                postValue.append(dataVal.getStatistic().getName()).append("%3D").append(dataVal.getValue())
                        .append("%3B");
            }
            //Strip off the trailing semicolon escape
            postValue.setLength(postValue.length() - 3);

            postData(postValue);
        }
    }

    private void postData(@Nonnull final StringBuilder postValue) {
        @Nonnull HttpPost method = new HttpPost(_uri);
        @Nonnull StringEntity entity = new StringEntity(postValue.toString(), ContentType.APPLICATION_FORM_URLENCODED);
        method.setEntity(entity);
        @Nullable HttpEntity responseEntity = null;
        try {
            LOGGER.info("Posting to " + _uri + " value '" + postValue.toString() + "'");
            HttpResponse result = _client.execute(method);
            responseEntity = result.getEntity();
            if (result.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                LOGGER.info("Post response ok");
            } else {
                LOGGER.warn("post was not accepted, status: " + result + ", body: " +
                        IOUtils.toString(responseEntity.getContent(), "UTF-8"));
            }
        } catch (IOException e) {
            LOGGER.error("Error on reporting", e);
        } finally {
            if (responseEntity != null) {
                try {
                    responseEntity.getContent().close();
                    LOGGER.debug("closed content stream");
                } catch (Exception ignored) {
                    LOGGER.warn("error closing content stream");
                }
            } else {
                LOGGER.debug("responseEntity is null");
            }
        }
    }

    @Override
    public void close() {
    }

}
