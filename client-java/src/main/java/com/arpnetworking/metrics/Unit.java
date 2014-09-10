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
package com.arpnetworking.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Units available for recording metrics. The units are used to aggregate values
 * of the same metric published in different units (e.g. bytes and kilobytes).
 * Publishing a metric with units from different domains will cause some of the
 * data to be discarded by the aggregator (e.g. bytes and seconds). This 
 * includes discarding data when some data has a unit and some data does not 
 * have any unit. This library cannot detect such inconsistencies since 
 * aggregation can occur across Metric instances, processes and even hosts.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public enum Unit {

    // Time
    /**
     * Nanoseconds.
     */
    NANOSECOND("nanosecond"),
    /**
     * Microseconds.
     */
    MICROSECOND("microsecond"),
    /**
     * Milliseconds.
     */
    MILLISECOND("millisecond"),
    /**
     * Seconds.
     */
    SECOND("second"),
    /**
     * Minutes.
     */
    MINUTE("minute"),
    /**
     * Hours.
     */
    HOUR("hour"),
    /**
     * Days.
     */
    DAY("day"),
    /**
     * Weeks.
     */
    WEEK("week"),

    // Size
    /**
     * Bytes.
     */
    BYTE("byte"),
    /**
     * Kilobytes.
     */
    KILOBYTE("kilobyte"),
    /**
     * Megabytes.
     */
    MEGABYTE("megabyte"),
    /**
     * Gigabytes.
     */
    GIGABYTE("gigabyte");

    public String getSerializedName() {
        return _serializedName;
    }

    /**
     * Transform a <code>TimeUnit</code> into a <code>Unit</code>. If the no
     * such transformation exists the function returns <code>null</code>.
     * 
     * @param timeUnit The <code>TimeUnit</code> to transform.
     * @return The corresponding <code>Unit</code>.
     */
    public static Unit fromTimeUnit(final TimeUnit timeUnit) {
        return TIME_UNIT_MAP.get(timeUnit);
    }

    /**
     * Transform a <code>Unit</code> into a <code>TimeUnit</code>. If the no
     * such transformation exists the function returns <code>null</code>.
     * 
     * @param unit The <code>Unit</code> to transform.
     * @return The corresponding <code>TimeUnit</code>.
     */
    public static TimeUnit toTimeUnit(final Unit unit) {
        return INVERSE_TIME_UNIT_MAP.get(unit);
    }

    private Unit(final String serializedName) {
        _serializedName = serializedName;
    }

    private final String _serializedName;

    // CHECKSTYLE.OFF: IllegalInstantiation - No Guava dependency here.
    private static final Map<TimeUnit, Unit> TIME_UNIT_MAP = new HashMap<>();
    private static final Map<Unit, TimeUnit> INVERSE_TIME_UNIT_MAP = new HashMap<>();
    // CHECKSTYLE.ON: IllegalInstantiation

    static {
        TIME_UNIT_MAP.put(TimeUnit.DAYS, Unit.DAY);
        TIME_UNIT_MAP.put(TimeUnit.HOURS, Unit.HOUR);
        TIME_UNIT_MAP.put(TimeUnit.MINUTES, Unit.MINUTE);
        TIME_UNIT_MAP.put(TimeUnit.SECONDS, Unit.SECOND);
        TIME_UNIT_MAP.put(TimeUnit.MILLISECONDS, Unit.MILLISECOND);
        TIME_UNIT_MAP.put(TimeUnit.MICROSECONDS, Unit.MICROSECOND);
        TIME_UNIT_MAP.put(TimeUnit.NANOSECONDS, Unit.NANOSECOND);

        for (final Map.Entry<TimeUnit, Unit> entry : TIME_UNIT_MAP.entrySet()) {
            INVERSE_TIME_UNIT_MAP.put(entry.getValue(), entry.getKey());
        }
    }
}
