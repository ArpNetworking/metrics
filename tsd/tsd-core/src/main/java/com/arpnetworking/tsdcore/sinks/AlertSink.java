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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.configuration.Configuration;
import com.arpnetworking.configuration.Listener;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.DynamicConfigurationFactory;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.scripting.Alert;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.tsdcore.scripting.lua.LuaAlert;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticDeserializer;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.internal.MoreTypes;
import net.sf.oval.constraint.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Intermediate publisher which computes and annotates <code>AggregatedData</code>
 * with alert or alarm conditions.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class AlertSink extends BaseSink implements Sink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final PeriodicData periodicData) {
        LOGGER.debug()
                .setMessage("Writing aggregated data")
                .addData("sink", getName())
                .addData("dataSize", periodicData.getData().size())
                .addData("conditionsSize", periodicData.getConditions().size())
                .log();

        final ImmutableList.Builder<Condition> newConditions = ImmutableList.builder();
        boolean haveNewConditions = false;

        try (final Metrics metrics = _metricsFactory.create()) {
            // Check for new clusters or services
            boolean newClusterServices = false;
            for (final AggregatedData datum : periodicData.getData()) {
                final  DynamicConfigurationFactory.Key clusterServiceKey = new DynamicConfigurationFactory.Key(
                        datum.getFQDSN().getCluster(),
                        datum.getFQDSN().getService());
                if (!_clusterServices.contains(clusterServiceKey)) {
                    LOGGER.debug()
                            .setMessage("Discovered new cluster-service")
                            .addData("sink", getName())
                            .addData("cluster", datum.getFQDSN().getCluster())
                            .addData("service", datum.getFQDSN().getService())
                            .log();
                    _clusterServices.add(clusterServiceKey);
                    newClusterServices = true;
                }
            }
            if (newClusterServices) {
                // NOTE: Dynamic configuration loading is asynchronous and
                // therefore it is possible that the first few times a cluster
                // or service is found that its alerts will not be evaluated
                // (this includes after every restart).
                final DynamicConfiguration newConfiguration = _dynamicConfigurationFactory.create(
                        new DynamicConfiguration.Builder()
                                .setObjectMapper(OBJECT_MAPPER)
                                .addListener(_configurationListener),
                        _clusterServices);
                final DynamicConfiguration oldConfiguration = _configuration.getAndSet(newConfiguration);
                if (oldConfiguration != null) {
                    oldConfiguration.shutdown();
                }
                newConfiguration.launch();
            }
            metrics.setGauge("sinks/alert/" + getMetricSafeName() + "/cluster_services", _clusterServices.size());

            // Evaluate all expressions currently loaded
            haveNewConditions = evaluateAlerts(periodicData, newConditions, metrics);
        }

        // Invoke nested sink
        if (!haveNewConditions) {
            _sink.recordAggregateData(periodicData);
        } else {
            newConditions.addAll(periodicData.getConditions());
            _sink.recordAggregateData(
                    PeriodicData.Builder.clone(periodicData, new PeriodicData.Builder())
                            .setConditions(newConditions.build())
                            .build());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        final DynamicConfiguration configuration = _configuration.get();
        if (configuration != null) {
            configuration.shutdown();
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    @Override
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("super", super.toLogValue())
                .put("alerts", _alerts)
                .put("clusterServices", _clusterServices)
                .put("sink", _sink)
                .build();
    }

    private boolean evaluateAlerts(
            final PeriodicData periodicData,
            final ImmutableList.Builder<Condition> newConditions,
            final Metrics metrics) {

        boolean haveNewConditions = false;

        // Evaluate alerts
        final Counter evaluations = metrics.createCounter("sinks/alert/" + getMetricSafeName() + "/evaluations");
        final Counter failures = metrics.createCounter("sinks/alert/" + getMetricSafeName() + "/failures");
        final Counter missing = metrics.createCounter("sinks/alert/" + getMetricSafeName() + "/missing");
        final List<Alert> alerts = _alerts.get();
        if (alerts != null) {
            for (final Alert alert : alerts) {
                try {
                    // Evaluate the alert and store the result
                    evaluations.increment();
                    final Condition condition = alert.evaluate(periodicData);
                    if (!condition.isTriggered().isPresent()) {
                        missing.increment();
                    }
                    haveNewConditions = true;
                    newConditions.add(condition);
                } catch (final ScriptingException e) {
                    // TODO(vkoskela): Configure an alert failure alert! [MAI-450]
                    failures.increment();
                    LOGGER.warn()
                            .setMessage("Failed to evalaute alert")
                            .addData("alert", alert)
                            .addData("data", periodicData.getData())
                            .setThrowable(e)
                            .log();
                }
            }
        }

        return haveNewConditions;
    }

    private AlertSink(final Builder builder) {
        super(builder);
        _metricsFactory = builder._metricsFactory;
        _dynamicConfigurationFactory = builder._dynamicConfigurationFactory;
        _configurationListener = new ConfigurationListener();
        _sink = builder._sink;
    }

    private final MetricsFactory _metricsFactory;
    private final DynamicConfigurationFactory _dynamicConfigurationFactory;
    private final Listener _configurationListener;
    private final Sink _sink;
    private final Set<DynamicConfigurationFactory.Key> _clusterServices = Sets.newConcurrentHashSet();
    private final AtomicReference<DynamicConfiguration> _configuration = new AtomicReference<>();
    private final AtomicReference<List<Alert>> _alerts = new AtomicReference<>();

    private static final ParameterizedType ALERT_TYPE = new MoreTypes.ParameterizedTypeImpl(
            null, // OwnerType
            List.class,
            LuaAlert.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertSink.class);

    static {
        final SimpleModule module = new SimpleModule("AlertSink");
        module.addDeserializer(Statistic.class, new StatisticDeserializer());

        final Set<Class<? extends Alert>> alertClasses = INTERFACE_DATABASE.findClassesWithInterface(Alert.class);
        for (final Class<? extends Alert> alertClass : alertClasses) {
            BuilderDeserializer.addTo(module, alertClass);
        }

        OBJECT_MAPPER.registerModules(module);
    }

    private final class ConfigurationListener implements Listener {

        @Override
        public void offerConfiguration(final Configuration configuration) throws Exception {
            final List<Alert> alerts = configuration.getAs(
                    ALERT_TYPE,
                    Collections.<Alert>emptyList());
            _offeredAlerts = Lists.newArrayList(alerts);
        }

        @Override
        public void applyConfiguration() {
            _alerts.set(_offeredAlerts);
            LOGGER.debug()
                    .setMessage("Updated alerts")
                    .addData("alerts", _alerts)
                    .log();
        }

        private List<Alert> _offeredAlerts = Collections.emptyList();
    }

    /**
     * <code>Builder</code> for <code>AlertSink</code>.
     */
    public static final class Builder extends BaseSink.Builder<Builder, AlertSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(AlertSink.class);
        }

        /**
         * The aggregated data sink to buffer. Cannot be null.
         *
         * @param value The aggregated data sink to buffer.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setDynamicConfigurationFactory(final DynamicConfigurationFactory value) {
            _dynamicConfigurationFactory = value;
            return this;
        }

        /**
         * The aggregated data sink to wrap. Cannot be null.
         *
         * @param value The aggregated data sink to wrap.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * Instance of <code>MetricsFactory</code>. Cannot be null. This field
         * may be injected automatically by Jackson/Guice if setup to do so.
         *
         * @param value Instance of <code>MetricsFactory</code>.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMetricsFactory(final MetricsFactory value) {
            _metricsFactory = value;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private DynamicConfigurationFactory _dynamicConfigurationFactory;
        @NotNull
        private Sink _sink;
        @JacksonInject
        @NotNull
        private MetricsFactory _metricsFactory;
    }
}
