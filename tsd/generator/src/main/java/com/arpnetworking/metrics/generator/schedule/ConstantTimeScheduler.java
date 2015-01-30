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

package com.arpnetworking.metrics.generator.schedule;

import org.joda.time.Period;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler that schedules the next at a constant time after the previous.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ConstantTimeScheduler implements Scheduler {

    /**
     * Public constructor.
     *
     * @param time The time interval for scheduling.
     */
    public ConstantTimeScheduler(final Period time) {
        _time = TimeUnit.NANOSECONDS.convert(time.toStandardDuration().getMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Public constructor.
     *
     * @param period The time interval in nanoseconds.
     */
    public ConstantTimeScheduler(final long period) {
        _time = period;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long next(final long previousExecutionTime) {
        return previousExecutionTime + _time;
    }

    private final long _time;
}
