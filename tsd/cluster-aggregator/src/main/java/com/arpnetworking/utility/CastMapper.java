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

package com.arpnetworking.utility;

import akka.dispatch.Mapper;

/**
 * Map method that just casts to another class.
 *
 * @param <T> Input type
 * @param <R> Output type
 * @author Brandon Arp (barp at groupon dot com)
 */
public class CastMapper<T, R> extends Mapper<T, R> {
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public R apply(final T parameter) {
        return (R) parameter;
    }
}

