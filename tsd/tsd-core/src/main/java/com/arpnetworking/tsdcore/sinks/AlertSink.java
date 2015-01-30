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
import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.scripting.Alert;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.internal.MoreTypes;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

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
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        final List<Condition> newConditions = Lists.newArrayList();

        try (final Metrics metrics = _metricsFactory.create()) {
            // Check for new clusters or services
            boolean newClusterServices = false;
            for (final AggregatedData datum : data) {
                final String clusterService = datum.getFQDSN().getCluster() + "." + datum.getFQDSN().getService();
                if (!_clusterServices.contains(clusterService)) {
                    LOGGER.debug(String.format(
                            "Discovered new cluster-service; cluster=%s, service=%s, alertSink=%s",
                            datum.getFQDSN().getCluster(),
                            datum.getFQDSN().getService(),
                            this));
                    _clusterServices.add(clusterService);
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
                        _clusterServices,
                        Collections.<Pattern>emptySet());
                final DynamicConfiguration oldConfiguration = _configuration.getAndSet(newConfiguration);
                if (oldConfiguration != null) {
                    oldConfiguration.shutdown();
                }
                newConfiguration.launch();
            }
            metrics.setGauge("Sinks/AlertSink/" + getMetricSafeName() + "/ClusterServices", _clusterServices.size());

            // Evaluate all expressions currently loaded
            evaluateAlerts(data, newConditions, metrics);
        }

        // Invoke nested sink
        if (newConditions.isEmpty()) {
            _sink.recordAggregateData(data, conditions);
        } else {
            newConditions.addAll(conditions);
            _sink.recordAggregateData(data, newConditions);
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

    private void evaluateAlerts(
            final Collection<AggregatedData> data,
            final Collection<Condition> newConditions,
            final Metrics metrics) {

        // ** HACK ** HACK ** HACK ** HACK **
        //
        // See ExpressionSink for details.
        //

        final AggregatedData hackDatum = Iterables.getFirst(data, null);
        final String host = hackDatum != null ? hackDatum.getHost() : null;
        final Period period = hackDatum != null ? hackDatum.getPeriod() : null;
        final DateTime periodStart = hackDatum != null ? hackDatum.getPeriodStart() : null;

        // ** HACK ** HACK ** HACK ** HACK **

        // Evaluate alerts
        final Counter evaluations = metrics.createCounter("Sinks/AlertSink/" + getMetricSafeName() + "/Evaluations");
        final Counter failures = metrics.createCounter("Sinks/AlertSink/" + getMetricSafeName() + "/Failures");
        final Counter missing = metrics.createCounter("Sinks/AlertSink/" + getMetricSafeName() + "/Missing");
        final List<Alert> alerts = _alerts.get();
        if (alerts != null) {
            for (final Alert alert : alerts) {
                try {
                    // Evaluate the alert and store the result
                    evaluations.increment();
                    final Condition condition = alert.evaluate(
                            host,
                            period,
                            periodStart,
                            data);
                    if (!condition.isTriggered().isPresent()) {
                        missing.increment();
                    }
                    newConditions.add(condition);
                } catch (final ScriptingException e) {
                    // TODO(vkoskela): Configure an alert failure alert! [MAI-450]
                    failures.increment();
                    LOGGER.warn(
                            String.format(
                                    "Alert evaluation failed; alert=%s, data=%s",
                                    alert,
                                    data),
                            e);
                }
            }
        }
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
    private final Set<String> _clusterServices = Sets.newConcurrentHashSet();
    private final AtomicReference<DynamicConfiguration> _configuration = new AtomicReference<>();
    private final AtomicReference<List<Alert>> _alerts = new AtomicReference<>();

    private static final ParameterizedType ALERT_TYPE = new MoreTypes.ParameterizedTypeImpl(
            null, // OwnerType
            Map.class,
            String.class,
            new MoreTypes.ParameterizedTypeImpl(
                null, // OwnerType
                List.class,
                Alert.class));
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertSink.class);

    static {
        final SimpleModule module = new SimpleModule("AlertSink");

        final Set<Class<? extends Alert>> alertClasses = INTERFACE_DATABASE.findClassesWithInterface(Alert.class);
        for (final Class<? extends Alert> alertClass : alertClasses) {
            BuilderDeserializer.addTo(module, alertClass);
        }

        OBJECT_MAPPER.registerModules(module);
    }

    private final class ConfigurationListener implements Listener {

        @Override
        public void offerConfiguration(final Configuration configuration) throws Exception {
            final Map<String, List<Alert>> alerts = configuration.getAs(
                    ALERT_TYPE,
                    Collections.<String, List<Alert>>emptyMap());
            _offeredAlerts = Lists.newArrayList();
            for (final List<Alert> list : alerts.values()) {
                _offeredAlerts.addAll(list);
            }
        }

        @Override
        public void applyConfiguration() {
            _alerts.set(_offeredAlerts);
            LOGGER.debug(String.format("Updated alerts; alerts=%s", _alerts));
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
