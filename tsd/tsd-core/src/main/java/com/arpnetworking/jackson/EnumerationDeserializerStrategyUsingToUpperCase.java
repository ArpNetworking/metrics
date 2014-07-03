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
 * Implementation of <code>EnumerationDeserializerStrategy</code> that uses the
 * case insensitive name of the enumerations to obtain the values.
 *
 * This assumes that all the values of the enum are on capitalized.
 *
 * @param <T> The class/type of enumeration.
 * 
 * @author Carlos Indo (carlos at groupon dot com)
 */
public final class EnumerationDeserializerStrategyUsingToUpperCase<T extends Enum<T>>
        implements EnumerationDeserializerStrategy<T> {

    /**
     * Creates a new instance of <code>EnumerationDeserializerStrategyUsingToUpperCase</code>.
     *
     * @param <T> The class/type of enumeration.
     * @return a new instance of <code>EnumerationDeserializerStrategyUsingToUpperCase</code>.
     */
    public static <T extends Enum<T>> EnumerationDeserializerStrategy<T> newInstance() {
        return new EnumerationDeserializerStrategyUsingToUpperCase<T>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T toEnum(final Class<T> enumClass, final String key) throws EnumerationNotFoundException {
        try {
            return Enum.valueOf(enumClass, key.toUpperCase());
        } catch (final IllegalArgumentException | NullPointerException ex) {
            throw new EnumerationNotFoundException(key, enumClass, ex);
        }
    }

    private EnumerationDeserializerStrategyUsingToUpperCase() {}
}
