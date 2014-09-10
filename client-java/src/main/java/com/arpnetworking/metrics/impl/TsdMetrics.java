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
package com.arpnetworking.metrics.impl;

import com.arpnetworking.metrics.Counter;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.Quantity;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.metrics.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of <code>Metrics</code> that publishes metrics as
 * time series data (TSD). 
 * 
 * This class does not throw exceptions if it is used improperly or if the
 * underlying IO subsystem fails to write the metrics. An example of improper 
 * use would be if the user invokes stop on a timer without calling start. To 
 * prevent breaking the client application no exception is thrown; instead a 
 * warning is logged using the SLF4J <code>LoggerFactory</code> for this class.
 * 
 * Another example would be if the disk is full or fails to record the metrics 
 * when <code>close</code> is invoked the library will not throw an exception. 
 * However, it will attempt to write a warning using the SLF4J 
 * <code>LoggerFactory</code> for this class; although this is likely to fail
 * if the underlying hardware is experiencing problems.
 * 
 * If clients desire an intrusive failure propagation strategy or prefer to 
 * implement their own failure handling via callback we are open to implementing
 * such an alternative strategy. Please contact us with your feature request.
 * 
 * For more information about the semantics of this class and its methods please
 * refer to the <code>Metrics</code> interface documentation. To create an 
 * instance of this class use <code>TsdMetricsFactory</code>. 
 * 
 * This class is thread safe; however, it makes no effort to order
 * operations on the same data. For example, it is safe for two threads to
 * start and stop separate timers but if the threads start and stop the same
 * timer than it is up to the caller to ensure that start is called before
 * stop.
 * 
 * The library does attempt to detect incorrect usage, for example modifying
 * metrics after closing and starting but never stopping a timer; however, in
 * a multithreaded environment it is not guaranteed that these warnings are
 * emitted. It is up to clients to ensure that multithreaded use of the same
 * <code>TsdMetrics</code> instance is correct.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class TsdMetrics implements Metrics {

    /**
     * {@inheritDoc}
     */
    @Override
    public Counter createCounter(final String name) {
        return createCounterInternal(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(final String name) {
        incrementCounter(name, 1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementCounter(final String name, final long value) {
        if (!assertIsOpen()) {
            return;
        }

        final ConcurrentLinkedDeque<Quantity> counters = getOrCreateDeque(_counterSamples, name);
        TsdCounter counter = _counters.get(name);
        if (counter == null) {
            final TsdCounter newCounter = TsdCounter.newInstance(name, _isOpen);
            counter = getOrCreate(_counters, name, newCounter);
            if (counter == newCounter) {
                counters.add(counter);
            }
        }
        counter.increment(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementCounter(final String name) {
        incrementCounter(name, -1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementCounter(final String name, final long value) {
        incrementCounter(name, -1L * value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetCounter(final String name) {
        if (!assertIsOpen()) {
            return;
        }
        _counters.put(name, createCounterInternal(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final String name) {
        if (!assertIsOpen()) {
            // To prevent the calling code from throwing a NPE we just return a
            // timer object; note that the call to assertIsOpen has already
            // logged a warning about incorrect use of the class.
            return TsdTimer.newInstance(name, _isOpen);
        }
        final ConcurrentLinkedDeque<Quantity> samples = getOrCreateDeque(_timerSamples, name);
        final TsdTimer timer = TsdTimer.newInstance(name, _isOpen);
        samples.add(timer);
        return timer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTimer(final String name) {
        if (!assertIsOpen()) {
            return;
        }
        // In this case the normal behavior is to insert; only if the library is
        // being incorrectly used will there be a running timer with the same
        // name.
        final TsdTimer timer = TsdTimer.newInstance(name, _isOpen);
        if (_timers.putIfAbsent(name, timer) != null) {
            // This is in place of an exception; see class Javadoc
            _logger.warn(String.format("Cannot start timer because timer already started; timerName=%s", name));
            return;
        }
        final ConcurrentLinkedDeque<Quantity> samples = getOrCreateDeque(_timerSamples, name);
        samples.add(timer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTimer(final String name) {
        if (!assertIsOpen()) {
            return;
        }
        final Timer timer = _timers.remove(name);
        if (timer == null) {
            // This is in place of an exception; see class Javadoc
            _logger.warn(String.format("Cannot stop timer because timer was not started; timerName=%s", name));
            return;
        }
        timer.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimer(final String name, final long duration, final TimeUnit unit) {
        setTimer(name, duration, Unit.fromTimeUnit(unit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimer(final String name, final long duration, final Unit unit) {
        if (!assertIsOpen()) {
            return;
        }
        final ConcurrentLinkedDeque<Quantity> samples = getOrCreateDeque(_timerSamples, name);
        samples.add(TsdQuantity.newInstance(Long.valueOf(duration), unit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final double value) {
        setGauge(name, value, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final double value, final Unit unit) {
        if (!assertIsOpen()) {
            return;
        }
        final Deque<Quantity> list = getOrCreateDeque(_gaugeSamples, name);
        list.add(TsdQuantity.newInstance(Double.valueOf(value), unit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final long value) {
        setGauge(name, value, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGauge(final String name, final long value, final Unit unit) {
        if (!assertIsOpen()) {
            return;
        }
        final Deque<Quantity> list = getOrCreateDeque(_gaugeSamples, name);
        list.add(TsdQuantity.newInstance(Long.valueOf(value), unit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void annotate(final String key, final String value) {
        if (!assertIsOpen()) {
            return;
        }
        _annotations.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return _isOpen.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (!assertIsOpen(_isOpen.getAndSet(false))) {
            return;
        }
        _annotations.put(FINAL_TIMESTAMP_KEY, _dateTimeFormatIso8601.format(new Date()));

        final Map<String, String> annotations = Collections.unmodifiableMap(_annotations);
        final Map<String, List<Quantity>> timerSamples = cloneSamples(_timerSamples, PREDICATE_STOPPED_TIMERS);
        final Map<String, List<Quantity>> counterSamples = cloneSamples(_counterSamples, PREDICATE_ALL_QUANTITIES);
        final Map<String, List<Quantity>> gaugeSamples = cloneSamples(_gaugeSamples, PREDICATE_ALL_QUANTITIES);

        for (final Sink sink : _sinks) {
            sink.record(
                    annotations,
                    timerSamples,
                    counterSamples,
                    gaugeSamples);
        }
    }

    // NOTE: Package private for testing
    <T> T getOrCreate(final ConcurrentMap<String, T> map, final String key, final T newValue) {
        final T currentValue = map.putIfAbsent(key, newValue);
        if (currentValue != null) {
            return currentValue;
        }
        return newValue;
    }

    private TsdCounter createCounterInternal(final String name) {
        if (!assertIsOpen()) {
            // To prevent the calling code from throwing a NPE we just return a
            // counter object; note that the call to assertIsOpen has already
            // logged a warning about incorrect use of the class.
            return TsdCounter.newInstance(name, _isOpen);
        }

        // Create and register a new counter sample
        final ConcurrentLinkedDeque<Quantity> counters = getOrCreateDeque(_counterSamples, name);
        final TsdCounter newCounter = TsdCounter.newInstance(name, _isOpen);
        counters.add(newCounter);
        return newCounter;
    }

    private <T> ConcurrentLinkedDeque<T> getOrCreateDeque(final ConcurrentMap<String, ConcurrentLinkedDeque<T>> map, final String key) {
        ConcurrentLinkedDeque<T> deque = map.get(key);
        if (deque == null) {
            final ConcurrentLinkedDeque<T> newDeque = new ConcurrentLinkedDeque<>();
            deque = getOrCreate(map, key, newDeque);
        }
        return deque;
    }

    private boolean assertIsOpen() {
        return assertIsOpen(_isOpen.get());
    }

    private boolean assertIsOpen(final boolean isOpen) {
        if (!isOpen) {
            // This is in place of an exception; see class Javadoc
            _logger.warn("Metrics object was closed during an operation; you may have a race condition");
        }
        return isOpen;
    }

    private Map<String, List<Quantity>> cloneSamples(
            final Map<String, ? extends Collection<? extends Quantity>> samples,
            final Predicate<Quantity> predicate) {
        // CHECKSTYLE.OFF: IllegalInstantiation - No Guava
        final Map<String, List<Quantity>> clonedSamples = new HashMap<>((int) (samples.size() / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
        // CHECKSTYLE.ON: IllegalInstantiation
        for (final Map.Entry<String, ? extends Collection<? extends Quantity>> entry : samples.entrySet()) {
            final List<Quantity> quantities = new ArrayList<>(entry.getValue().size());
            clonedSamples.put(entry.getKey(), quantities);
            for (final Quantity quantity : entry.getValue()) {
                if (predicate.apply(quantity)) {
                    quantities.add(TsdQuantity.newInstance(quantity.getValue(), quantity.getUnit()));
                } else {
                    _logger.warn(String.format("Sample rejected; name=%s, sample=%s, predicate=%s", entry.getKey(), quantity, predicate));
                }
            }
        }
        return clonedSamples;
    }

    /**
     * Package private constructor. Instances of this class should be 
     * constructed with an appropriate implementation of 
     * <code>MetricsFactory</code>.
     * 
     * @param queryLogger
     */
    // NOTE: Package private for testing
    TsdMetrics(final List<Sink> sinks) {
        this(sinks, LOGGER);
    }

    // NOTE: Package private for testing
    TsdMetrics(final List<Sink> sinks, final Logger logger) {
        _sinks = sinks;
        _logger = logger;
        _dateTimeFormatIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
        _annotations.put(INITIAL_TIMESTAMP_KEY, _dateTimeFormatIso8601.format(new Date()));
    }

    private final List<Sink> _sinks;
    private final Logger _logger;
    private final AtomicBoolean _isOpen = new AtomicBoolean(true);
    private final ConcurrentMap<String, TsdCounter> _counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TsdTimer> _timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Quantity>> _counterSamples = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Quantity>> _timerSamples = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Quantity>> _gaugeSamples = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> _annotations = new ConcurrentHashMap<>();

    // NOTE: SimpleDateFormat is not thread safe thus it is non-static
    private final DateFormat _dateTimeFormatIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");

    private static final Predicate<Quantity> PREDICATE_ALL_QUANTITIES = new AllQuantitiesPredicate();
    private static final Predicate<Quantity> PREDICATE_STOPPED_TIMERS = new StoppedTimersPredicate();
    private static final Logger LOGGER = LoggerFactory.getLogger(TsdMetrics.class);
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final String FINAL_TIMESTAMP_KEY = "finalTimestamp";
    private static final String INITIAL_TIMESTAMP_KEY = "initTimestamp";

    private interface Predicate<T> {

        boolean apply(T item);
    }

    private static final class AllQuantitiesPredicate implements Predicate<Quantity> {

        @Override
        public boolean apply(final Quantity item) {
            return true;
        }
    }

    private static final class StoppedTimersPredicate implements Predicate<Quantity> {

        @Override
        public boolean apply(final Quantity item) {
            if (item instanceof Timer) {
                final Timer timer = (Timer) item;
                return timer.isStopped();
            }
            return true;
        }
    }
}
