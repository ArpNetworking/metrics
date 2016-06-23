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

import com.arpnetworking.commons.jackson.databind.ObjectMapperFactory;
import com.arpnetworking.configuration.Configuration;
import com.arpnetworking.configuration.Listener;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.DynamicConfigurationFactory;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.scripting.Expression;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.tsdcore.scripting.lua.LuaExpression;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.internal.MoreTypes;
import net.sf.oval.constraint.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Intermediate publisher which computes additional <code>AggregatedData</code>
 * instances using configured expressions.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class ExpressionSink extends BaseSink implements Sink {

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

        final Collection<AggregatedData> newData;

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
                // or service is found that its expressions will not be
                // evaluated (this includes after every restart).
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
            metrics.setGauge("sinks/expression_sink/" + getMetricSafeName() + "/cluster_services", _clusterServices.size());

            // Evaluate all expressions currently loaded
            newData = evaluateExpressions(periodicData, metrics);
        }

        // Invoke nested sink
        _sink.recordAggregateData(
                PeriodicData.Builder.clone(periodicData, new PeriodicData.Builder())
                        .setData(ImmutableList.copyOf(newData))
                        .build());
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
                .put("clusterServices", _clusterServices)
                .put("expressions", _expressions)
                .put("sink", _sink)
                .build();
    }

    private ImmutableList<AggregatedData> evaluateExpressions(
            final PeriodicData periodicData,
            final Metrics metrics) {

        // Evalaute expressions
        final Counter evaluations = metrics.createCounter("sinks/expression_sink/" + getMetricSafeName() + "/evaluations");
        final Counter failures = metrics.createCounter("sinks/expression_sink/" + getMetricSafeName() + "/failures");
        final Counter missing = metrics.createCounter("sinks/expression_sink/" + getMetricSafeName() + "/missing");
        final ImmutableList.Builder<AggregatedData> newDataBuilder = ImmutableList.builder();
        newDataBuilder.addAll(periodicData.getData());
        final List<Expression> expressions = _expressions.get();
        if (expressions != null) {
            for (final Expression expression : expressions) {
                try {
                    evaluations.increment();
                    // NOTE: This is a bottom-less performance pit of doom. We need to convert the evaluation
                    // from a single expression to all expressions.
                    final Optional<AggregatedData> result = expression.evaluate(
                            PeriodicData.Builder.clone(periodicData, new PeriodicData.Builder())
                                    .setData(newDataBuilder.build())
                                    .build());
                    if (!result.isPresent()) {
                        missing.increment();
                    } else {
                        newDataBuilder.add(result.get());
                    }
                } catch (final ScriptingException e) {
                    failures.increment();
                    LOGGER.warn()
                            .setMessage("Expression evaluation failed")
                            .addData("expression", expression)
                            .addData("data", newDataBuilder.build())
                            .setThrowable(e)
                            .log();
                }
            }
        }
        return newDataBuilder.build();
    }

    private ExpressionSink(final Builder builder) {
        super(builder);
        _metricsFactory = builder._metricsFactory;
        _dynamicConfigurationFactory = builder._dynamicConfigurationFactory;
        _configurationListener = new ConfigurationListener(_expressions);
        _sink = builder._sink;
    }

    private final MetricsFactory _metricsFactory;
    private final DynamicConfigurationFactory _dynamicConfigurationFactory;
    private final Listener _configurationListener;
    private final Sink _sink;
    private final Set<DynamicConfigurationFactory.Key> _clusterServices = Sets.newConcurrentHashSet();
    private final AtomicReference<DynamicConfiguration> _configuration = new AtomicReference<>();
    private final AtomicReference<List<Expression>> _expressions = new AtomicReference<>();

    private static final ParameterizedType EXPRESSION_TYPE = new MoreTypes.ParameterizedTypeImpl(
            null, // OwnerType
            List.class,
            LuaExpression.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionSink.class);

    static {
        final SimpleModule module = new SimpleModule("ExpressionSink");

        final Set<Class<? extends Expression>> expressionClasses = INTERFACE_DATABASE.findClassesWithInterface(Expression.class);
        for (final Class<? extends Expression> expressionClass : expressionClasses) {
            BuilderDeserializer.addTo(module, expressionClass);
        }

        OBJECT_MAPPER.registerModules(module);
    }

    /* package private */ static class ConfigurationListener implements Listener {

        /* package private */ ConfigurationListener(final AtomicReference<List<Expression>> acceptedExpressions) {
            _acceptedExpressions = acceptedExpressions;
        }

        @Override
        public void offerConfiguration(final Configuration configuration) throws Exception {
            // Deserialize all expressions
            final List<Expression> expressions = configuration.getAs(
                    EXPRESSION_TYPE,
                    Collections.<Expression>emptyList());

            // Index all expressions by target FQDSN
            // NOTE: This will throw an IllegalArgumentException if more than
            // one expression targets the same FQDSN.
            final Map<FQDSN, Expression> expressionsByFqdsn = Maps.uniqueIndex(
                    expressions,
                    Expression::getTargetFQDSN);

            // Build the ordered set of expressions from bottom-up
            // NOTE: This will throw an IllegalArgumentException if any
            // expression depends on a parent expression.
            final Set<FQDSN> orderedExpressions = Sets.newLinkedHashSet();
            for (final Expression expression : expressionsByFqdsn.values()) {
                final Set<FQDSN> parentExpressions = Sets.newHashSet();
                insertExpression(expression, expressionsByFqdsn, parentExpressions, orderedExpressions);
            }

            // Map the ordered expression FQDSNs back to expressions
            _offeredExpressions = orderedExpressions.stream().map(
                    expressionsByFqdsn::get).collect(Collectors.toList());
        }

        @Override
        public void applyConfiguration() {
            _acceptedExpressions.set(_offeredExpressions);
            LOGGER.debug()
                    .setMessage("Updated expressions")
                    .addData("expressions", _acceptedExpressions)
                    .log();
        }

        private void insertExpression(
                final Expression expression,
                final Map<FQDSN, Expression> expressionsByFqdsn,
                final Set<FQDSN> parentExpressions,
                final Set<FQDSN> orderedExpressions) {
            final FQDSN fqdsn = expression.getTargetFQDSN();
            if (!parentExpressions.contains(fqdsn)) {
                // Evaluate the sub-graph only if it has not been processed
                if (!orderedExpressions.contains(fqdsn)) {
                    // Add yourself as a parent
                    parentExpressions.add(fqdsn);

                    // Process all your dependencies. If any transitive dependency
                    // references a parent then there is a cycle.
                    for (final FQDSN dependencyFqdsn : expression.getDependencies()) {
                        final Expression dependency = expressionsByFqdsn.get(dependencyFqdsn);
                        // Only ensure expression dependencies are ordered; all non-expression
                        // dependencies are evaluated before expressions and are thus available.
                        if (dependency != null) {
                            insertExpression(
                                    dependency,
                                    expressionsByFqdsn,
                                    parentExpressions,
                                    orderedExpressions);
                        }
                    }

                    // Record yourself in the expression evaluation order. At this
                    // point all your dependencies are already in the list and none
                    // of them depend on you or your parents.
                    orderedExpressions.add(fqdsn);

                    // Remove yourself as a parent
                    parentExpressions.remove(fqdsn);
                }
            } else {
                throw new IllegalArgumentException(String.format(
                        "Expression dependency cycle detected; expression=%s, parents=%s",
                        expression,
                        parentExpressions));
            }
        }

        private final AtomicReference<List<Expression>> _acceptedExpressions;
        private List<Expression> _offeredExpressions = Collections.emptyList();
    }

    /**
     * <code>Builder</code> for <code>ExpressionSink</code>.
     */
    public static final class Builder extends BaseSink.Builder<Builder, ExpressionSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(ExpressionSink.class);
        }

        /**
         * The aggregated data sink to wrap. Cannot be null.
         *
         * @param value The aggregated data sink to wrap.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setDynamicConfigurationFactory(final DynamicConfigurationFactory value) {
            _dynamicConfigurationFactory = value;
            return this;
        }

        /**
         * The aggregated data sink to buffer. Cannot be null.
         *
         * @param value The aggregated data sink to buffer.
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
