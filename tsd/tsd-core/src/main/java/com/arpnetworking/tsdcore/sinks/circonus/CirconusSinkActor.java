/**
 * Copyright 2014 Groupon.com
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
package com.arpnetworking.tsdcore.sinks.circonus;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogBuilder;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.steno.RateLimitLogBuilder;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.sinks.circonus.api.BrokerListResponse;
import com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundle;
import com.arpnetworking.tsdcore.statistics.HistogramStatistic;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import play.libs.F;
import play.libs.ws.WSResponse;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Reports data to Circonus HttpTrap.
 *
 * This actor maintains a non-blocking HTTP client to Circonus internally.  It is responsible for
 * creating the necessary check bundles to post to and maintains a mapping of incoming aggregated data
 * and the check bundles that accept that data.
 *
 * Messages:
 *   External -
 *     Aggregation - Sent to the actor to send AggregatedData to the sink.
 *   Internal -
 *     BrokerListResponse - Sent from the Circonus client.  After receiving, used to decide which brokers to send
 *       the check bundle registrations for.
 *     ServiceCheckBinding - Sent internally after registration of a check bundle.  The binding is stored internally
 *       to keep track of check bundle urls.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class CirconusSinkActor extends UntypedActor {
    /**
     * Creates a {@link akka.actor.Props} for use in Akka.
     *
     * @param client Circonus client
     * @param broker Circonus broker to push to
     * @param maximumConcurrency the maximum number of parallel metric submissions
     * @param maximumQueueSize the maximum size of the pending metrics queue
     * @param spreadPeriod the maximum wait time before starting to send metrics
     * @param enableHistograms true to turn on histogram publication
     * @return A new {@link akka.actor.Props}
     */
    public static Props props(
            final CirconusClient client,
            final String broker,
            final int maximumConcurrency,
            final int maximumQueueSize,
            final Period spreadPeriod,
            final boolean enableHistograms) {
        return Props.create(CirconusSinkActor.class, client, broker, maximumConcurrency, maximumQueueSize, spreadPeriod, enableHistograms);
    }

    /**
     * Public constructor.
     *
     * @param client Circonus client
     * @param broker Circonus broker to push to
     * @param maximumConcurrency the maximum number of parallel metric submissions
     * @param maximumQueueSize the maximum size of the pending metrics queue
     * @param spreadPeriod the maximum wait time before starting to send metrics
     * @param enableHistograms true to turn on histogram publication
     */
    public CirconusSinkActor(
            final CirconusClient client,
            final String broker,
            final int maximumConcurrency,
            final int maximumQueueSize,
            final Period spreadPeriod,
            final boolean enableHistograms) {
        _client = client;
        _brokerName = broker;
        _maximumConcurrency = maximumConcurrency;
        _enableHistograms = enableHistograms;
        _pendingRequests = EvictingQueue.create(maximumQueueSize);
        if (Period.ZERO.equals(spreadPeriod)) {
            _spreadingDelayMillis = 0;
        } else {
            _spreadingDelayMillis = new Random().nextInt((int) spreadPeriod.toStandardDuration().getMillis());
        }
        _dispatcher = getContext().system().dispatcher();
        context().actorOf(BrokerRefresher.props(_client), "broker-refresher");
        _checkBundleRefresher = context().actorOf(CheckBundleActivator.props(_client), "check-bundle-refresher");
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("actor", this.self())
                .put("brokerName", _brokerName)
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
        if (message instanceof EmitAggregation) {
            if (_selectedBrokerCid.isPresent()) {
                final EmitAggregation aggregation = (EmitAggregation) message;
                final Collection<AggregatedData> data = aggregation.getData();
                publish(data);
            } else {
                NO_BROKER_LOG_BUILDER
                        .addData("actor", self())
                        .log();
            }
        } else if (message instanceof CheckBundleLookupResponse) {
            final CheckBundleLookupResponse response = (CheckBundleLookupResponse) message;
            if (response.isSuccess()) {
                final ServiceCheckBinding binding = response.getBinding().get();
                _bundleMap.put(response.getKey(), binding);
                _checkBundleRefresher.tell(
                        new CheckBundleActivator.NotifyCheckBundle(response.getBinding().get().getCheckBundle()),
                        self());
            } else {
                LOGGER.error()
                        .setMessage("Error creating check bundle")
                        .addData("request", response.getRequest())
                        .addData("actor", self())
                        .setThrowable(response.getCause().get())
                        .log();
                _pendingLookups.remove(response.getKey());
            }
        } else if (message instanceof BrokerRefresher.BrokerLookupComplete) {
            handleBrokerLookupComplete((BrokerRefresher.BrokerLookupComplete) message);
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

    private void handleBrokerLookupComplete(final BrokerRefresher.BrokerLookupComplete message) {
        final BrokerListResponse response = message.getResponse();
        final List<BrokerListResponse.Broker> brokers = response.getBrokers();

        Optional<BrokerListResponse.Broker> selectedBroker = Optional.absent();
        for (final BrokerListResponse.Broker broker : response.getBrokers()) {
            if (broker.getName().equalsIgnoreCase(_brokerName)) {
                selectedBroker = Optional.of(broker);
            }
        }

        if (!selectedBroker.isPresent()) {
            LOGGER.warn()
                    .setMessage("Broker list does not contain desired broker")
                    .addData("brokers", brokers)
                    .addData("desired", _brokerName)
                    .addData("actor", self())
                    .log();
        } else {
            LOGGER.info()
                    .setMessage("Broker list contains desired broker")
                    .addData("brokers", brokers)
                    .addData("desired", _brokerName)
                    .addData("actor", self())
                    .log();
            _selectedBrokerCid = Optional.of(selectedBroker.get().getCid());
        }
    }

    private void processCompletedRequest(final PostComplete complete) {
        _inflightRequestsCount--;
        final int responseStatusCode = complete.getResponse().getStatus();
        if (responseStatusCode == HttpResponseStatus.OK.code()) {
            LOGGER.debug()
                    .setMessage("Data submission accepted")
                    .addData("status", responseStatusCode)
                    .addContext("actor", self())
                    .log();
        } else {
            LOGGER.warn()
                    .setMessage("Data submission rejected")
                    .addData("status", responseStatusCode)
                    .addContext("actor", self())
                    .log();
        }
    }

    private void processFailedRequest(final PostFailure failure) {
        _inflightRequestsCount--;
        LOGGER.error()
                .setMessage("Data submission error")
                .addContext("actor", self())
                .setThrowable(failure.getCause())
                .log();
    }

    private Map<String, Object> serialize(final Collection<AggregatedData> data) {
        final Map<String, Object> dataNode = Maps.newHashMap();
        for (final AggregatedData aggregatedData : data) {
            final String name = new StringBuilder()
                    .append(aggregatedData.getPeriod().toString(ISOPeriodFormat.standard()))
                    .append("/")
                    .append(aggregatedData.getFQDSN().getMetric())
                    .append("/")
                    .append(aggregatedData.getFQDSN().getStatistic().getName())
                    .toString();
            // For histograms, if they're enabled, we'll build the histogram data node
            if (_enableHistograms && aggregatedData.getFQDSN().getStatistic() instanceof HistogramStatistic) {
                final HistogramStatistic.HistogramSupportingData histogramSupportingData = (HistogramStatistic.HistogramSupportingData)
                        aggregatedData.getSupportingData();
                final HistogramStatistic.HistogramSnapshot histogram = histogramSupportingData.getHistogramSnapshot();
                final ArrayList<String> valueList = new ArrayList<>(histogram.getEntriesCount());
                final MathContext context = new MathContext(2, RoundingMode.DOWN);
                for (final Map.Entry<Double, Integer> entry : histogram.getValues()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        final BigDecimal decimal = new BigDecimal(entry.getKey(), context);
                        final String bucketString = String.format("H[%s]=%d", decimal.toPlainString(), entry.getValue());
                        valueList.add(bucketString);
                    }
                }

                final Map<String, Object> histogramValueNode = Maps.newHashMap();
                histogramValueNode.put("_type", "n"); // Histograms are type "n"
                histogramValueNode.put("_value", valueList);
                dataNode.put(name, histogramValueNode);
            } else {
                dataNode.put(name, aggregatedData.getValue().getValue());
            }
        }
        return dataNode;
    }

    /**
      * Queues the messages for transmission.
      */
    private void publish(final Collection<AggregatedData> data) {
        // Collect the aggregated data by the "key".  In this case the key is unique part of a check_bundle:
        // service, cluster, and host
        final ImmutableListMultimap<String, AggregatedData> index = FluentIterable.from(data)
                .index(
                        input -> String.format(
                                "%s_%s_%s",
                                input.getFQDSN().getService(),
                                input.getFQDSN().getCluster(),
                                input.getHost()));

        final boolean pendingWasEmpty = _pendingRequests.isEmpty();
        final List<RequestQueueEntry> toQueue = Lists.newArrayList();

        for (final Map.Entry<String, Collection<AggregatedData>> entry : index.asMap().entrySet()) {
            final String targetKey = entry.getKey();
            final Collection<AggregatedData> serviceData = entry.getValue();
            final ServiceCheckBinding binding = _bundleMap.get(targetKey);

            if (binding != null) {
                // Queue the request(s)
                toQueue.add(new RequestQueueEntry(binding, serialize(serviceData)));
            } else {
                if (!_pendingLookups.contains(targetKey)) {
                    // We don't have an outstanding request to lookup the URI, create one.
                    final AggregatedData aggregatedData = Iterables.get(serviceData, 0);
                    _pendingLookups.add(targetKey);

                    final F.Promise<CheckBundleLookupResponse> response = createCheckBundle(targetKey, aggregatedData);

                    // Send the completed, mapped response back to ourselves.
                    Patterns.pipe(response.wrapped(), _dispatcher).to(self());
                }

                // We can't send the request to it right now, skip this service
                NO_CHECK_BUNDLE_LOG_BUILDER
                        .addData("actor", self())
                        .log();
            }
        }

        final int evicted = Math.max(0, toQueue.size() - _pendingRequests.remainingCapacity());
        _pendingRequests.addAll(toQueue);

        if (evicted > 0) {
            LOGGER.warn()
                    .setMessage("Evicted data from Circonus sink queue")
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
        final RequestQueueEntry request = _pendingRequests.poll();
        _inflightRequestsCount++;

        final F.Promise<Object> responsePromise = _client.sendToHttpTrap(request.getData(), request.getBinding()._url)
                .<Object>map(PostComplete::new)
                .recover(PostFailure::new);
        Patterns.pipe(responsePromise.wrapped(), context().dispatcher()).to(self());
    }

    private F.Promise<CheckBundleLookupResponse> createCheckBundle(
            final String targetKey,
            final AggregatedData aggregatedData) {
        final CheckBundle request = new CheckBundle.Builder()
                .addBroker(_selectedBrokerCid.get())
                .addTag("monitoring_agent:aint")
                .addTag(String.format("monitoring_cluster:%s", aggregatedData.getFQDSN().getCluster()))
                .addTag(String.format("service:%s", aggregatedData.getFQDSN().getService()))
                .addTag(String.format("hostname:%s", aggregatedData.getHost()))
                .setTarget(aggregatedData.getHost())
                .setDisplayName(
                        String.format(
                                "%s/%s",
                                aggregatedData.getFQDSN().getCluster(),
                                aggregatedData.getFQDSN().getService()))
                .setStatus("active")
                .build();

        // Map the response to a ServiceCheckBinding
        return _client.getOrCreateCheckBundle(request)
                .map(
                        response -> {
                            final URI result;
                            try {
                                result = new URI(response.getConfig().get("submission_url"));
                            } catch (final URISyntaxException e) {
                                throw Throwables.propagate(e);
                            }
                            return CheckBundleLookupResponse.success(
                                    new ServiceCheckBinding(targetKey, result, response), request);
                        },
                        _dispatcher)
                .recover(
                        failure -> CheckBundleLookupResponse.failure(targetKey, failure, request),
                        _dispatcher);
    }

    private Optional<String> _selectedBrokerCid = Optional.absent();
    private ActorRef _checkBundleRefresher;
    private int _inflightRequestsCount = 0;
    private boolean _waiting = false;

    private final ExecutionContextExecutor _dispatcher;
    private final Set<String> _pendingLookups = Sets.newHashSet();
    private final Map<String, ServiceCheckBinding> _bundleMap = Maps.newHashMap();
    private final CirconusClient _client;
    private final String _brokerName;
    private final int _maximumConcurrency;
    private final boolean _enableHistograms;
    private final int _spreadingDelayMillis;
    private final EvictingQueue<RequestQueueEntry> _pendingRequests;

    private static final Logger LOGGER = LoggerFactory.getLogger(CirconusSinkActor.class);
    private static final LogBuilder NO_BROKER_LOG_BUILDER = new RateLimitLogBuilder(
            LOGGER.warn()
                    .setMessage("Unable to push data to Circonus")
                    .addData("reason", "desired broker not yet discovered"),
            Duration.ofMinutes(1));
    private static final LogBuilder NO_CHECK_BUNDLE_LOG_BUILDER = new RateLimitLogBuilder(
            LOGGER.warn()
                    .setMessage("Unable to push data to Circonus")
                    .addData("reason", "check bundle not yet found or created"),
            Duration.ofMinutes(1));

    /**
     * Message class to wrap a list of {@link com.arpnetworking.tsdcore.model.AggregatedData}.
     */
    public static final class EmitAggregation {

        /**
         * Public constructor.
         * @param data Data to emit.
         */
        public EmitAggregation(final Collection<AggregatedData> data) {
            _data = Lists.newArrayList(data);
        }

        public Collection<AggregatedData> getData() {
            return Collections.unmodifiableList(_data);
        }

        private final List<AggregatedData> _data;
    }

    private static final class CheckBundleLookupResponse {
        public static CheckBundleLookupResponse success(final ServiceCheckBinding binding, final CheckBundle request) {
            return new CheckBundleLookupResponse(binding.getKey(), Optional.of(binding), Optional.<Throwable>absent(), request);
        }

        public static CheckBundleLookupResponse failure(final String key, final Throwable throwable, final CheckBundle request) {
            return new CheckBundleLookupResponse(key, Optional.<ServiceCheckBinding>absent(), Optional.of(throwable), request);
        }

        private CheckBundleLookupResponse(
                final String key,
                final Optional<ServiceCheckBinding> binding,
                final Optional<Throwable> cause,
                final CheckBundle request) {
            _key = key;
            _binding = binding;
            _cause = cause;
            _request = request;
        }

        public boolean isSuccess() {
            return !_cause.isPresent();
        }

        public boolean isFailed() {
            return _cause.isPresent();
        }

        public Optional<ServiceCheckBinding> getBinding() {
            return _binding;
        }

        public Optional<Throwable> getCause() {
            return _cause;
        }

        public String getKey() {
            return _key;
        }

        public CheckBundle getRequest() {
            return _request;
        }

        private final String _key;
        private final Optional<ServiceCheckBinding> _binding;
        private final Optional<Throwable> _cause;
        private final CheckBundle _request;
    }

    private static final class ServiceCheckBinding {
        public ServiceCheckBinding(final String key, final URI url, final CheckBundle checkBundle) {
            _key = key;
            _url = url;
            _checkBundle = checkBundle;
        }

        public String getKey() {
            return _key;
        }

        public URI getUrl() {
            return _url;
        }

        public CheckBundle getCheckBundle() {
            return _checkBundle;
        }

        private final String _key;
        private final URI _url;
        private final com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundle _checkBundle;
    }

    private static final class RequestQueueEntry {
        public RequestQueueEntry(final ServiceCheckBinding binding, final Map<String, Object> data) {
            _binding = binding;
            _data = data;
        }

        public ServiceCheckBinding getBinding() {
            return _binding;
        }

        public Map<String, Object> getData() {
            return _data;
        }

        private final ServiceCheckBinding _binding;
        private final Map<String, Object> _data;
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

    private static final class WaitTimeExpired {}
}

