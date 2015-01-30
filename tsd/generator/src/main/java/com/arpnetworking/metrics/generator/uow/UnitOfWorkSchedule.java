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

package com.arpnetworking.metrics.generator.uow;

import com.arpnetworking.metrics.generator.schedule.Scheduler;

/**
 * Holds a <code>UnitOfWorkGenerator</code> and a scheduler.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class UnitOfWorkSchedule {
    /**
     * Public constructor.
     *
     * @param generator Metric generator.
     * @param scheduler Schedule to generate the metric with.
     */
    public UnitOfWorkSchedule(final UnitOfWorkGenerator generator, final Scheduler scheduler) {
        _generator = generator;
        _scheduler = scheduler;
    }

    public UnitOfWorkGenerator getGenerator() {
        return _generator;
    }

    public Scheduler getScheduler() {
        return _scheduler;
    }

    private final UnitOfWorkGenerator _generator;
    private final Scheduler _scheduler;
}
