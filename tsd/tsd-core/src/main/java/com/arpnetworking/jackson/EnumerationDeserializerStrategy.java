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
package com.arpnetworking.jackson;

import com.arpnetworking.exceptions.EnumerationNotFoundException;

/**
 * Interface for the strategy pattern on the <code>EnumerationDeserializer</code>.
 *
 * @param <T> The class/type of enumeration.
 * 
 * @author Carlos Indo (carlos at groupon dot com)
 */
public interface EnumerationDeserializerStrategy<T extends Enum<T>> {

    /**
     * Strategy method to map enumerations of type E from a String.
     *
     * @param enumClass The enumeration class/type.
     * @param key The serialized enumeration key to deserialize.
     * @return an instance of the enumeration represented by key.
     * @throws EnumerationNotFoundException when an instance cannot be found
     * matching the provided key using this strategy.
     */
    T toEnum(final Class<T> enumClass, final String key) throws EnumerationNotFoundException;
}
