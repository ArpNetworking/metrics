/**
 * Copyright 2016 Groupon.com
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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.tsdcore.model.PeriodicData;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.sf.oval.constraint.NotNull;

import java.util.Optional;

/**
 * Sink extracts and adds dimension for "domain" based on hostname.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DomainInjectingSink extends BaseSink {

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final PeriodicData data) {
        if (!data.getDimensions().containsKey(DOMAIN_DIMENSION_NAME)) {
            final Optional<String> domain = getDomain(data.getDimensions().get("host"));
            if (domain.isPresent()) {
                final PeriodicData.Builder dataBuilder = PeriodicData.Builder.clone(data);
                dataBuilder.setDimensions(ImmutableMap.<String, String>builder()
                        .putAll(data.getDimensions())
                        .put(DOMAIN_DIMENSION_NAME, domain.get())
                        .build());
                _sink.recordAggregateData(dataBuilder.build());
                return;
            }
        }
        _sink.recordAggregateData(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // Nothing to do
    }

    private Optional<String> getDomain(final String host) {
        if (host == null) {
            return Optional.empty();
        }
        Optional<String> longestSuffix = Optional.empty();
        for (final String suffix : _suffixes) {
            if (host.endsWith(SUFFIX_SEPARATOR + suffix)
                    && (!longestSuffix.isPresent() || suffix.length() > longestSuffix.get().length())) {
                longestSuffix = Optional.of(suffix);
            }
        }
        return longestSuffix;
    }

    private DomainInjectingSink(final Builder builder) {
        super(builder);
        _sink = builder._sink;
        _suffixes = builder._suffixes;
    }

    private final Sink _sink;
    private final ImmutableSet<String> _suffixes;

    private static final String SUFFIX_SEPARATOR = ".";
    private static final String DOMAIN_DIMENSION_NAME = "domain";

    /**
     * Implementation of builder pattern for <code>DomainInjectingSink</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static final class Builder extends BaseSink.Builder<Builder, DomainInjectingSink> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(DomainInjectingSink.class);
        }

        /**
         * Sets recognized domain suffixes.
         *
         * @param value The recognized suffixes.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSuffixes(final ImmutableSet<String> value) {
            _suffixes = value;
            return self();
        }

        /**
         * The sink to wrap. Cannot be null.
         *
         * @param value The sink to wrap.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSink(final Sink value) {
            _sink = value;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private ImmutableSet<String> _suffixes = ImmutableSet.of();
        @NotNull
        private Sink _sink;
    }
}
