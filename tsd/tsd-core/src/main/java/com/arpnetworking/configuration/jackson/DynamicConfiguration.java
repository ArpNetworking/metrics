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
package com.arpnetworking.configuration.jackson;

import com.arpnetworking.configuration.Configuration;
import com.arpnetworking.configuration.Listener;
import com.arpnetworking.configuration.Trigger;
import com.arpnetworking.utility.Launchable;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dynamic configuration implementation of <code>Configuration</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DynamicConfiguration extends BaseJacksonConfiguration implements Configuration, Launchable {

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("super", super.toString())
                .add("Snapshot", _snapshot)
                .add("SourceBuilders", _sourceBuilders)
                .add("Listeners", _listeners)
                .add("TriggerEvaluator", _triggerEvaluator)
                .add("TriggerEvaluatorExecutor", _triggerEvaluatorExecutor)
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonNodeSource getJsonSource() {
        return _snapshot.get().getJsonSource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch() {
        LOGGER.debug(String.format("Launching dynamic configuration; this=%s", this));
        _triggerEvaluatorExecutor = Executors.newSingleThreadExecutor();
        _triggerEvaluatorExecutor.submit(_triggerEvaluator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        LOGGER.debug(String.format("Shutting down dynamic configuration; this=%s", this));
        try {
            _triggerEvaluator.stop();
            // CHECKSTYLE.OFF: IllegalCatch - Prevent dynamic configuration from shutting down.
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            LOGGER.error(String.format("Exception shutting down dynamic configuration; configuration=%s", this), e);
        }
        _triggerEvaluatorExecutor.shutdown();
        try {
            _triggerEvaluatorExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            LOGGER.error(String.format("Timed-out shutting down dynamic configuration; configuration=%s", this), e);
        }
    }

    private void loadConfiguration() {
        final List<JsonNodeSource> sources = Lists.transform(
                _sourceBuilders,
                new Function<com.arpnetworking.utility.Builder<? extends JsonNodeSource>, JsonNodeSource>() {
                    @Override
                    public JsonNodeSource apply(final com.arpnetworking.utility.Builder<? extends JsonNodeSource> jsonSourceBuilder) {
                        return jsonSourceBuilder.build();
                    }
                });

        final StaticConfiguration snapshot = new StaticConfiguration.Builder()
                .setObjectMapper(_objectMapper)
                .setSources(sources)
                .build();

        for (final Listener listener : _listeners) {
            try {
                LOGGER.debug(String.format("Offering configuration; listener=%s", listener));
                listener.offerConfiguration(snapshot);
                // CHECKSTYLE.OFF: IllegalCatch - Any exception is considered validation failure.
            } catch (final Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                LOGGER.error(
                        String.format(
                                "Validation of offered configuration failed; listener=%s, configuration=%s",
                                listener,
                                snapshot),
                        e);

                // TODO(vkoskela): Persist "good" configuration across restarts [MAI-?]
                // The code will leave the good configuration in the running instance
                // but the configuration sources may be in a state such that the next
                // restart will only have the latest (currently bad) configuration
                // available.

                return;
            }
        }

        _snapshot.set(snapshot);

        for (final Listener listener : _listeners) {
            try {
                LOGGER.debug(String.format("Applying configuration; listener=%s", listener));
                listener.applyConfiguration();
                // CHECKSTYLE.OFF: IllegalCatch - Apply configuration to all instances.
            } catch (final Exception e) {
                // CHECKSTYLE.ON: IllegalCatch
                LOGGER.warn(
                        String.format(
                                "Applying new configuration failed; listener=%s, configuration=%s",
                                listener,
                                _snapshot),
                        e);
            }
        }
    }

    private DynamicConfiguration(final Builder builder) {
        super(builder);
        _sourceBuilders = ImmutableList.copyOf(builder._sourceBuilders);
        _listeners = ImmutableList.copyOf(builder._listeners);

        _triggerEvaluator = new TriggerEvaluator(Lists.newArrayList(builder._triggers));
    }

    private final AtomicReference<StaticConfiguration> _snapshot = new AtomicReference<>();
    private final List<com.arpnetworking.utility.Builder<? extends JsonNodeSource>> _sourceBuilders;
    private final List<Listener> _listeners;
    private final TriggerEvaluator _triggerEvaluator;

    private ExecutorService _triggerEvaluatorExecutor;

    private static final Duration TRIGGER_EVALUATION_INTERVAL = Duration.standardSeconds(60);
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfiguration.class);

    private final class TriggerEvaluator implements Runnable {

        public TriggerEvaluator(final List<Trigger> triggers) {
            _triggers = triggers;
            _isRunning = true;
        }

        public void stop() {
            _isRunning = false;
        }

        @Override
        public void run() {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(final Thread thread, final Throwable throwable) {
                    LOGGER.error("Unhandled exception!", throwable);
                }
            });

            while (_isRunning) {
                // Evaluate all the triggers to ensure all triggers are reset
                // before loading the configuration.
                boolean reload = false;
                for (final Trigger trigger : _triggers) {
                    try {
                        reload = reload || trigger.evaluateAndReset();
                        // CHECKSTYLE.OFF: IllegalCatch - Evaluate and reset all triggers
                    } catch (final Throwable t) {
                        // CHECKSTYLE.ON: IllegalCatch
                        LOGGER.warn(String.format("Trigger evaluate and reset failed; trigger=%s", trigger), t);
                    }
                }

                // Reload the configuration
                if (reload) {
                    try {
                        loadConfiguration();
                        // CHECKSTYLE.OFF: IllegalCatch - Prevent thread from being killed
                    } catch (final Exception e) {
                        // CHECKSTYLE.ON: IllegalCatch
                        LOGGER.error("Failed to load configuration", e);
                    }
                }

                // Wait for the next evaluation period
                try {
                    final DateTime sleepTimeout = DateTime.now().plus(TRIGGER_EVALUATION_INTERVAL);
                    while (DateTime.now().isBefore(sleepTimeout) && _isRunning) {
                        Thread.sleep(100);
                    }
                } catch (final InterruptedException e) {
                    LOGGER.debug(String.format("Interrupted; isRunning=%s", _isRunning), e);
                }
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(TriggerEvaluator.class)
                    .add("id", Integer.toHexString(System.identityHashCode(this)))
                    .add("IsRunning", _isRunning)
                    .add("Triggers", _triggers)
                    .toString();
        }

        private final List<Trigger> _triggers;
        private volatile boolean _isRunning;
    }

    /**
     * Builder for <code>DynamicConfiguration</code>.
     */
    public static final class Builder extends BaseJacksonConfiguration.Builder<Builder, DynamicConfiguration> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DynamicConfiguration.class);
        }

        /**
         * Set the <code>List</code> of <code>JsonSource</code> instance
         * <code>Builder</code> instances. Cannot be null.
         *
         * @param value The <code>List</code> of <code>JsonSource</code>
         * instance <code>Builder</code> instances.
         * @return This <code>Builder</code> instance.
         */
        public Builder setSourceBuilders(final List<com.arpnetworking.utility.Builder<? extends JsonNodeSource>> value) {
            _sourceBuilders = Lists.newArrayList(value);
            return self();
        }

        /**
         * Add a <code>JsonSource</code> <code>Builder</code> instance.
         *
         * @param value The <code>JsonSource</code> <code>Builder</code> instance.
         * @return This <code>Builder</code> instance.
         */
        public Builder addSourceBuilder(final com.arpnetworking.utility.Builder<? extends JsonNodeSource> value) {
            if (_sourceBuilders == null) {
                _sourceBuilders = Lists.newArrayList();
            }
            _sourceBuilders.add(value);
            return self();
        }

        /**
         * Set the <code>List</code> of <code>Trigger</code> instances. Cannot
         * be null.
         *
         * @param value The <code>List</code> of <code>Trigger</code> instances.
         * @return This <code>Builder</code> instance.
         */
        public Builder setTriggers(final List<Trigger> value) {
            _triggers = Lists.newArrayList(value);
            return self();
        }

        /**
         * Add a <code>ConfigurationTrigger</code> instance.
         *
         * @param value The <code>ConfigurationTrigger</code> instance.
         * @return This <code>Builder</code> instance.
         */
        public Builder addTrigger(final Trigger value) {
            if (_triggers == null) {
                _triggers = Lists.newArrayList(value);
            } else {
                _triggers.add(value);
            }
            return self();
        }

        /**
         * Set the <code>List</code> of <code>Listener</code> instances. Cannot
         * be null.
         *
         * @param value The <code>List</code> of <code>Listener</code> instances.
         * @return This <code>Builder</code> instance.
         */
        public Builder setListeners(final List<Listener> value) {
            _listeners = Lists.newArrayList(value);
            return self();
        }

        /**
         * Add a <code>ConfigurationListener</code> instance.
         *
         * @param value The <code>ConfigurationListener</code> instance.
         * @return This <code>Builder</code> instance.
         */
        public Builder addListener(final Listener value) {
            if (_listeners == null) {
                _listeners = Lists.newArrayList(value);
            } else {
                _listeners.add(value);
            }
            return self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private List<com.arpnetworking.utility.Builder<? extends JsonNodeSource>> _sourceBuilders;
        @NotNull
        private List<Trigger> _triggers = Lists.newArrayList();
        @NotNull
        private List<Listener> _listeners;
    }
}
