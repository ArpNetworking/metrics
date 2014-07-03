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
package com.arpnetworking.tsdcore.limiter;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limit the number of aggregations that will be emitted.
 * <p/>
 * addToAggregations() should be called when a new TSData is added to the TSAggregations
 * mark() should be called each time a datum is added to a TSData.
 * <p/>
 * There is a lot of machinery:
 * <ul>
 * <li>The _marks map keeps track of the last time a metric was emitted and the number of aggregations for that
 * metric.</li>
 * <li>_nAggregations is the sum of the aggregation counts in the _marks map</li>
 * <li>Since _marks and _nAggregations have to updated together, operations on them live in a critical section
 * controlled by _updateMarksMutex</li>
 * <li>The _marks list is persisted across program runs</li>
 * <li>The _marks list written to disk when a new metric is seen, and periodically (so that the timestamps stay
 * relatively fresh)</li>
 * <li>Metrics can age out of the _marks list</li>
 * </ul>
 *
 * There are two entry points for the limiter and both are called from line processor.
 * <code>addToAggregions()</code> is called when a new <code>TSData</code> is added to the
 * <code>_aggregations</code> map and <code>mark()</code> is called when a data point is added
 * to a <code>TSData</code> to record the last time a metric was written. The code was structured
 * this way way because it is expected that the MetricsLimiter will only be used for a short time.
 *
 * A better solution if the <code>MetricsLimiter</code> is to be permanent is to merge the limiter
 * and the <code>_aggregations</code> into a single object so the updating of the metrics limit list
 * and the last-written times is hidden inside the <code>_aggregations</code> object.
 * 
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
public final class DefaultMetricsLimiter implements MetricsLimiter {

    /**
     * Create a new <code>Builder</code> for this class.
     * 
     * @return New <code>Builder</code> for this class.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@inheritDoc}
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_EXIT")
    @Override
    public boolean offer(final AggregatedData data, final DateTime time) {
        final String key = data.getService() + "-" + data.getMetric() + "-" + data.getPeriod() + "-" + data.getStatistic().getName();
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
        if (lastLogged == null || now - lastLogged.longValue() >= _loggingInterval.getMillis()) {
            LOGGER.error(String.format(
                    "Limited aggregate %s; already aggregating %d",
                    key,
                    Integer.valueOf(_nAggregations)));
            _lastLogged.put(key, Long.valueOf(now));
        }

        return false;
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
                LOGGER.info(String.format("Aggregation %s aged out", key));
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

    private DefaultMetricsLimiter(final Builder builder) {
        this._maxAggregations = builder.getMaxAggregations();
        this._loggingInterval = builder.getLoggingInterval();
        this._ageOutThreshold = builder.getAgeOutThreshold();

        this._stateManager = builder.getStateManagerBuilder().build(_marks);

        // Load the persisted limit state & enable periodic state writes
        updateMarksAndAggregationCount(_stateManager.readState(), System.currentTimeMillis());
        if (builder.isEnableStateAutoWriter()) {
            _stateManager.startAutoWriter();
        }
    }

    private final long _maxAggregations;
    private final Duration _loggingInterval;
    private final Duration _ageOutThreshold;

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

    private static final long DEFAULT_MAX_AGGREGATIONS = 1000;
    private static final Duration DEFAULT_LOGGING_INTERVAL = Duration.standardMinutes(5);
    private static final Duration DEFAULT_AGE_OUT_THRESHOLD = Duration.standardDays(7);

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetricsLimiter.class);

    /**
     * Hold the number of aggregations and the last time a metric produced a 
     * data point.
     */
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
            return Objects.toStringHelper(Mark.class)
                    .add("Count", _count)
                    .add("Timer", _time)
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
    public static class Builder {
        private long _maxAggregations = DEFAULT_MAX_AGGREGATIONS;
        private Duration _loggingInterval = DEFAULT_LOGGING_INTERVAL;
        private Duration _ageOutThreshold = DEFAULT_AGE_OUT_THRESHOLD;
        private MetricsLimiterStateManager.Builder _stateManagerBuilder;
        private boolean _enableStateAutoWriter = true;

        public long getMaxAggregations() {
            return _maxAggregations;
        }

        /**
         * Set the max aggregations.
         * 
         * @param maxAggregations The max aggregations.
         * @return This instance of <code>Builder</code>.
         */
        public Builder withMaxAggregations(final long maxAggregations) {
            this._maxAggregations = maxAggregations;
            return this;
        }

        public Duration getLoggingInterval() {
            return _loggingInterval;
        }

        /**
         * Set the logging interval.
         * 
         * @param loggingInterval The logging interval.
         * @return This instance of <code>Builder</code>.
         */
        public Builder withLoggingInterval(final Duration loggingInterval) {
            this._loggingInterval = loggingInterval;
            return this;
        }

        public Duration getAgeOutThreshold() {
            return _ageOutThreshold;
        }

        /**
         * Set the age out threshold.
         * 
         * @param ageOutThreshold The age out threshold.
         * @return This instance of <code>Builder</code>.
         */
        public Builder withAgeOutThreshold(final Duration ageOutThreshold) {
            this._ageOutThreshold = ageOutThreshold;
            return this;
        }

        public MetricsLimiterStateManager.Builder getStateManagerBuilder() {
            return _stateManagerBuilder;
        }

        /**
         * Set the <code>MetricsLimiterStateManager</code> instance.
         * 
         * @param stateManagerBuilder The <code>MetricsLimiterStateManager</code>
         * instance.
         * @return This instance of <code>Builder</code>.
         */
        public Builder withStateManagerBuilder(final MetricsLimiterStateManager.Builder stateManagerBuilder) {
            this._stateManagerBuilder = stateManagerBuilder;
            return this;
        }

        public boolean isEnableStateAutoWriter() {
            return _enableStateAutoWriter;
        }

        /**
         * Set whether the state auto writer is enabled.
         * 
         * @param enableStateAutoWriter Whether the state auto writer is enabled.
         * @return This instance of <code>Builder</code>.
         */
        public Builder withEnableStateAutoWriter(final boolean enableStateAutoWriter) {
            this._enableStateAutoWriter = enableStateAutoWriter;
            return this;
        }

        /**
         * Create an instance of <code>DefaultMetricsLimiter</code>.
         * 
         * @return An instance of <code>DefaultMetricsLimiter</code>.
         */
        public DefaultMetricsLimiter build() {
            return new DefaultMetricsLimiter(this);
        }
    }

}
