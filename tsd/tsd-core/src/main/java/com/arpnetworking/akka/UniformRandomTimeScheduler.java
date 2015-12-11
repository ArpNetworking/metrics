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
package com.arpnetworking.akka;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Scheduler;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.utility.OvalBuilder;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.ValidateWithMethod;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scheduler that will send a message in a uniform random time interval.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class UniformRandomTimeScheduler {
    /**
     * Stops the scheduling.
     */
    public void stop() {
        final Cancellable cancellable = _scheduled.getAndSet(null);
        if (cancellable != null) {
            cancellable.cancel();
        }
    }

    private Cancellable schedule() {
        // Compute the next tick
        final int spreadMillis = (int) _maximumTime.minus(_minimumTime).toMillis();
        final int base = (int) _minimumTime.toMillis();
        final FiniteDuration randomSleep = FiniteDuration.apply(_random.nextInt(spreadMillis) + base, TimeUnit.MILLISECONDS);
        return _scheduler.scheduleOnce(randomSleep, (Runnable) this::sendAndScheduleMessage, _executionContext);
    }

    private void sendAndScheduleMessage() {
        _target.tell(_message, _sender);
        _scheduled.set(schedule());
    }

    private UniformRandomTimeScheduler(final Builder builder) {
        _executionContext = builder._executionContext;
        _maximumTime = builder._maximumTime;
        _minimumTime = builder._minimumTime;
        _scheduler = builder._scheduler;
        _sender = builder._sender;
        _target = builder._target;
        _message = builder._message;
        sendAndScheduleMessage();
    }

    private AtomicReference<Cancellable> _scheduled = new AtomicReference<>(null);

    private final ExecutionContext _executionContext;
    private final FiniteDuration _maximumTime;
    private final FiniteDuration _minimumTime;
    private final Scheduler _scheduler;
    private final ActorRef _sender;
    private final ActorRef _target;
    private final Random _random = new Random();
    private final Object _message;

    private static final Logger LOGGER = LoggerFactory.getLogger(UniformRandomTimeScheduler.class);

    /**
     * Builder for {@link UniformRandomTimeScheduler}.
     */
    public static final class Builder extends OvalBuilder<UniformRandomTimeScheduler> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(UniformRandomTimeScheduler.class);
        }

        /**
         * The target actor. Required. Cannot be null.
         *
         * @param value The actor to send the message to.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setTarget(final ActorRef value) {
            _target = value;
            return this;
        }

        /**
         * The actor the message will be from. Optional. Defaults to ActorRef.noSender().
         *
         * @param value The actor the message will be from.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSender(final ActorRef value) {
            _sender = value;
            return this;
        }

        /**
         * The scheduler to schedule with. Required. Cannot be null.
         *
         * @param value The scheduler to schedule with.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setScheduler(final Scheduler value) {
            _scheduler = value;
            return this;
        }

        /**
         * The message to send. Required. Cannot be null.
         *
         * @param value The message to send.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMessage(final Object value) {
            _message = value;
            return this;
        }

        /**
         * The execution context to run the send on. Required. Cannot be null.
         *
         * @param value The execution context to send on.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setExecutionContext(final ExecutionContext value) {
            _executionContext = value;
            return this;
        }

        /**
         * The minimum time. Required. Cannot be null. Must be greater than or equal to 0.
         *
         * @param value The minimum time between message sends.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMinimumTime(final FiniteDuration value) {
            _minimumTime = value;
            return this;
        }

        /**
         * The maximum time. Required. Cannot be null. Must be greater than minimum time.
         *
         * @param value The maximum time between message sends.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setMaximumTime(final FiniteDuration value) {
            _maximumTime = value;
            return this;
        }

        private boolean minZeroDuration(final FiniteDuration duration) {
            return Duration.Zero().lteq(duration);
        }

        private boolean minLessThanMax(final FiniteDuration max) {
            return max.gt(_minimumTime);
        }

        @NotNull
        private ActorRef _target;
        @NotNull
        @ValidateWithMethod(methodName = "minZeroDuration", parameterType = FiniteDuration.class)
        private FiniteDuration _minimumTime;
        @NotNull
        @ValidateWithMethod.List(value = {
            @ValidateWithMethod(methodName = "minZeroDuration", parameterType = FiniteDuration.class),
            @ValidateWithMethod(methodName = "minLessThanMax", parameterType = FiniteDuration.class)})
        private FiniteDuration _maximumTime;
        @NotNull
        private ActorRef _sender = ActorRef.noSender();
        @NotNull
        private ExecutionContext _executionContext;
        @NotNull
        private Scheduler _scheduler;
        @NotNull
        private Object _message;
    }
}
