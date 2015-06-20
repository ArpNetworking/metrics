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

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.dispatch.Recover;
import akka.pattern.Patterns;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogBuilder;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.steno.RateLimitLogBuilder;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.sinks.circonus.api.BrokerListResponse;
import com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundleRequest;
import com.arpnetworking.tsdcore.sinks.circonus.api.CheckBundleResponse;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
     * Public constructor.
     *
     * @param client Circonus client
     * @param brokerName Circonus broker to push to
     */
    public CirconusSinkActor(final CirconusClient client, final String brokerName) {
        _client = client;
        _brokerName = brokerName;
        _dispatcher = getContext().system().dispatcher();
    }

    /**
     * Creates a {@link akka.actor.Props} for use in Akka.
     *
     * @param client Circonus client
     * @param broker Circonus broker to push to
     * @return A new {@link akka.actor.Props}
     */
    public static Props props(final CirconusClient client, final String broker) {
        return Props.create(CirconusSinkActor.class, client, broker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preStart() throws Exception {
        lookupBrokers();
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "id", Integer.toHexString(System.identityHashCode(this)),
                "class", this.getClass(),
                "Actor", this.self(),
                "BrokerName", _brokerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private void lookupBrokers() {
        final Future<BrokerListResponse> responseFuture = _client.getBrokers();
        Patterns.pipe(responseFuture, _dispatcher)
                .pipeTo(getSelf(), getSelf());
        responseFuture.onComplete(
                new OnComplete<BrokerListResponse>() {
                    @Override
                   public void onComplete(final Throwable failure, final BrokerListResponse success) {
                       final FiniteDuration next;
                       if (failure != null) {
                           // On failure, retry again in 5 seconds
                           next = FiniteDuration.apply(5, TimeUnit.SECONDS);
                           LOGGER.error()
                                    .setMessage("Failed to lookup broker, trying again in 5 seconds")
                                    .setThrowable(failure)
                                    .log();
                       } else {
                           // On success, refresh in 1 hour
                           next = FiniteDuration.apply(1, TimeUnit.HOURS);
                       }
                       getContext().system().scheduler().scheduleOnce(
                               next,
                               () -> { lookupBrokers(); },
                               _dispatcher);
                   }
               },
               _dispatcher);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof BrokerListResponse) {
            final BrokerListResponse response = (BrokerListResponse) message;
            final Optional<BrokerListResponse.Broker> selectedBroker = getBroker(response);
            if (!selectedBroker.isPresent()) {
                LOGGER.warn()
                        .setMessage("Broker list does not contain desired broker")
                        .addData("brokers", response.getBrokers())
                        .addData("desired", _brokerName)
                        .addData("actor", self())
                        .log();
            } else {
                LOGGER.info()
                        .setMessage("Broker list contains desired broker")
                        .addData("brokers", response.getBrokers())
                        .addData("desired", _brokerName)
                        .addData("actor", self())
                        .log();
                _selectedBrokerCid = Optional.of(selectedBroker.get().getCid());
            }
        } else if (message instanceof EmitAggregation) {
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
            }
            _pendingLookups.remove(response.getKey());
        } else {
            unhandled(message);
        }
    }

    private Optional<BrokerListResponse.Broker> getBroker(final BrokerListResponse response) {
        for (final BrokerListResponse.Broker broker : response.getBrokers()) {
            if (broker.getName().equalsIgnoreCase(_brokerName)) {
                return Optional.of(broker);
            }
        }
        return Optional.absent();
    }

    private Collection<Map<String, Object>> serialize(final Collection<AggregatedData> data) {
        final Map<String, Object> dataNode = Maps.newHashMap();
        for (final AggregatedData aggregatedData : data) {
            final String name = new StringBuilder()
                    .append(aggregatedData.getPeriod().toString(ISOPeriodFormat.standard()))
                    .append("_")
                    .append(aggregatedData.getFQDSN().getMetric())
                    .append("_")
                    .append(aggregatedData.getFQDSN().getStatistic().getName())
                    .toString();
            dataNode.put(name, aggregatedData.getValue().getValue());
        }
        return Collections.singletonList(dataNode);
    }

    private void publish(final Collection<AggregatedData> data) {
        // Collect the aggregated data by the "key".  In this case the key is unique part of a check_bundle:
        // service, cluster, and host
        final ImmutableListMultimap<String, AggregatedData> index = FluentIterable.from(data).index(
                new Function<AggregatedData, String>() {
                    @Override
                    public String apply(final AggregatedData input) {
                        return String.format(
                                "%s_%s_%s",
                                input.getFQDSN().getService(),
                                input.getFQDSN().getCluster(),
                                input.getHost());
                    }
                }
        );

        for (final Map.Entry<String, Collection<AggregatedData>> entry : index.asMap().entrySet()) {
            final String targetKey = entry.getKey();
            final Collection<AggregatedData> serviceData = entry.getValue();
            final ServiceCheckBinding binding = _bundleMap.get(targetKey);
            if (binding != null) {
                // Send the request(s)
                for (final Map<String, Object> serialized : serialize(serviceData)) {
                    _client.sendToHttpTrap(serialized, binding._url);
                }
            } else {
                if (!_pendingLookups.contains(targetKey)) {
                    // We don't have an outstanding request to lookup the URI, create one.
                    final AggregatedData aggregatedData = Iterables.get(serviceData, 0);
                    _pendingLookups.add(targetKey);

                    final Future<CheckBundleLookupResponse> response = createCheckBundle(targetKey, aggregatedData);

                    // Send the completed, mapped response back to ourselves.
                    Patterns.pipe(response, _dispatcher).to(self());
                }

                // We can't send the request to it right now, skip this service
                NO_CHECK_BUNDLE_LOG_BUILDER
                        .addData("actor", self())
                        .log();
                continue;
            }
        }
    }

    private Future<CheckBundleLookupResponse> createCheckBundle(
            final String targetKey,
            final AggregatedData aggregatedData) {
        final CheckBundleRequest request = new CheckBundleRequest.Builder()
                .addBroker(_selectedBrokerCid.get())
                .addTag("AINT")
                .addTag(String.format("cluster:%s", aggregatedData.getFQDSN().getCluster()))
                .addTag(String.format("service:%s", aggregatedData.getFQDSN().getService()))
                .addTag(String.format("host:%s", aggregatedData.getHost()))
                .setTarget(aggregatedData.getHost())
                .setDisplayName(
                        String.format(
                                "aint_%s_%s_%s",
                                aggregatedData.getFQDSN().getCluster(),
                                aggregatedData.getHost(),
                                aggregatedData.getFQDSN().getService()))
                .build();

        // Map the response to a ServiceCheckBinding
        return _client.getOrCreateCheckBundle(request)
                .map(
                        new Mapper<CheckBundleResponse, CheckBundleLookupResponse>() {
                            @Override
                            public CheckBundleLookupResponse apply(final CheckBundleResponse response) {
                                return CheckBundleLookupResponse.success(
                                        new ServiceCheckBinding(targetKey, response.getUrl()));
                            }
                        },
                        _dispatcher)
                .recover(
                        new Recover<CheckBundleLookupResponse>() {
                            @Override
                            public CheckBundleLookupResponse recover(final Throwable failure) {
                                LOGGER.error()
                                        .setMessage("Error creating check bundle")
                                        .addData("request", request)
                                        .addData("actor", self())
                                        .setThrowable(failure)
                                        .log();
                                return CheckBundleLookupResponse.failure(targetKey, failure);
                            }
                        },
                        _dispatcher);
    }

    private Optional<String> _selectedBrokerCid = Optional.absent();

    private final ExecutionContextExecutor _dispatcher;
    private final Set<String> _pendingLookups = Sets.newHashSet();
    private final Map<String, ServiceCheckBinding> _bundleMap = Maps.newHashMap();
    private final CirconusClient _client;
    private final String _brokerName;

    private static final Logger LOGGER = LoggerFactory.getLogger(CirconusSinkActor.class);
    private static final LogBuilder NO_BROKER_LOG_BUILDER = new RateLimitLogBuilder(
            LOGGER.warn()
                    .setMessage("Unable to push data to Circonus")
                    .addData("reason", "desired broker not yet discovered"),
            Period.minutes(1));
    private static final LogBuilder NO_CHECK_BUNDLE_LOG_BUILDER = new RateLimitLogBuilder(
            LOGGER.warn()
                    .setMessage("Unable to push data to Circonus")
                    .addData("reason", "check bundle not yet found or created"),
            Period.minutes(1));

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
        public static CheckBundleLookupResponse success(final ServiceCheckBinding binding) {
            return new CheckBundleLookupResponse(binding.getKey(), Optional.of(binding), Optional.<Throwable>absent());
        }

        public static CheckBundleLookupResponse failure(final String key, final Throwable throwable) {
            return new CheckBundleLookupResponse(key, Optional.<ServiceCheckBinding>absent(), Optional.of(throwable));
        }

        private CheckBundleLookupResponse(
                final String key,
                final Optional<ServiceCheckBinding> binding,
                final Optional<Throwable> cause) {
            _key = key;
            _binding = binding;
            _cause = cause;
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

        private final String _key;
        private final Optional<ServiceCheckBinding> _binding;
        private final Optional<Throwable> _cause;
    }

    private static final class ServiceCheckBinding {
        public ServiceCheckBinding(final String key, final URI url) {
            _key = key;
            _url = url;
        }

        public String getKey() {
            return _key;
        }

        public URI getUrl() {
            return _url;
        }

        private final String _key;
        private final URI _url;
    }
}
