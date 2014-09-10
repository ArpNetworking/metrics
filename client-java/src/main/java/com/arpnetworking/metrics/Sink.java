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

import java.util.List;
import java.util.Map;

/**
 * Interface representing a destination to record metrics to.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface Sink {

    /**
     * Invoked by <code>Metrics</code> to record data to this <code>Sink</code>.
     * 
     * @param annotations The annotations.
     * @param timerSamples The timer samples.
     * @param counterSamples The counter samples.
     * @param gaugeSamples The guage samples.
     */
    void record(
            Map<String, String> annotations,
            Map<String, List<Quantity>> timerSamples,
            Map<String, List<Quantity>> counterSamples,
            Map<String, List<Quantity>> gaugeSamples);
}
