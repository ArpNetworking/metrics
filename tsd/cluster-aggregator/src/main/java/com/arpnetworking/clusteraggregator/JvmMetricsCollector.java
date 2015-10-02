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
package com.arpnetworking.clusteraggregator;

import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActor;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.jvm.JvmMetricsRunnable;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.MoreObjects;
import org.joda.time.Period;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Actor responsible for collecting JVM metrics on a periodic basis.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class JvmMetricsCollector extends UntypedActor {

    /**
     * Creates a <code>Props</code> for construction in Akka.
     *
     * @param interval An instance of <code>Period</code>.
     * @param metricsFactory A <code>MetricsFactory</code> to use for metrics creation.
     * @return A new <code>Props</code>.
     */
    public static Props props(
            final Period interval,
            final MetricsFactory metricsFactory) {
        return Props.create(JvmMetricsCollector.class, interval, metricsFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preStart() {
        LOGGER.info()
                .setMessage("Starting JVM metrics collector actor.")
                .addData("actor", self().toString())
                .log();
        _cancellable = _scheduler.schedule(
                INITIAL_DELAY,
                _interval,
                self(),
                new CollectJvmMetrics(),
                getContext().system().dispatcher(),
                self());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postStop() {
        LOGGER.info().setMessage("Stopping JVM metrics collection.").log();
        _cancellable.cancel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        LOGGER.trace().setMessage("Message received")
                .addData("data", message)
                .addData("actor", self().toString())
                .log();
        if (message instanceof CollectJvmMetrics) {
            _jvmMetricsRunnable.run();
        } else {
            unhandled(message);
        }
    }

    /* package private */ Cancellable getCancellable() {
        return _cancellable;
    }

    /**
     * Package private constructor. Strictly for testing.
     *
     * @param interval An instance of <code>FiniteDuration</code>.
     * @param runnable An instance of <code>Runnable</code>.
     */
    /* package private */ JvmMetricsCollector(
            final FiniteDuration interval,
            final Runnable runnable,
            final Scheduler scheduler) {
        _interval = interval;
        _jvmMetricsRunnable = runnable;
        if (scheduler == null) {
            _scheduler = getContext().system().scheduler();
        } else {
            _scheduler = scheduler;
        }
    }

    private JvmMetricsCollector(
            final Period interval,
            final MetricsFactory metricsFactory) {
        this(
                FiniteDuration.create(
                        interval.toStandardDuration().getMillis(),
                        TimeUnit.MILLISECONDS),
                new JvmMetricsRunnable.Builder()
                        .setMetricsFactory(metricsFactory)
                        .setSwallowException(false) // Relying on the default akka supervisor strategy here.
                        .build(),
                null
        );
    }
    private Cancellable _cancellable;

    private final FiniteDuration _interval;
    private final Runnable _jvmMetricsRunnable;
    private final Scheduler _scheduler;

    private static final FiniteDuration INITIAL_DELAY = FiniteDuration.Zero();
    private static final Logger LOGGER = LoggerFactory.getLogger(JvmMetricsCollector.class);

    /**
     * Message class to collect JVM metrics. Package private for testing.
     *
     * @author Deepika Misra (deepika at groupon dot com)
     */
    /* package private */ static final class CollectJvmMetrics {

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", Integer.toHexString(System.identityHashCode(this)))
                    .toString();
        }

        /* package private */ CollectJvmMetrics() { }
    }
}
