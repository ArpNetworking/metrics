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

package com.arpnetworking.metrics.generator.util;

import com.arpnetworking.metrics.generator.uow.UnitOfWorkSchedule;

/**
 * Represents work item in a scheduling <code>PriorityQueue</code>.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
class WorkEntry {
    /**
     * Public constructor.
     *
     * @param schedule The unit of work schedule info.
     * @param value The current execution time.
     */
    public WorkEntry(final UnitOfWorkSchedule schedule, final long value) {
        _schedule = schedule;
        _currentValue = value;
    }

    public long getCurrentValue() {
        return _currentValue;
    }

    public UnitOfWorkSchedule getSchedule() {
        return _schedule;
    }

    private final long _currentValue;
    private final UnitOfWorkSchedule _schedule;
}
