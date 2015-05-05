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
package com.arpnetworking.tsdaggregator;

import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdaggregator.model.Record;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.utility.Launchable;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.observer.Observable;
import com.arpnetworking.utility.observer.Observer;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Performs aggregation of <code>Record</code> instances per <code>Period</code>.
 * This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class Aggregator implements Observer, Launchable {

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void launch() {
        LOGGER.debug()
                .setMessage("Launching aggregator")
                .addData("aggregator", this)
                .log();

        _periodClosers.clear();
        if (!_periods.isEmpty()) {
            _periodCloserExecutor = Executors.newFixedThreadPool(_periods.size());
            // TODO(vkoskela): Convert to scheduled thread executor [MAI-468]
            for (final Period period : _periods) {
                final PeriodCloser periodCloser = new PeriodCloser.Builder()
                        .setPeriod(period)
                        .setBucketBuilder(
                                new Bucket.Builder()
                                        .setCounterStatistics(_counterStatistics)
                                        .setGaugeStatistics(_gaugeStatistics)
                                        .setTimerStatistics(_timerStatistics)
                                        .setPeriod(period)
                                        .setCluster(_cluster)
                                        .setHost(_host)
                                        .setService(_service)
                                        .setSink(_sink))
                        .build();
                _periodClosers.add(periodCloser);
                _periodCloserExecutor.submit(periodCloser);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        LOGGER.debug()
                .setMessage("Stopping aggregator")
                .addData("aggregator", this)
                .log();

        for (final PeriodCloser periodCloser : _periodClosers) {
            periodCloser.shutdown();
        }
        _periodClosers.clear();
        if (_periodCloserExecutor != null) {
            _periodCloserExecutor.shutdown();
            try {
                _periodCloserExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                LOGGER.warn("Unable to shutdown period closer executor", e);
            }
            _periodCloserExecutor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notify(final Observable observable, final Object event) {
        if (!(event instanceof Record)) {
            LOGGER.error()
                    .setMessage("Observed unsupported event")
                    .addData("event", event)
                    .log();
            return;
        }
        final Record record = (Record) event;
        LOGGER.trace()
                .setMessage("Processing record")
                .addData("record", record)
                .log();
        for (final PeriodCloser periodCloser : _periodClosers) {
            periodCloser.record(record);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Service", _service)
                .add("Cluster", _cluster)
                .add("Host", _host)
                .add("Sink", _sink)
                .add("TimerStatistics", _timerStatistics)
                .add("CounterStatistics", _counterStatistics)
                .add("GaugeStatistics", _gaugeStatistics)
                .add("PeriodClosers", _periodClosers)
                .toString();

    }

    private Aggregator(final Builder builder) {
        _periods = ImmutableSet.copyOf(builder._periods);
        _service = builder._service;
        _cluster = builder._cluster;
        _host = builder._host;
        _sink = builder._sink;
        _counterStatistics = ImmutableSet.copyOf(builder._counterStatistics);
        _gaugeStatistics = ImmutableSet.copyOf(builder._gaugeStatistics);
        _timerStatistics = ImmutableSet.copyOf(builder._timerStatistics);
    }

    private final ImmutableSet<Period> _periods;
    private final String _service;
    private final String _cluster;
    private final String _host;
    private final Sink _sink;
    private final ImmutableSet<Statistic> _timerStatistics;
    private final ImmutableSet<Statistic> _counterStatistics;
    private final ImmutableSet<Statistic> _gaugeStatistics;
    private final ArrayList<PeriodCloser> _periodClosers = Lists.newArrayList();

    private ExecutorService _periodCloserExecutor = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);

    /**
     * <code>Builder</code> implementation for <code>Aggregator</code>.
     */
    public static final class Builder extends OvalBuilder<Aggregator> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(Aggregator.class);
        }

        /**
         * Set the service. Cannot be null or empty.
         *
         * @param value The service.
         * @return This <code>Builder</code> instance.
         */
        public Builder setService(final String value) {
            _service = value;
            return this;
        }

        /**
         * Set the cluster. Cannot be null or empty.
         *
         * @param value The cluster.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return this;
        }

        /**
         * Set the host. Cannot be null or empty.
         *
         * @param value The host.
         * @return This <code>Builder</code> instance.
         */
        public Builder setHost(final String value) {
            _host = value;
            return this;
        }

        /**
         * Set the sink. Cannot be null or empty.
         *
         * @param value The sink.
         * @return This <code>Builder</code> instance.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * Set the periods. Cannot be null or empty.
         *
         * @param value The periods.
         * @return This <code>Builder</code> instance.
         */
        public Builder setPeriods(final Set<Period> value) {
            _periods = value;
            return this;
        }

        /**
         * Set the timer statistics. Cannot be null or empty.
         *
         * @param value The timer statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setTimerStatistics(final Set<Statistic> value) {
            _timerStatistics = value;
            return this;
        }

        /**
         * Set the counter statistics. Cannot be null or empty.
         *
         * @param value The counter statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCounterStatistics(final Set<Statistic> value) {
            _counterStatistics = value;
            return this;
        }

        /**
         * Set the gauge statistics. Cannot be null or empty.
         *
         * @param value The gauge statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setGaugeStatistics(final Set<Statistic> value) {
            _gaugeStatistics = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _service;
        @NotNull
        @NotEmpty
        private String _cluster;
        @NotNull
        @NotEmpty
        private String _host;
        @NotNull
        private Sink _sink;
        @NotNull
        private Set<Period> _periods;
        @NotNull
        private Set<Statistic> _timerStatistics;
        @NotNull
        private Set<Statistic> _counterStatistics;
        @NotNull
        private Set<Statistic> _gaugeStatistics;
    }
}
