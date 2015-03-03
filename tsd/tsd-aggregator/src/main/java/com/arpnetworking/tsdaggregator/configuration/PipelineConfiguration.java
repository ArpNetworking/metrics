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
package com.arpnetworking.tsdaggregator.configuration;

import com.arpnetworking.configuration.jackson.DynamicConfigurationFactory;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.tsdcore.parsers.Parser;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.tsdcore.statistics.CountStatistic;
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.arpnetworking.tsdcore.statistics.MedianStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.SumStatistic;
import com.arpnetworking.tsdcore.statistics.TP0Statistic;
import com.arpnetworking.tsdcore.statistics.TP100Statistic;
import com.arpnetworking.tsdcore.statistics.TP90Statistic;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector;
import com.fasterxml.jackson.module.guice.GuiceInjectableValues;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Representation of TsdAggregator pipeline configuration. Each pipeline can
 * define one or more sources and one or more sinks.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class PipelineConfiguration {

    /**
     * Create an <code>ObjectMapper</code> for Pipeline configuration.
     *
     * @param injector The Guice <code>Injector</code> instance.
     * @return An <code>ObjectMapper</code> for Pipeline configuration.
     */
    public static ObjectMapper createObjectMapper(final Injector injector) {
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();

        final SimpleModule module = new SimpleModule("Pipeline");
        BuilderDeserializer.addTo(module, PipelineConfiguration.class);

        final Set<Class<? extends Sink>> sinkClasses = INTERFACE_DATABASE.findClassesWithInterface(Sink.class);
        for (final Class<? extends Sink> sinkClass : sinkClasses) {
            BuilderDeserializer.addTo(module, sinkClass);
        }

        final Set<Class<? extends Source>> sourceClasses = INTERFACE_DATABASE.findClassesWithInterface(Source.class);
        for (final Class<? extends Source> sourceClass : sourceClasses) {
            BuilderDeserializer.addTo(module, sourceClass);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Set<Class<? extends Parser<?>>> parserClasses = INTERFACE_DATABASE.findClassesWithInterface((Class) Parser.class);
        for (final Class<? extends Parser<?>> parserClass : parserClasses) {
            BuilderDeserializer.addTo(module, parserClass);
        }

        final Set<Class<? extends DynamicConfigurationFactory>> dcFactoryClasses =
                INTERFACE_DATABASE.findClassesWithInterface(DynamicConfigurationFactory.class);
        for (final Class<? extends DynamicConfigurationFactory> dcFactoryClass : dcFactoryClasses) {
            BuilderDeserializer.addTo(module, dcFactoryClass);
        }

        objectMapper.registerModules(module);

        final GuiceAnnotationIntrospector guiceIntrospector = new GuiceAnnotationIntrospector();
        objectMapper.setInjectableValues(new GuiceInjectableValues(injector));
        objectMapper.setAnnotationIntrospectors(
                new AnnotationIntrospectorPair(
                        guiceIntrospector, objectMapper.getSerializationConfig().getAnnotationIntrospector()),
                new AnnotationIntrospectorPair(
                        guiceIntrospector, objectMapper.getDeserializationConfig().getAnnotationIntrospector()));

        return objectMapper;
    }

    public String getName() {
        return _name;
    }

    public String getService() {
        return _service;
    }

    public Optional<String> getHost() {
        return _host;
    }

    public String getCluster() {
        return _cluster;
    }

    public List<Source> getSources() {
        return _sources;
    }

    public List<Sink> getSinks() {
        return _sinks;
    }

    public Set<Period> getPeriods() {
        return _periods;
    }

    public Set<Statistic> getTimerStatistic() {
        return _timerStatistic;
    }

    public Set<Statistic> getCounterStatistic() {
        return _counterStatistic;
    }

    public Set<Statistic> getGaugeStatistic() {
        return _gaugeStatistic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Name", _name)
                .add("Service", _service)
                .add("Cluster", _cluster)
                .add("Sources", _sources)
                .add("Sinks", _sinks)
                .add("Periods", _periods)
                .add("TimerStatistic", _timerStatistic)
                .add("CounterStatistic", _counterStatistic)
                .add("GaugeStatistic", _gaugeStatistic)
                .toString();
    }

    private PipelineConfiguration(final Builder builder) {
        _name = builder._name;
        _service = builder._service;
        _cluster = builder._cluster;
        _host = Optional.fromNullable(builder._host);
        _sources = ImmutableList.copyOf(builder._sources);
        _sinks = ImmutableList.copyOf(builder._sinks);
        _periods = ImmutableSet.copyOf(builder._periods);
        _timerStatistic = ImmutableSet.copyOf(builder._timerStatistics);
        _counterStatistic = ImmutableSet.copyOf(builder._counterStatistics);
        _gaugeStatistic = ImmutableSet.copyOf(builder._gaugeStatistics);
    }

    private final String _name;
    private final String _service;
    private final String _cluster;
    private final Optional<String> _host;
    private final ImmutableList<Source> _sources;
    private final ImmutableList<Sink> _sinks;
    private final ImmutableSet<Period> _periods;
    private final ImmutableSet<Statistic> _timerStatistic;
    private final ImmutableSet<Statistic> _counterStatistic;
    private final ImmutableSet<Statistic> _gaugeStatistic;

    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();

    /**
     * Implementation of builder pattern for <code>PipelineConfiguration</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends OvalBuilder<PipelineConfiguration> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(PipelineConfiguration.class);
        }

        /**
         * The name of the pipeline. Cannot be null or empty.
         *
         * @param value The name of the pipeline.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setName(final String value) {
            _name = value;
            return this;
        }

        /**
         * The name of the host. Optional. If not provided, it will be looked up at runtime.
         *
         * @param value The name of the host.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHost(final String value) {
            _host = value;
            return this;
        }

        /**
         * The name of the service processed by this pipeline. Cannot be null or
         * empty.
         *
         * @param value The name of the service.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setService(final String value) {
            _service = value;
            return this;
        }
        /**
         * The name of the cluster processed by this pipeline. Cannot be null or
         * empty.
         *
         * @param value The name of the cluster.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return this;
        }

        /**
         * The query log sources. Cannot be null.
         *
         * @param value The query log sources.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSources(final List<Source> value) {
            _sources = value;
            return this;
        }

        /**
         * The sinks. Cannot be null.
         *
         * @param value The sinks.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSinks(final List<Sink> value) {
            _sinks = value;
            return this;
        }

        /**
         * The aggregation periods. Cannot be null or empty. Default is one
         * second and five minute periods.
         *
         * @param value The sinks.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPeriods(final Set<Period> value) {
            _periods = value;
            return this;
        }

        /**
         * The statistics to compute for all timers. Cannot be null or empty.
         * Default is TP50, TP90, TP99, Mean and Count.
         *
         * @param value The timer statistics.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setTimerStatistics(final Set<Statistic> value) {
            _timerStatistics = value;
            return this;
        }

        /**
         * The statistics to compute for all counters. Cannot be null or empty.
         * Default is Mean, Sum and Count.
         *
         * @param value The counter statistics.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCounterStatistics(final Set<Statistic> value) {
            _counterStatistics = value;
            return this;
        }

        /**
         * The statistics to compute for all gauges. Cannot be null or empty.
         * Default is Min, Max and Mean.
         *
         * @param value The gauge statistics.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setGaugeStatistics(final Set<Statistic> value) {
            _gaugeStatistics = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _name;
        @NotNull
        @NotEmpty
        private String _service;
        @NotNull
        @NotEmpty
        private String _cluster;
        @NotEmpty
        private String _host;
        @NotNull
        private List<Source> _sources = Collections.emptyList();
        @NotNull
        private List<Sink> _sinks = Collections.emptyList();
        @NotNull
        @NotEmpty
        private Set<Period> _periods = Sets.newHashSet(Period.seconds(1), Period.minutes(1));
        @NotNull
        @NotEmpty
        private Set<Statistic> _timerStatistics = Sets.<Statistic>newHashSet(
                new MedianStatistic(), new TP90Statistic(), new TP99Statistic(), new MeanStatistic(), new CountStatistic());
        @NotNull
        @NotEmpty
        private Set<Statistic> _counterStatistics = Sets.<Statistic>newHashSet(
                new MeanStatistic(), new SumStatistic(), new CountStatistic());
        @NotNull
        @NotEmpty
        private Set<Statistic> _gaugeStatistics = Sets.<Statistic>newHashSet(
                new TP0Statistic(), new TP100Statistic(), new MeanStatistic());
    }
}
