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
import com.arpnetworking.tsdaggregator.model.Metric;
import com.arpnetworking.tsdaggregator.model.Record;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.OrderedStatistic;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultiset;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Contains samples for a particular aggregation period in time.
*
* @author Ville Koskela (vkoskela at groupon dot com)
*/
/* package private */ class Bucket {

    /**
     * Close the bucket. The aggregates for each metric are emitted to the sink.
     */
    public void close() {
        if (_isOpen.getAndSet(false)) {
            final Collection<AggregatedData> data = Lists.newArrayList();
            computeStatistics(_counterMetricSamples, _counterStatistics, data);
            computeStatistics(_gaugeMetricSamples, _gaugeStatistics, data);
            computeStatistics(_timerMetricSamples, _timerStatistics, data);
            _sink.recordAggregateData(data, Collections.<Condition>emptyList());
        } else {
            LOGGER.warn()
                    .setMessage("Bucket closed multiple times")
                    .addData("bucket", this)
                    .log();
        }
    }

    /**
     * Add data in the form of a <code>Record</code> to this <code>Bucket</code>.
     *
     * @param record The data to add to this <code>Bucket</code>.
     */
    public void add(final Record record) {
        for (final Map.Entry<String, ? extends Metric> entry : record.getMetrics().entrySet()) {
            final String name = entry.getKey();
            final Metric metric = entry.getValue();

            if (metric.getValues().isEmpty()) {
                LOGGER.debug()
                        .setMessage("Discarding metric")
                        .addData("reason", "no samples")
                        .addData("name", name)
                        .addData("metric", metric)
                        .log();
                continue;
            }

            switch (metric.getType()) {
                case COUNTER: {
                    final boolean sorted = _counterStatistics.stream().anyMatch((s) -> s instanceof OrderedStatistic);
                    addMetric(name, metric, record.getTime(), _counterMetricSamples, sorted);
                    break;
                }
                case GAUGE: {
                    final boolean sorted = _gaugeStatistics.stream().anyMatch((s) -> s instanceof OrderedStatistic);
                    addMetric(name, metric, record.getTime(), _gaugeMetricSamples, sorted);
                    break;
                }
                case TIMER: {
                    final boolean sorted = _timerStatistics.stream().anyMatch((s) -> s instanceof OrderedStatistic);
                    addMetric(name, metric, record.getTime(), _timerMetricSamples, sorted);
                    break;
                }
                default:
                    LOGGER.warn()
                            .setMessage("Discarding metric")
                            .addData("reason", "unsupported type")
                            .addData("name", name)
                            .addData("metric", metric)
                            .log();
            }
        }
    }

    public DateTime getStart() {
        return _start;
    }

    public boolean isOpen() {
        return _isOpen.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("IsOpen", _isOpen)
                .add("Sink", _sink)
                .add("Cluster", _cluster)
                .add("Start", _service)
                .add("Host", _host)
                .add("Start", _start)
                .add("Period", _period)
                .add("CounterStatistics", _counterStatistics)
                .add("GaugeStatistics", _gaugeStatistics)
                .add("TimerStatistics", _timerStatistics)
                .toString();
    }

    private void computeStatistics(
            final Map<String, Collection<Quantity>> metricSamples,
            final Set<Statistic> statistics,
            final Collection<AggregatedData> data) {

        final FQDSN.Builder fqdsnBuilder = new FQDSN.Builder()
                .setCluster(_cluster)
                .setService(_service);
        final AggregatedData.Builder datumBuilder = new AggregatedData.Builder()
                .setStart(_start)
                .setPeriod(_period)
                .setHost(_host);

        for (final Map.Entry<String, Collection<Quantity>> entry : metricSamples.entrySet()) {
            final String metric = entry.getKey();
            final Collection<Quantity> samples = entry.getValue();
            fqdsnBuilder.setMetric(metric);

            // Unify sample units
            // TODO(vkoskela): Extend Quantity to support unit-aware arithmetic. [MAI-?]
            final List<Quantity> computableSamples = Quantity.unify(samples);

            // Compute statistics
            for (final Statistic statistic : statistics) {
                datumBuilder.setFQDSN(
                        fqdsnBuilder.setStatistic(statistic)
                                .build());

                data.add(datumBuilder.setValue(statistic.calculate(computableSamples))
                        .setPopulationSize((long) samples.size())
                        .setSamples(samples)
                        .build());
            }
        }
    }

    private void addMetric(
            final String name,
            final Metric metric,
            final DateTime time,
            final Map<String, Collection<Quantity>> data,
            final boolean sorted) {

        Collection<Quantity> samples = data.get(name);
        if (samples == null) {
            final Collection<Quantity> newSamples;
            if (sorted) {
                newSamples = TreeMultiset.create();
            } else {
                newSamples = new ArrayList<>();
            }
            samples = data.putIfAbsent(name, newSamples);
            if (samples == null) {
                samples = newSamples;
            }
        }
        synchronized (samples) {
            if (!_isOpen.get()) {
                LOGGER.warn()
                        .setMessage("Discarding metric")
                        .addData("reason", "added after close")
                        .addData("bucket", this)
                        .addData("name", name)
                        .addData("metric", metric)
                        .addData("time", time)
                        .log();
                return;
            }
            samples.addAll(metric.getValues());
        }
    }

    Bucket(final Builder builder) {
        _sink = builder._sink;
        _cluster = builder._cluster;
        _service = builder._service;
        _host = builder._host;
        _start = builder._start;
        _period = builder._period;
        _counterStatistics = builder._counterStatistics;
        _gaugeStatistics = builder._gaugeStatistics;
        _timerStatistics = builder._timerStatistics;
    }


    private final AtomicBoolean _isOpen = new AtomicBoolean(true);
    private final Sink _sink;
    private final String _cluster;
    private final String _service;
    private final String _host;
    private final DateTime _start;
    private final Period _period;
    private final Set<Statistic> _counterStatistics;
    private final Set<Statistic> _gaugeStatistics;
    private final Set<Statistic> _timerStatistics;
    private final Map<String, Collection<Quantity>> _counterMetricSamples = Maps.newConcurrentMap();
    private final Map<String, Collection<Quantity>> _gaugeMetricSamples = Maps.newConcurrentMap();
    private final Map<String, Collection<Quantity>> _timerMetricSamples = Maps.newConcurrentMap();

    private static final Logger LOGGER = LoggerFactory.getLogger(Bucket.class);

    /**
     * <code>Builder</code> implementation for <code>Bucket</code>.
     */
    public static final class Builder extends OvalBuilder<Bucket> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(Bucket.class);
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
         * Set the timer statistics. Cannot be null or empty.
         *
         * @param value The timer statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setTimerStatistics(final ImmutableSet<Statistic> value) {
            _timerStatistics = value;
            return this;
        }

        /**
         * Set the counter statistics. Cannot be null or empty.
         *
         * @param value The counter statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCounterStatistics(final ImmutableSet<Statistic> value) {
            _counterStatistics = value;
            return this;
        }

        /**
         * Set the gauge statistics. Cannot be null or empty.
         *
         * @param value The gauge statistics.
         * @return This <code>Builder</code> instance.
         */
        public Builder setGaugeStatistics(final ImmutableSet<Statistic> value) {
            _gaugeStatistics = value;
            return this;
        }

        /**
         * Set the start. Cannot be null or empty.
         *
         * @param value The start.
         * @return This <code>Builder</code> instance.
         */
        public Builder setStart(final DateTime value) {
            _start = value;
            return this;
        }

        /**
         * Set the period. Cannot be null or empty.
         *
         * @param value The period.
         * @return This <code>Builder</code> instance.
         */
        public Builder setPeriod(final Period value) {
            _period = value;
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
        private DateTime _start;
        @NotNull
        private Period _period;
        @NotNull
        private ImmutableSet<Statistic> _timerStatistics;
        @NotNull
        private ImmutableSet<Statistic> _counterStatistics;
        @NotNull
        private ImmutableSet<Statistic> _gaugeStatistics;
    }
}
