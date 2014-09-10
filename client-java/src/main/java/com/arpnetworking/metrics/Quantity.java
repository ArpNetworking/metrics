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
 * Interface for value and unit. This class is thread safe.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface Quantity {

    /**
     * Access the magnitude of the quantity.
     * 
     * @return The magnitude.
     */
    Number getValue();

    /**
     * Access the units that the magnitude is expressed in.
     * 
     * @return The units of the magnitude.
     */
    Unit getUnit();
}
