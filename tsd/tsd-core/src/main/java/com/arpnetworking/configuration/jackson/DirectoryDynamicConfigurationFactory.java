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

import com.arpnetworking.configuration.DirectoryTrigger;
import com.arpnetworking.utility.OvalBuilder;

import java.io.File;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Implementation of <code>DynamicConfigurationFactory</code> which maps keys
 * and key patterns to to file names and file name patterns.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DirectoryDynamicConfigurationFactory implements DynamicConfigurationFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamicConfiguration create(
            final DynamicConfiguration.Builder builder,
            final Collection<String> keys,
            final Collection<Pattern> keyPatterns) {

        return builder
                .addSourceBuilder(new JsonNodeDirectorySource.Builder()
                        .setDirectory(_directory)
                        .setFileNames(keys)
                        .setFileNamePatterns(keyPatterns))
                .addTrigger(new DirectoryTrigger.Builder()
                        .setDirectory(_directory)
                        .setFileNames(keys)
                        .setFileNamePatterns(keyPatterns)
                        .build())
                .build();
    }

    private DirectoryDynamicConfigurationFactory(final Builder builder) {
        _directory = builder._directory;
    }

    private final File _directory;

    /**
     * <code>Builder</code> implementation for <code>DirectoryDynamicConfigurationFactory</code>.
     */
    public static final class Builder extends OvalBuilder<DirectoryDynamicConfigurationFactory> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DirectoryDynamicConfigurationFactory.class);
        }

        /**
         * Set the directory.
         *
         * @param value The directory.
         * @return This <code>Builder</code> instance.
         */
        public Builder setDirectory(final File value) {
            _directory = value;
            return this;
        }

        private File _directory;
    }
}
