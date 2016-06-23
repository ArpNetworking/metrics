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
package com.arpnetworking.tsdcore.limiter.legacy;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.logback.annotations.Loggable;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limit the number of aggregations that will be emitted.
 * <p>
 * <ul>
 * <li><code>addToAggregations()</code> should be called when a new TSData is added
 * to the TSAggregations.</li>
 * <li><code>mark()</code> should be called each time a datum is added to a TSData.</li>
 * </ul>
 * There is a lot of machinery:
 * <ul>
 * <li>The _marks map keeps track of the last time a metric was emitted and the
 * number of aggregations for that metric.</li>
 * <li>_nAggregations is the sum of the aggregation counts in the _marks map</li>
 * <li>Since _marks and _nAggregations have to updated together, operations on
 * them live in a critical section controlled by _updateMarksMutex</li>
 * <li>The _marks list is persisted across program runs</li>
 * <li>The _marks list written to disk when a new metric is seen, and periodically
 * (so that the timestamps stay relatively fresh)</li>
 * <li>Metrics can age out of the _marks list</li>
 * </ul>
 *
 * There are two entry points for the limiter and both are called from line
 * processor. <code>addToAggregions()</code> is called when a new
 * <code>TSData</code> is added to the <code>_aggregations</code> map and
 * <code>mark()</code> is called when a data point is added to a
 * <code>TSData</code> to record the last time a metric was written. The code
 * was structured this way way because it is expected that the MetricsLimiter
 * will only be used for a short time.
 *
 * A better solution if the <code>MetricsLimiter</code> is to be permanent is
 * to merge the limiter and the <code>_aggregations</code> into a single object
 * so the updating of the metrics limit list and the last-written times is
 * hidden inside the <code>_aggregations</code> object.
 *
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
public final class LegacyMetricsLimiter implements Closeable {

    /**
     * Consider whether a <code>AggregatedData</code> instance should be
     * processed further given predefined limits on the number of unique
     * instances.
     *
     * @param data <code>AggregatedData</code> instance to consider.
     * @param time The current date and time.
     * @return True if and only if the data was accepted.
     */
    @SuppressFBWarnings(value = "DM_EXIT")
    public boolean offer(final AggregatedData data, final DateTime time) {
        final String key = data.getFQDSN().getCluster() + "-"
                + data.getFQDSN().getService() + "-"
                + data.getFQDSN().getMetric() + "-"
                + data.getFQDSN().getStatistic().getName() + "-"
                + data.getPeriod();

        final long now = time.getMillis();

        final int nNewAggregations = 1;

        // We're updating the marks list and it's size together, so need a mutex section
        synchronized (_updateMarksMutex) {
            // If the metric is in the marks, list, it is already incorporated in _nAggregations so just add it
            // to the aggregations
            if (_marks.containsKey(key)) {
                _marks.get(key)._time = now;
                return true;
            }

            // Age out metrics to free up some slots if we're going exceed the maximum
            if (_nAggregations + nNewAggregations > _maxAggregations) {
                ageOutAggregations(now);
            }

            // If we now have enough room, create a marks entry, update _nAggregations and add the aggregations
            if (_nAggregations + nNewAggregations <= _maxAggregations) {
                _marks.putIfAbsent(key, new Mark(nNewAggregations, now));
                _nAggregations += nNewAggregations;
                _stateManager.requestWrite();
                return true;
            }
        }

        // If we get here, there was no room for the new aggregations, log it (but not too often) and then ignore
        final Long lastLogged = _lastLogged.get(key);
        if (lastLogged == null || now - lastLogged >= _loggingInterval.getMillis()) {
            LOGGER.warn()
                    .setMessage("Limited aggregate")
                    .addData("key", key)
                    .addData("count", _nAggregations)
                    .log();
            _lastLogged.put(key, now);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (_enableStateAutoWriter) {
            _stateManager.stopAutoWriter(true);
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("maxAggregations", _maxAggregations)
                .put("numberAggregations", _nAggregations)
                .put("stateManager", _stateManager)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    // Stuff below here has package scope for test access

    /**
     * Remove aggregations that are older than _ageOutThresholdMs if nNewAggregations would exceed _maxAggregations.
     * Returns true if aggregations were removed (and therefore the list of aggregations should be flushed to disk).
     * <p/>
     * Note: since _nAggregations and aggregations are being updated together, this needs to run within a critical
     * section guarded by _updateMetricsListMutex.
     */
    boolean ageOutAggregations(final long nowMs) {
        boolean removedSome = false;

        // Loop through removing as many as we can
        final Iterator<Map.Entry<String, Mark>> entries = _marks.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<String, Mark> aggregationMarkTime = entries.next();
            if (nowMs - aggregationMarkTime.getValue()._time >= _ageOutThreshold.getMillis()) {
                final String key = aggregationMarkTime.getKey();
                entries.remove();
                _nAggregations -= aggregationMarkTime.getValue()._count;
                LOGGER.info()
                        .setMessage("Aggregation aged out")
                        .addData("key", key)
                        .log();
                removedSome = true;
            }
        }

        return removedSome;
    }

    MetricsLimiterStateManager getStateManager() {
        return _stateManager;
    }

    void updateMarksAndAggregationCount(final Map<String, Mark> marks, final long nowMs) {
        synchronized (_updateMarksMutex) {
            for (final Map.Entry<String, Mark> metricMark : marks.entrySet()) {
                // If the mark is too old, ignore it
                if (metricMark.getValue()._time < nowMs - _ageOutThreshold.getMillis()) {
                    continue;
                }

                final Mark oldMark = _marks.putIfAbsent(metricMark.getKey(), metricMark.getValue());
                if (oldMark == null) {
                    // We just added a mark for a metric not previously seen in this session
                    _nAggregations += metricMark.getValue()._count;
                    continue;
                }
                // There is already a mark for the metric, so make a new mark with the max of the historical count
                // and the most recent timestamp
                final Mark newMark = new Mark(
                        Math.max(oldMark._count, metricMark.getValue()._count),
                        Math.max(oldMark._time, metricMark.getValue()._time)
                        );
                _marks.put(metricMark.getKey(), newMark);
            }
        }
    }

    Map<String, Mark> getMarks() {
        return Collections.unmodifiableMap(_marks);
    }

    int getNAggregations() {
        return _nAggregations;
    }

    private LegacyMetricsLimiter(final Builder builder) {
        _maxAggregations = builder._maxAggregations;
        _loggingInterval = builder._loggingInterval;
        _ageOutThreshold = builder._ageOutThreshold;
        _stateManager = builder._stateManagerBuilder.build(_marks);

        // Load the persisted limit state & enable periodic state writes
        updateMarksAndAggregationCount(_stateManager.readState(), System.currentTimeMillis());
        _enableStateAutoWriter = builder._enableStateAutoWriter;
        if (_enableStateAutoWriter) {
            _stateManager.startAutoWriter();
        }
    }

    private final long _maxAggregations;
    private final Duration _loggingInterval;
    private final Duration _ageOutThreshold;
    private final boolean _enableStateAutoWriter;

    // Used to synchronize updates to _nAggregations & aggregations (from the LineProcessor)
    private final Object _updateMarksMutex = new Object();
    // Records the last time a metric had tsd data recorded, typically includes data from prior runs of tsdAgg
    private final ConcurrentHashMap<String, Mark> _marks = new ConcurrentHashMap<>();
    // Keeps track of the last time a message was logged saying that a particular metric was being
    // dropped because too many metrics are in use.
    private final Map<String, Long> _lastLogged = Maps.newHashMap();
    private final MetricsLimiterStateManager _stateManager;
    // The current number of aggregations that have ad tsd data recorded
    private int _nAggregations;

    private static final Duration DEFAULT_LOGGING_INTERVAL = Duration.standardMinutes(5);
    private static final Duration DEFAULT_AGE_OUT_THRESHOLD = Duration.standardDays(7);

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyMetricsLimiter.class);

    /**
     * Hold the number of aggregations and the last time a metric produced a
     * data point.
     */
    @Loggable
    public static class Mark {

        /**
         * Public constructor.
         *
         * @param count The count.
         * @param time The timestamp in milliseconds since epoch.
         */
        public Mark(final long count, final long time) {
            this._count = count;
            this._time = time;
        }

        public long getCount() {
            return _count;
        }

        public long getTime() {
            return _time;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", Integer.toHexString(System.identityHashCode(this)))
                    .add("class", this.getClass())
                    .add("count", getCount())
                    .add("time", getTime())
                    .toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Mark mark = (Mark) o;

            return _count == mark._count
                    && _time == mark._time;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result = (int) (_time ^ (_time >>> 32));
            result = 31 * result + ((int) _count);
            return result;
        }

        private final long _count;
        private long _time;
    }

    /**
     * Builder for a MetricsLimiter.
     */
    public static final class Builder extends OvalBuilder<LegacyMetricsLimiter> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(LegacyMetricsLimiter.class);
        }

        /**
         * Set the max aggregations.
         *
         * @param maxAggregations The max aggregations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaxAggregations(final Long maxAggregations) {
            _maxAggregations = maxAggregations;
            return this;
        }

        /**
         * Set the logging interval.
         *
         * @param loggingInterval The logging interval. Optional. The default
         * is five minutes.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setLoggingInterval(final Duration loggingInterval) {
            _loggingInterval = loggingInterval;
            return this;
        }

        /**
         * Set the age out threshold. Optional. The default is seven days.
         *
         * @param ageOutThreshold The age out threshold.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setAgeOutThreshold(final Duration ageOutThreshold) {
            _ageOutThreshold = ageOutThreshold;
            return this;
        }

        /**
         * Set the <code>MetricsLimiterStateManager</code> instance.
         *
         * @param stateManagerBuilder The <code>MetricsLimiterStateManager</code>
         * instance.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setStateManagerBuilder(final MetricsLimiterStateManager.Builder stateManagerBuilder) {
            _stateManagerBuilder = stateManagerBuilder;
            return this;
        }

        /**
         * Set whether the state auto writer is enabled. Optional. The default
         * is true.
         *
         * @param enableStateAutoWriter Whether the state auto writer is enabled.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setEnableStateAutoWriter(final Boolean enableStateAutoWriter) {
            _enableStateAutoWriter = enableStateAutoWriter;
            return this;
        }

        @NotNull
        @Min(0)
        private Long _maxAggregations;
        @NotNull
        private MetricsLimiterStateManager.Builder _stateManagerBuilder;
        @NotNull
        private Duration _loggingInterval = DEFAULT_LOGGING_INTERVAL;
        @NotNull
        private Duration _ageOutThreshold = DEFAULT_AGE_OUT_THRESHOLD;
        @NotNull
        private Boolean _enableStateAutoWriter = Boolean.TRUE;
    }
}
