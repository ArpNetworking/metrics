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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Optional;

import java.io.IOException;

/**
 * Serializer for <code>Optional</code>. The serialized format of an optional
 * is simply the serialized format of the object contained in the optional or
 * null if no instance is present.
 *
 * TODO(vkoskela): This is _duplicated_ in metrics-portal and should find its way to a common utility package.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@SuppressWarnings("rawtypes")
public final class OptionalSerializer extends JsonSerializer<Optional> {

    /**
     * Create a new instance of <code>JsonSerializer&lt;Optional&gt;</code>.
     *
     * @return New instance of <code>JsonSerializer&lt;Optional&gt;</code>.
     */
    public static JsonSerializer<Optional> newInstance() {
        return new OptionalSerializer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(
            final Optional optional,
            final JsonGenerator generator,
            final SerializerProvider provider)
            throws IOException {
        if (optional.isPresent()) {
            provider.defaultSerializeValue(optional.get(), generator);
        } else {
            provider.defaultSerializeNull(generator);
        }
    }

    private OptionalSerializer() {}
}

