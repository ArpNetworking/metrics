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

package com.arpnetworking.clusteraggregator.configuration;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.utility.ConfiguredLaunchableFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;

import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Serves as a router for configuration-created actors.  Handles reconfiguration messages and swaps references on reconfiguration.
 *
 * @param <T> The type of configuration
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConfigurableActorProxy<T> extends UntypedActor {
    /**
     * Creates a {@link Props}.
     *
     * @param factory factory to create an actor
     * @param <T>     configuration type
     * @return a new {@link Props}
     */
    public static <T> Props props(final ConfiguredLaunchableFactory<Props, T> factory) {
        return Props.create(ConfigurableActorProxy.class, factory);
    }

    /**
     * Public constructor.
     *
     * @param factory Factory to create an actor from a configuration.
     */
    public ConfigurableActorProxy(final ConfiguredLaunchableFactory<Props, T> factory) {
        _factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof ApplyConfiguration) {
            @SuppressWarnings("unchecked")
            final ApplyConfiguration<T> applyConfiguration = (ApplyConfiguration<T>) message;
            applyConfiguration(applyConfiguration);
        } else if (message instanceof SwapActor) {
            swapActor();
        } else if (message instanceof Terminated) {
            actorTerminated((Terminated) message);
        } else if (message instanceof SubscribeToNotifications) {
            _observers.add(sender());
        } else {
            if (_state.equals(Service.State.RUNNING)) {
                _currentChild.get().forward(message, context());
            } else {
                if (_messageBuffer.size() >= MAX_BUFFERED_MESSAGES) {
                    final BufferedMessage dropped = _messageBuffer.remove();
                    LOGGER.error()
                            .setMessage("Message buffer full, dropping oldest message")
                            .addData("dropped", dropped.getMessage())
                            .addContext("actor", self())
                            .log();
                }
                // TODO(barp): record the buffer size as a metric [MAI-472]
                _messageBuffer.add(new BufferedMessage(sender(), message));
            }
        }
    }

    private void actorTerminated(final Terminated message) {
        LOGGER.trace()
                .setMessage("Received a terminated message")
                .addData("terminated", message)
                .addContext("actor", self())
                .log();
        final ActorRef terminatedActor = message.actor();
        // Make sure the currentChild is the one that died
        if (!terminatedActor.equals(_currentChild.orNull())) {
            LOGGER.error()
                    .setMessage("Terminated message received from unknown actor")
                    .addData("terminated", message)
                    .addContext("actor", self())
                    .log();
            return;
        }
        _currentChild = Optional.absent();
        self().tell(new SwapActor(), self());
    }

    private void swapActor() {
        LOGGER.trace()
                .setMessage("Received a swap actor message")
                .addContext("actor", self())
                .log();
        // The old actor should be stopped already, create the new ref and set it
        final Props props = _factory.create(_pendingConfiguration.get());
        final ActorRef newRef = context().actorOf(props);
        context().watch(newRef);
        LOGGER.debug()
                .setMessage("Started new actor due to configuration change")
                .addData("newActor", newRef)
                .addContext("actor", self())
                .log();
        final ConfigurableActorStarted actorStartedNotification = new ConfigurableActorStarted(newRef);
        _observers.forEach(o -> o.tell(actorStartedNotification, self()));

        _currentChild = Optional.of(newRef);
        _pendingConfiguration = Optional.absent();
        _state = Service.State.RUNNING;
        while (!_messageBuffer.isEmpty()) {
            final BufferedMessage entry = _messageBuffer.remove();
            newRef.tell(entry.getMessage(), entry.getSender());
        }
    }

    private void applyConfiguration(final ApplyConfiguration<T> applyConfiguration) {
        LOGGER.trace()
                .setMessage("Received an apply configuration message")
                .addData("configuration", applyConfiguration.getConfiguration())
                .addContext("actor", self())
                .log();
        _pendingConfiguration = Optional.of(applyConfiguration.getConfiguration());
        // Tear down the old actor
        if (_currentChild.isPresent() && _state.equals(Service.State.RUNNING)) {
            _state = Service.State.STOPPING;
            final ActorRef toStop = _currentChild.get();
            toStop.tell(PoisonPill.getInstance(), self());
            LOGGER.info()
                    .setMessage("Requested current actor to shutdown for swap")
                    .addData("currentActor", toStop)
                    .addContext("actor", self())
                    .log();
        } else if (_state.equals(Service.State.NEW)) {
            LOGGER.info()
                    .setMessage("Applying initial configuration, no current actor to shutdown")
                    .addContext("actor", self())
                    .log();
            self().tell(new SwapActor(), self());
        }
    }

    private Optional<ActorRef> _currentChild = Optional.absent();
    private Optional<T> _pendingConfiguration = Optional.absent();
    private Service.State _state = Service.State.NEW;
    private final ConfiguredLaunchableFactory<Props, T> _factory;
    private final Deque<BufferedMessage> _messageBuffer = new LinkedList<>();
    private final List<ActorRef> _observers = Lists.newArrayList();

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableActorProxy.class);
    private static final int MAX_BUFFERED_MESSAGES = 10000;

    private static final class SwapActor {
    }

    private static final class BufferedMessage {
        private BufferedMessage(final ActorRef sender, final Object message) {
            _sender = sender;
            _message = message;
        }

        public Object getMessage() {
            return _message;
        }

        public ActorRef getSender() {
            return _sender;
        }

        private final ActorRef _sender;
        private final Object _message;
    }

    /**
     * Message class to cause a new configuration to be applied.
     *
     * @param <C> type of the configuration
     */
    public static final class ApplyConfiguration<C> {
        /**
         * Public constructor.
         *
         * @param configuration Configuration to apply. Must be immutable.
         */
        public ApplyConfiguration(final C configuration) {
            _configuration = configuration;
        }

        public C getConfiguration() {
            return _configuration;
        }

        private final C _configuration;
    }

    /**
     * Message class to send to the {@link com.arpnetworking.clusteraggregator.configuration.ConfigurableActorProxy} to
     * indicate you want to receive event notifications.
     */
    public static final class SubscribeToNotifications implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Message class to tell observers that a new actor has started due to a configuration change.
     */
    public static final class ConfigurableActorStarted implements Serializable {
        /**
         * Public constructor.
         *
         * @param actor The new actor started due to a configuration change.
         */
        public ConfigurableActorStarted(final ActorRef actor) {
            _actor = actor;
        }

        public ActorRef getActor() {
            return _actor;
        }

        private final ActorRef _actor;
        private static final long serialVersionUID = 1L;
    }
}
