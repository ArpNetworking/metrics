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
package com.arpnetworking.tsdcore.limiter;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.utility.Launchable;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.joda.time.DateTime;

/**
 * Interface for classes which track and limit the number of metrics created.
 *
 * @author Joe Frisbie (jfrisbie at groupon dot com)
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface MetricsLimiter extends Launchable {

    /**
     * Apply a limit to time series data based on number of unique metric
     * aggregation periods.
     *
     * @param data Instance of <code>AggregatedData</code>.
     * @param time The timestamp the data was last seen at.
     * @return True if and only if the data passes the limit test.
     */
    boolean offer(AggregatedData data, DateTime time);
}
