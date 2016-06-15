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

package com.arpnetworking.clusteraggregator.test;

import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.metrics.Unit;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A test helper to assist in branch coverage for close with resources uses of a {@link com.arpnetworking.metrics.Metrics}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ThrowingMetrics implements Metrics {
    /**
     * Public constructor.
     * @param throwOnRecord Throws errors on metrics recording
     * @param throwOnClose Throws errors on metrics close
     */
    public ThrowingMetrics(final boolean throwOnRecord, final boolean throwOnClose) {
        _throwOnRecord = throwOnRecord;
        _throwOnClose = throwOnClose;
    }

    /**
     * {@inheritDoc}
     */
    //CHECKSTYLE.OFF: IllegalThrows - We're gonna do a lot of runtime throwing in here
    @Override
    public Counter createCounter(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
        return Mockito.mock(Counter.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(final String name, final long value) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementCounter(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementCounter(final String name, final long value) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetCounter(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
        return Mockito.mock(Timer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTimer(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTimer(final String name) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimer(final String name, final long duration, final TimeUnit unit) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimer(final String name, final long duration, final Unit unit) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final double value) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final double value, final Unit unit) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final long value) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final long value, final Unit unit) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnnotation(final String s, final String s1) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnnotations(final Map<String, String> map) {
        if (_throwOnRecord) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instant getOpenTime() {
        return Clock.systemUTC().instant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instant getCloseTime() {
        return Clock.systemUTC().instant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (_throwOnClose) {
            throw new RuntimeException("boom");
        }
    }
    //CHECKSTYLE.ON: IllegalThrows

    private final boolean _throwOnRecord;
    private final boolean _throwOnClose;
}
