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

import com.arpnetworking.configuration.triggers.DirectoryTrigger;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.Collection;
import java.util.List;
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
        for (final File directory : _directories) {
            builder
                    .addSourceBuilder(
                            new JsonNodeDirectorySource.Builder()
                                    .setDirectory(directory)
                                    .setFileNames(keys)
                                    .setFileNamePatterns(keyPatterns))
                    .addTrigger(new DirectoryTrigger.Builder()
                            .setDirectory(directory)
                            .setFileNames(keys)
                            .setFileNamePatterns(keyPatterns)
                            .build());
        }

        return builder.build();
    }

    private DirectoryDynamicConfigurationFactory(final Builder builder) {
        _directories = Lists.newArrayList(builder._directories);
    }

    private final List<File> _directories;

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
         * Set the directories.
         *
         * @param value The directories.
         * @return This <code>Builder</code> instance.
         */
        public Builder setDirectories(final List<File> value) {
            _directories = value;
            return this;
        }

        private List<File> _directories;
    }
}
