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
package com.arpnetworking.steno;

import com.arpnetworking.logback.annotations.LogValue;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Limits actual log output to at most once per specified <code>Period</code>.
 * The implementation will add two data attributes <code>_skipped</code> and
 * <code>_lastLogTime</code> to the wrapped <code>LogBuilder</code> instance.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class RateLimitLogBuilder implements LogBuilder {

    /**
     * Public constructor.
     *
     * @param logBuilder Instance of <code>LogBuilder</code>.
     * @param period Minimum time between log message output.
     */
    public RateLimitLogBuilder(final LogBuilder logBuilder, final Period period) {
        _logBuilder = logBuilder;
        _period = period;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogBuilder setEvent(final String value) {
        _logBuilder.setEvent(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogBuilder setMessage(final String value) {
        _logBuilder.setMessage(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogBuilder setThrowable(final Throwable value) {
        _logBuilder.setThrowable(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogBuilder addData(final String name, final Object value) {
        _logBuilder.addData(name, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogBuilder addContext(final String name, final Object value) {
        _logBuilder.addContext(name, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log() {
        final DateTime now = DateTime.now();
        boolean shouldLog = true;
        if (_lastLogTime.isPresent()) {
            if (!_lastLogTime.get().plus(_period).isBeforeNow()) {
                shouldLog = false;
                ++_skipped;
            }
        }
        if (shouldLog) {
            _logBuilder
                    .addData("_skipped", _skipped)
                    .addData("_lastLogTime", _lastLogTime)
                    .log();
            _lastLogTime = Optional.of(now);
            _skipped = 0;
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.<String, Object>builder()
                .put("id", Integer.toHexString(System.identityHashCode(this)))
                .put("class", this.getClass())
                .put("LogBuilder", LogReferenceOnly.of(_logBuilder))
                .put("Period", _period)
                .put("LastLogTime", _lastLogTime)
                .put("Skipped", _skipped)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private LogBuilder _logBuilder;
    private Period _period;
    private Optional<DateTime> _lastLogTime = Optional.absent();
    private int _skipped = 0;
}
