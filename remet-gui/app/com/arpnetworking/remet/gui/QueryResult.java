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
package com.arpnetworking.remet.gui;

import java.util.List;

/**
 * Interface describing the result of a generic query.
 *
 * @param <T> The result value type.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface QueryResult<T> {

    /**
     * The matching instances of {@code <T>}.
     *
     * @return The matching instances of {@code <T>}.
     */
    List<? extends T> values();

    /**
     * The total number of matching instances of {@code <T>}. This may be greater than the number of instances of
     * {@code <T>} returned.
     *
     * @return The total number of matching hosts.
     */
    long total();
}
