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

import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.tsdcore.exceptions.ConfigurationException;
import com.arpnetworking.tsdcore.parsers.Parser;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.tsdcore.statistics.MeanStatistic;
import com.arpnetworking.tsdcore.statistics.NStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.SumStatistic;
import com.arpnetworking.tsdcore.statistics.TP0Statistic;
import com.arpnetworking.tsdcore.statistics.TP100Statistic;
import com.arpnetworking.tsdcore.statistics.TP50Statistic;
import com.arpnetworking.tsdcore.statistics.TP90Statistic;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.exception.ConstraintsViolatedException;

import org.joda.time.Period;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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

    public String getName() {
        return _name;
    }

    public String getServiceName() {
        return _serviceName;
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
        return Objects.toStringHelper(this)
                .add("Name", _name)
                .add("ServiceName", _serviceName)
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
        _serviceName = builder._serviceName;
        _sources = ImmutableList.copyOf(builder._sources);
        _sinks = ImmutableList.copyOf(builder._sinks);
        _periods = ImmutableSet.copyOf(builder._periods);
        _timerStatistic = ImmutableSet.copyOf(builder._timerStatistics);
        _counterStatistic = ImmutableSet.copyOf(builder._counterStatistics);
        _gaugeStatistic = ImmutableSet.copyOf(builder._gaugeStatistics);
    }

    private final String _name;
    private final String _serviceName;
    private final ImmutableList<Source> _sources;
    private final ImmutableList<Sink> _sinks;
    private final ImmutableSet<Period> _periods;
    private final ImmutableSet<Statistic> _timerStatistic;
    private final ImmutableSet<Statistic> _counterStatistic;
    private final ImmutableSet<Statistic> _gaugeStatistic;

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
         * The name of the service processed by this pipeline. Cannot be null or
         * empty.
         * 
         * @param value The name of the service.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setServiceName(final String value) {
            _serviceName = value;
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
        private String _serviceName;
        @NotNull
        private List<Source> _sources = Collections.emptyList();
        @NotNull
        private List<Sink> _sinks = Collections.emptyList();
        @NotNull
        @NotEmpty
        private Set<Period> _periods = Sets.newHashSet(Period.seconds(1), Period.minutes(5));
        @NotNull
        @NotEmpty
        private Set<Statistic> _timerStatistics = Sets.<Statistic>newHashSet(
                new TP50Statistic(), new TP90Statistic(), new TP99Statistic(), new MeanStatistic(), new NStatistic());
        @NotNull
        @NotEmpty
        private Set<Statistic> _counterStatistics = Sets.<Statistic>newHashSet(
                new MeanStatistic(), new SumStatistic(), new NStatistic());
        @NotNull
        @NotEmpty
        private Set<Statistic> _gaugeStatistics = Sets.<Statistic>newHashSet(
                new TP0Statistic(), new TP100Statistic(), new MeanStatistic());
    }

    /**
     * Guice <code>Module</code> for <code>PipelineConfiguration</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Module extends ObjectMapperModule {

        /**
         * Public constructor.
         */
        public Module() {
            super(Names.named("PipelineConfigurationObjectMapper"));

            final SimpleModule module = new SimpleModule("PipelineConfiguration");
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

            registerModule(module);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void configure(final Binder binder) {
            binder.bind(PipelineConfigurationFactory.class).to(DefaultPipelineConfigurationFactory.class);

            super.configure(binder);
        }

        private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();
    }

    /**
     * Factory interface for <code>PipelineConfiguration</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public interface PipelineConfigurationFactory {

        /**
         * Create a new <code>PipelineConfiguration</code> instance.
         * 
         * @param uri The <code>URI</code> to load configuration from.
         * @return New <code>PipelineConfiguration</code> instance.
         * @throws ConfigurationException if configuration load/parse fails.
         */
        PipelineConfiguration create(URI uri) throws ConfigurationException;
    }

    private static final class DefaultPipelineConfigurationFactory implements PipelineConfigurationFactory {

        @Inject
        public DefaultPipelineConfigurationFactory(@Named("PipelineConfigurationObjectMapper") final ObjectMapper objectMapper) {
            _objectMapper = objectMapper;
        }

        @Override
        public PipelineConfiguration create(final URI uri) throws ConfigurationException {
            try {
                final URL url = uri.toURL();
                try {
                    return _objectMapper.readValue(url, PipelineConfiguration.class);
                } catch (final IOException | ConstraintsViolatedException e) {
                    throw new ConfigurationException(String.format("Unable to parse configuration; uri=%s", uri), e);
                }

            } catch (final MalformedURLException e) {
                throw new ConfigurationException(String.format("Unable to parse configuration; uri=%s", uri), e);
            }
        }

        private final ObjectMapper _objectMapper;
    }
}
