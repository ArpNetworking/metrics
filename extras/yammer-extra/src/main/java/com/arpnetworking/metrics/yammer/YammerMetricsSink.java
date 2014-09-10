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
package com.arpnetworking.metrics.yammer;

import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Unit;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of <code>Sink</code> that records metrics to Yammer's 
 * <code>MetricsRegistry</code>. Yammer does not capture all the samples for
 * a counters or gauges; thus when converting from TSD counter and gauge samples
 * they are collapsed into a single measurement. For counters this is the sum of
 * the samples; as if a single count were taken during the unit of work. For
 * gauges this is the last sample taken. Timers record all their samples to
 * Yammer.
 * 
 * WARNING: Yammer has a single namespace of metrics while TSD has separate
 * namespaces for timers, counters and gauges. Therefore, you must ensure that
 * all your metric names are unique. 
 *
 * @author Brandon Arp (barp at groupon dot com)
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class YammerMetricsSink implements Sink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void record(
            final Map<String, String> annotations,
            final Map<String, List<Quantity>> timerSamples,
            final Map<String, List<Quantity>> counterSamples,
            final Map<String, List<Quantity>> gaugeSamples) {

        // Publish timers
        for (final Map.Entry<String, List<Quantity>> entry : timerSamples.entrySet()) {
            final Timer timer = _metricsRegistry.newTimer(getClass(), entry.getKey());
            for (final Quantity sample : entry.getValue()) {
                final TimeUnit timeUnit = sample.getUnit() == null ? TimeUnit.MILLISECONDS : Unit.toTimeUnit(sample.getUnit());
                timer.update(
                        sample.getValue().longValue(),
                        timeUnit);
            }
        }

        // Publish counters
        for (final Map.Entry<String, List<Quantity>> entry : counterSamples.entrySet()) {
            final Counter counter = _metricsRegistry.newCounter(YammerMetricsSink.class, entry.getKey());
            for (final Quantity sample : entry.getValue()) {
                counter.inc(sample.getValue().longValue());
            }
        }

        // Publish gauges
        for (final Map.Entry<String, List<Quantity>> entry : gaugeSamples.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final double value = entry.getValue().get(entry.getValue().size() - 1).getValue().doubleValue();
                final Gauge<Double> newGauge = new TsdGauge(value);

                final Gauge<Double> existingGauge = _metricsRegistry.newGauge(
                        YammerMetricsSink.class,
                        entry.getKey(),
                        newGauge);

                if (existingGauge != newGauge) {
                    if (existingGauge instanceof TsdGauge) {
                        final TsdGauge existingTsdGauge = (TsdGauge) existingGauge;
                        existingTsdGauge.setValue(value);
                    } else {
                        LOGGER.warn(String.format("Skipping non-tsd gauge for metric: %s", entry.getKey()));
                    }
                } else {
                    LOGGER.debug(String.format("Gauge created for metric: %s", entry.getKey()));
                }
            }
        }
    }

    /* package private */ MetricsRegistry getMetricsRegistry() {
        return _metricsRegistry;
    }

    /**
     * Protected constructor.
     * 
     * @param builder Instance of <code>Builder</code>.
     */
    protected YammerMetricsSink(final Builder builder) {
        _metricsRegistry = builder._metricsRegistry;
    }

    private final MetricsRegistry _metricsRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(YammerMetricsSink.class);

    private static final class TsdGauge extends Gauge<Double> {

        public TsdGauge(final double value) {
            _value = value;
        }

        public void setValue(final double value) {
            _value = value;
        }

        @Override
        public Double value() {
            return Double.valueOf(_value);
        }

        private double _value;
    }

    /**
     * Builder for <code>YammerMetricsSink</code>.
     * 
     * This class is thread safe.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder {

        /**
         * Create an instance of <code>Sink</code>.
         * 
         * @return Instance of <code>Sink</code>.
         */
        public YammerMetricsSink build() {
            if (_metricsRegistry == null) {
                _metricsRegistry = Metrics.defaultRegistry();
            }
            return new YammerMetricsSink(this);
        }

        /**
         * Set the Yammer <code>MetricsRegistry</code>. Optional. Default value
         * is to use the <code>defaultMetricsRegistry</code> from
         * <code>Metrics</code>.
         * 
         * @param value The value for the Yammer <code>MetricsRegistry</code>.
         * @return This <code>Builder</code> instance.
         */
        public Builder setMetricsRegistry(final MetricsRegistry value) {
            _metricsRegistry = value;
            return this;
        }

        private MetricsRegistry _metricsRegistry;
    }
}
