/**
 * Copyright 2014 Brandon Arp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.NotNull;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes to an HTTP endpoint. This class is thread safe.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public abstract class HttpPostSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", getName())
                .addData("dataSize", data.size())
                .addData("conditionsSize", conditions.size())
                .addData("uri", _uri)
                .log();

        if (!data.isEmpty() || !conditions.isEmpty()) {
            // TODO(vkoskela): Support parallel post requests [MAI-97]
            for (final HttpUriRequest request : createRequests(data, conditions)) {
                HttpEntity responseEntity = null;
                try {
                    // TODO(vkoskela): Add logging to client [MAI-89]
                    // TODO(vkoskela): Add instrumentation to client [MAI-90]
                    final HttpResponse result = CLIENT.execute(request);
                    responseEntity = result.getEntity();
                    final int responseStatusCode = result.getStatusLine().getStatusCode();

                    if (responseStatusCode == HttpStatus.SC_OK) {
                        LOGGER.debug()
                                .setMessage("Post accepted")
                                .addData("sink", getName())
                                .addData("uri", _uri)
                                .addData("status", responseStatusCode)
                                .log();
                    } else {
                        LOGGER.warn()
                                .setMessage("Post rejected")
                                .addData("sink", getName())
                                .addData("uri", _uri)
                                .addData("status", responseStatusCode)
                                .addData("requestSize", getContentLength(request))
                                .log();
                    }
                    _postRequests.incrementAndGet();
                } catch (final IOException e) {
                    LOGGER.error()
                            .setMessage("Post error")
                            .addData("sink", getName())
                            .addData("uri", _uri)
                            .addData("requestSize", getContentLength(request))
                            .setThrowable(e)
                            .log();
                } finally {
                    if (responseEntity != null) {
                        try {
                            responseEntity.getContent().close();
                            // CHECKSTYLE.OFF: IllegalCatch - Catch all exceptions
                        } catch (final Exception e) {
                            // CHECKSTYLE.ON: IllegalCatch
                            LOGGER.warn()
                                    .setMessage("Error closing response content stream")
                                    .addData("sink", getName())
                                    .addData("uri", _uri)
                                    .addData("requestSize", getContentLength(request))
                                    .setThrowable(e)
                                    .log();
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        LOGGER.info()
                .setMessage("Closing sink")
                .addData("sink", getName())
                .addData("recordsWritten", _postRequests)
                .addData("uri", _uri)
                .log();
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "super", super.toLogValue(),
                "Uri", _uri,
                "PostRequests", _postRequests);
    }

    /**
     * Creates an HTTP request from a serialized data entry. Default is an <code>HttpPost</code> containing
     * serializedData as the body with content type of application/json
     * @param serializedData The serialized data.
     * @return <code>HttpRequest</code> to execute
     */
    protected HttpUriRequest createRequest(final String serializedData) {
        final StringEntity requestEntity = new StringEntity(serializedData, ContentType.APPLICATION_JSON);
        final HttpPost request = new HttpPost(_uri);
        request.setEntity(requestEntity);
        return request;
    }

    /**
     * Create HTTP requests for each serialized data entry. The list is
     * guaranteed to be non-empty.
     *
     * @param data The <code>List</code> of <code>AggregatedData</code> to be
     * serialized.
     * @param conditions The <code>List</code> of <code>Condition</code>
     * instances to be published
     * @return The <code>HttpRequest</code> instance to execute.
     */
    protected Collection<HttpUriRequest> createRequests(
            final Collection<AggregatedData> data,
            final Collection<Condition> conditions) {
        final Collection<HttpUriRequest> requests = Lists.newArrayList();
        for (final String serializedData : serialize(data, conditions)) {
            requests.add(createRequest(serializedData));
        }
        return requests;
    }

    /**
     * Accessor for the <code>URI</code>.
     *
     * @return The <code>URI</code>.
     */
    protected URI getUri() {
        return _uri;
    }

    /**
     * Serialize the <code>AggregatedData</code> and <code>Condition</code> instances
     * for posting.
     *
     * @param data The <code>List</code> of <code>AggregatedData</code> to be
     * serialized.
     * @param conditions The <code>List</code> of <code>Condition</code>
     * instances to be published
     * @return The serialized representation of <code>AggregatedData</code>.
     */
    protected abstract Collection<String> serialize(
            final Collection<AggregatedData> data,
            final Collection<Condition> conditions);


    private static long getContentLength(final HttpUriRequest request) {
        if (request instanceof HttpEntityEnclosingRequestBase) {
            final HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            final HttpEntity entity = entityRequest.getEntity();
            if (entity != null) {
                return entity.getContentLength();
            }
        }
        return -1;
    }

    /**
     * Protected constructor.
     *
     * @param builder Instance of <code>Builder</code>.
     */
    protected HttpPostSink(final Builder<?, ?> builder) {
        super(builder);
        _uri = builder._uri;
    }

    private final URI _uri;
    private final AtomicLong _postRequests = new AtomicLong(0);

    private static final ClientConnectionManager CONNECTION_MANAGER = new PoolingClientConnectionManager();
    private static final HttpClient CLIENT = new DefaultHttpClient(CONNECTION_MANAGER);
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPostSink.class);
    private static final int CONNECTION_TIMEOUT_IN_MILLISECONDS = 3000;

    static {
        final HttpParams params = CLIENT.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_IN_MILLISECONDS);
    }

    /**
     * Implementation of abstract builder pattern for <code>HttpPostSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public abstract static class Builder<B extends BaseSink.Builder<B, S>, S extends HttpPostSink> extends BaseSink.Builder<B, S> {

        /**
         * The <code>URI</code> to post the aggregated data to. Cannot be null.
         *
         * @param value The <code>URI</code> to post the aggregated data to.
         * @return This instance of <code>Builder</code>.
         */
        public B setUri(final URI value) {
            _uri = value;
            return self();
        }

        /**
         * Protected constructor for subclasses.
         *
         * @param targetClass The concrete type to be created by the builder of
         * <code>AggregatedDataSink</code> implementation.
         */
        protected Builder(final Class<S> targetClass) {
            super(targetClass);
        }

        @NotNull
        private URI _uri;
    }
}
