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
package com.arpnetworking.configuration.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Interface for classes which create <code>DynamicConfiguration</code>
 * instances. The factory enables dynamic configurations where the definition
 * of the dynamic configuration, namely its source builders and triggers vary
 * based on data available at runtime.
 *
 * This is accomplished by creating a new dynamic configuration instance. The
 * specific sources and triggers included in the dynamic configuration are left
 * up to the specific implementation of this interface as is the interpretation
 * or mapping of keys and key patterns.
 *
 * The builder is supplied to the factory's create method in order to allow the
 * caller to set any non-source/trigger specific fields on the builder prior to
 * creation of the new dynamic configuration instance.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface DynamicConfigurationFactory {

    /**
     * Create a new <code>DynamicConfiguration</code> updated with the
     * specified keys and key patterns.
     *
     * @param builder The <code>Builder</code> for <code>DynamicConfiguration</code>.
     * @param keys The <code>Collection</code> of keys.
     * @param keyPatterns The <code>Collection</code> of key <code>Pattern</code>
     * instances.
     * @return New instance of <code>DynamicConfiguration</code>.
     */
    DynamicConfiguration create(
            final DynamicConfiguration.Builder builder,
            final Collection<String> keys,
            final Collection<Pattern> keyPatterns);
}
