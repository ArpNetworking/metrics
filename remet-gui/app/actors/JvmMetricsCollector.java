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
package actors;

import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.jvm.JvmMetricsRunnable;
import com.arpnetworking.play.configuration.ConfigurationHelper;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import play.Configuration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Actor responsible for collecting JVM metrics on a periodic basis.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class JvmMetricsCollector extends UntypedActor {

    /**
     * Public constructor.
     *
     * @param configuration Play app configuration.
     * @param metricsFactory An instance of <code>MetricsFactory</code>.
     */
    @Inject
    public JvmMetricsCollector(
            final Configuration configuration,
            final MetricsFactory metricsFactory) {
        _interval = ConfigurationHelper.getFiniteDuration(configuration, "metrics.jvm.interval");
        _jvmMetricsRunnable = new JvmMetricsRunnable.Builder()
                .setMetricsFactory(metricsFactory)
                .setSwallowException(false) // Relying on the default akka supervisor strategy here.
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preStart() {
        LOGGER.info()
                .setMessage("Starting JVM metrics collector actor.")
                .addData("actor", self())
                .log();
        _cancellable = getContext().system().scheduler().schedule(
                INITIAL_DELAY,
                _interval,
                self(),
                new CollectJvmMetrics(),
                getContext().dispatcher(),
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
                .addData("actor", self())
                .log();
        if (message instanceof CollectJvmMetrics) {
            _jvmMetricsRunnable.run();
        } else {
            unhandled(message);
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
                .put("interval", _interval)
                .put("jvmMetricsRunnable", _jvmMetricsRunnable)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private Cancellable _cancellable;

    private final FiniteDuration _interval;
    private final Runnable _jvmMetricsRunnable;

    private static final FiniteDuration INITIAL_DELAY = FiniteDuration.Zero();
    private static final Logger LOGGER = LoggerFactory.getLogger(JvmMetricsCollector.class);

    /**
     * Message class to collect JVM metrics.
     *
     * @author Deepika Misra (deepika at groupon dot com)
     */
    private static final class CollectJvmMetrics {

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", Integer.toHexString(System.identityHashCode(this)))
                    .add("class", this.getClass())
                    .toString();
        }

        private CollectJvmMetrics() { }
    }
}
