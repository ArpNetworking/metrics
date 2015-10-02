/**
 * Copyright 2015 Groupon.com
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

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.collect.EvictingQueue;
import org.apache.http.HttpStatus;
import org.joda.time.Period;
import play.libs.F;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.libs.ws.ning.NingWSClient;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Actor that sends HTTP requests via a Ning HTTP client.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HttpSinkActor extends UntypedActor {
    /**
     * Factory method to create a Props.
     *
     * @param client Http client to create requests from.
     * @param sink Sink that controls request creation and data serialization.
     * @param maximumConcurrency Maximum number of concurrent requests.
     * @param maximumQueueSize Maximum number of pending requests.
     * @param spreadPeriod Maximum time to delay sending new aggregates to spread load.
     * @return A new Props
     */
    public static Props props(
            final NingWSClient client,
            final HttpPostSink sink,
            final int maximumConcurrency,
            final int maximumQueueSize,
            final Period spreadPeriod) {
        return Props.create(HttpSinkActor.class, client, sink, maximumConcurrency, maximumQueueSize, spreadPeriod);
    }

    /**
     * Public constructor.
     *
     * @param client Http client to create requests from.
     * @param sink Sink that controls request creation and data serialization.
     * @param maximumConcurrency Maximum number of concurrent requests.
     * @param maximumQueueSize Maximum number of pending requests.
     * @param spreadPeriod Maximum time to delay sending new aggregates to spread load.
     */
    public HttpSinkActor(
            final NingWSClient client,
            final HttpPostSink sink,
            final int maximumConcurrency,
            final int maximumQueueSize,
            final Period spreadPeriod) {
        _client = client;
        _sink = sink;
        _maximumConcurrency = maximumConcurrency;
        _pendingRequests = EvictingQueue.create(maximumQueueSize);
        if (Period.ZERO.equals(spreadPeriod)) {
            _spreadingDelayMillis = 0;
        } else {
            _spreadingDelayMillis = new Random().nextInt((int) spreadPeriod.toStandardDuration().getMillis());
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("sink", _sink)
                .put("maximumConcurrency", _maximumConcurrency)
                .put("spreadingDelayMillis", _spreadingDelayMillis)
                .put("waiting", _waiting)
                .put("inflightRequestsCount", _inflightRequestsCount)
                .put("pendingRequestsCount", _pendingRequests.size())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof HttpSinkActor.EmitAggregation) {
            final EmitAggregation aggregation = (EmitAggregation) message;
            processEmitAggregation(aggregation);
        } else if (message instanceof PostComplete) {
            final PostComplete complete = (PostComplete) message;
            processCompletedRequest(complete);
            dispatchPending();
        } else if (message instanceof PostFailure) {
            final PostFailure failure = (PostFailure) message;
            processFailedRequest(failure);
            dispatchPending();
        } else if (message instanceof WaitTimeExpired) {
            LOGGER.debug()
                    .setMessage("Received WaitTimeExpired message")
                    .addContext("actor", self())
                    .log();
            _waiting = false;
            dispatchPending();
        } else {
            unhandled(message);
        }
    }

    private void processFailedRequest(final PostFailure failure) {
        _inflightRequestsCount--;
        LOGGER.error()
                .setMessage("Post error")
                .addData("sink", _sink)
                .addContext("actor", self())
                .setThrowable(failure.getCause())
                .log();
    }

    private void processCompletedRequest(final PostComplete complete) {
        _postRequests++;
        _inflightRequestsCount--;
        final int responseStatusCode = complete.getResponse().getStatus();
        if (responseStatusCode == HttpStatus.SC_OK) {
            LOGGER.debug()
                    .setMessage("Post accepted")
                    .addData("sink", _sink)
                    .addData("status", responseStatusCode)
                    .addContext("actor", self())
                    .log();
        } else {
            LOGGER.warn()
                    .setMessage("Post rejected")
                    .addData("sink", _sink)
                    .addData("status", responseStatusCode)
                    .addContext("actor", self())
                    .log();
        }
    }

    private void processEmitAggregation(final EmitAggregation emitMessage) {
        final PeriodicData periodicData = emitMessage.getData();

        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", _sink)
                .addData("dataSize", periodicData.getData().size())
                .addData("conditionsSize", periodicData.getConditions().size())
                .addContext("actor", self())
                .log();

        if (!periodicData.getData().isEmpty() || !periodicData.getConditions().isEmpty()) {
            final Collection<WSRequest> requests = _sink.createRequests(_client, periodicData);
            final boolean pendingWasEmpty = _pendingRequests.isEmpty();

            final int evicted = Math.max(0, requests.size() - _pendingRequests.remainingCapacity());
            for (final WSRequest request : requests) {
                // TODO(vkoskela): Add logging to client [MAI-89]
                // TODO(vkoskela): Add instrumentation to client [MAI-90]
                _pendingRequests.offer(request);
            }

            if (evicted > 0) {
                LOGGER.warn()
                        .setMessage("Evicted data from HTTP sink queue")
                        .addData("sink", _sink)
                        .addData("count", evicted)
                        .addContext("actor", self())
                        .log();
            }

            // If we don't currently have anything in-flight, we'll need to wait the spreading duration.
            // If we're already waiting, these requests will be sent after the waiting is over, no need to do anything else.
            if (pendingWasEmpty && !_waiting && _spreadingDelayMillis > 0) {
                _waiting = true;
                LOGGER.debug()
                        .setMessage("Scheduling http requests for later transmission")
                        .addData("delayMs", _spreadingDelayMillis)
                        .addContext("actor", self())
                        .log();
                context().system().scheduler().scheduleOnce(
                        FiniteDuration.apply(_spreadingDelayMillis, TimeUnit.MILLISECONDS),
                        self(),
                        new WaitTimeExpired(),
                        context().dispatcher(),
                        self());
            } else {
                dispatchPending();
            }
        }
    }

    /**
     * Dispatches the number of pending requests needed to drain the pendingRequests queue or meet the maximum concurrency.
     */
    private void dispatchPending() {
        LOGGER.debug()
                .setMessage("Dispatching requests")
                .addContext("actor", self())
                .log();
        while (_inflightRequestsCount < _maximumConcurrency && !_pendingRequests.isEmpty()) {
            fireNextRequest();
        }
    }

    private void fireNextRequest() {
        final WSRequest request = _pendingRequests.poll();
        _inflightRequestsCount++;

        final F.Promise<Object> responsePromise = request.execute()
                .<Object>map(PostComplete::new)
                .recover(PostFailure::new);
        Patterns.pipe(responsePromise.wrapped(), context().dispatcher()).to(self());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() throws Exception {
        super.postStop();
        LOGGER.info()
                .setMessage("Shutdown sink actor")
                .addData("sink", _sink)
                .addData("recordsWritten", _postRequests)
                .log();
    }

    private int _inflightRequestsCount = 0;
    private long _postRequests = 0;
    private boolean _waiting = false;
    private final int _maximumConcurrency;
    private final EvictingQueue<WSRequest> _pendingRequests;
    private final NingWSClient _client;
    private final HttpPostSink _sink;
    private final int _spreadingDelayMillis;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPostSink.class);

    /**
     * Message class to wrap a list of {@link com.arpnetworking.tsdcore.model.AggregatedData}.
     */
    public static final class EmitAggregation {

        /**
         * Public constructor.
         *
         * @param data Periodic data to emit.
         */
        public EmitAggregation(final PeriodicData data) {
            _data = data;
        }

        public PeriodicData getData() {
            return _data;
        }

        private final PeriodicData _data;
    }

    /**
     * Message class to wrap a completed HTTP request.
     */
    private static final class PostFailure {
        public PostFailure(final Throwable throwable) {
            _throwable = throwable;
        }

        public Throwable getCause() {
            return _throwable;
        }

        private final Throwable _throwable;
    }

    /**
     * Message class to wrap an errored HTTP request.
     */
    private static final class PostComplete {
        public PostComplete(final WSResponse response) {
            _response = response;
        }

        public WSResponse getResponse() {
            return _response;
        }

        private final WSResponse _response;
    }

    /**
     * Message class to indicate that we are now able to send data.
     */
    private static final class WaitTimeExpired { }
}
