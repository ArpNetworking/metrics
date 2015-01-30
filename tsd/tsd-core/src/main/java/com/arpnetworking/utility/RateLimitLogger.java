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

package com.arpnetworking.utility;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.base.Optional;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.ocpsoft.prettytime.PrettyTime;

/**
 * Rate limits log events.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class RateLimitLogger {
    private RateLimitLogger(final Builder builder) {
        _message = builder._message;
        _level = builder._level;
        _loggingAdapter = builder._loggingAdapter;
        _period = builder._period;
    }

    /**
     * Triggers logging of the message.
     */
    public void log() {
        if (_lastWrite.isPresent()) {
            final DateTime last = _lastWrite.get();
            if (last.plus(_period).isBeforeNow()) {
                if (_suppressed == 0) {
                    _loggingAdapter.log(_level.asInt(), _message);
                } else {
                    _loggingAdapter.log(
                            _level.asInt(),
                            String.format(
                                    "%s (suppressed=%d, lastLogged=%s)",
                                    _message,
                                    _suppressed,
                                    new PrettyTime().format(_lastWrite.get().toDate())));
                }
                _lastWrite = Optional.of(DateTime.now());
            } else {
                _suppressed++;
            }
        } else {
            _loggingAdapter.log(_level.asInt(), _message);
            _lastWrite = Optional.of(DateTime.now());
        }


    }

    private Optional<DateTime> _lastWrite = Optional.absent();
    private int _suppressed = 0;

    private final LoggingAdapter _loggingAdapter;
    private final Logging.LogLevel _level;
    private final String _message;
    private final Period _period;


    /**
     * Implementation of the builder pattern for {@link com.arpnetworking.utility.RateLimitLogger}.
     */
    public static class Builder extends OvalBuilder<RateLimitLogger> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(RateLimitLogger.class);
        }

        /**
         * Sets the log level of the message.
         *
         * @param value The log level
         * @return This builder
         */
        public Builder setLevel(final Logging.LogLevel value) {
            _level = value;
            return this;
        }

        /**
         * Sets the log message.
         *
         * @param value The log message
         * @return This builder
         */
        public Builder setMessage(final String value) {
            _message = value;
            return this;
        }

        /**
         * Sets the period between log messages.
         *
         * @param value The log message
         * @return This builder
         */
        public Builder setPeriod(final Period value) {
            _period = value;
            return this;
        }

        /**
         * Sets the logging adapter to write to.
         *
         * @param value The logging adapter
         * @return This builder
         */
        public Builder setLogAdapter(final LoggingAdapter value) {
            _loggingAdapter = value;
            return this;
        }

        @NotNull
        private Period _period;
        @NotNull
        @NotEmpty
        private String _message;
        @NotNull
        private Logging.LogLevel _level;
        @NotNull
        private LoggingAdapter _loggingAdapter;
    }
}
