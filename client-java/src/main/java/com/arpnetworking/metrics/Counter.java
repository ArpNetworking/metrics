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

/**
 * Interface for counter. Instances are initialized to zero on creation. The
 * zero-value sample is recorded when the <code>Metrics</code> instance is 
 * closed if no other actions are taken on the <code>Counter</code>.
 * 
 * Modifying the <code>Counter</code> instance's value modifies the single 
 * sample value. When the associated <code>Metrics</code> instance is closed
 * whatever value the sample has is recorded. To create another sample you
 * create a new <code>Counter</code> instance with the same name. 
 * 
 * Each timer instance is bound to a <code>Metrics</code> instance. After the 
 * <code>Metrics</code> instance is closed any modifications to the  
 * <code>Counter</code> instances bound to that <code>Metrics</code> instance
 * will be ignored. 
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface Counter {

    /**
     * Increment the counter sample by 1.
     */
    void increment();

    /**
     * Decrement the counter sample by 1.
     */
    void decrement();

    /**
     * Increment the counter sample by the specified value.
     * 
     * @param value The value to increment the counter by.
     */
    void increment(long value);

    /**
     * Decrement the counter sample by the specified value.
     * 
     * @param value The value to decrement the counter by.
     */
    void decrement(long value);
}
