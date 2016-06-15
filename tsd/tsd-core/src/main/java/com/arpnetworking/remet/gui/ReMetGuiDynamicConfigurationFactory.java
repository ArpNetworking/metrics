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
package com.arpnetworking.remet.gui;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.DynamicConfigurationFactory;
import com.arpnetworking.configuration.jackson.JsonNodePaginatedUriSource;
import com.arpnetworking.configuration.triggers.UriTrigger;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of <code>DynamicConfigurationFactory</code> which maps two part keys
 * to cluster and service parameter values in API calls to ReMet Gui for configuration
 * data.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class ReMetGuiDynamicConfigurationFactory implements DynamicConfigurationFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamicConfiguration create(
            final DynamicConfiguration.Builder builder,
            final Collection<Key> keys) {
        update(builder, keys);
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(
            final DynamicConfiguration.Builder builder,
            final Collection<Key> keys) {
        for (final Key key : keys) {
            // Require two part key; cluster and service names
            final List<String> parts = key.getParts();
            if (parts.size() != 2) {
                throw new IllegalArgumentException(String.format("Invalid key; key=%s", key));
            }

            // Create the key specific uri
            final URI keyUri;
            try {
                keyUri = new URIBuilder(_uri)
                        .addParameter(_clusterKey, parts.get(0))
                        .addParameter(_serviceKey, parts.get(1))
                        .build();
            } catch (final URISyntaxException e) {
                throw Throwables.propagate(e);
            }

            // Add the key specific paginated uri source and trigger
            builder
                    .addSourceBuilder(
                            new JsonNodePaginatedUriSource.Builder()
                                    .setUri(keyUri)
                                    .setDataKeys(ImmutableList.of("data"))
                                    .setNextPageKeys(ImmutableList.of("pagination", "next")))
                    .addTrigger(
                            new UriTrigger.Builder()
                                    .setUri(keyUri)
                                    .build());
        }
    }

    /**
     * {@inheritDoc}
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("uri", _uri)
                .put("clusterKey", _clusterKey)
                .put("serviceKey", _serviceKey)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private ReMetGuiDynamicConfigurationFactory(final Builder builder) {
        _uri = builder._uri;
        _clusterKey = builder._clusterKey;
        _serviceKey = builder._serviceKey;
    }

    private final URI _uri;
    private final String _clusterKey;
    private final String _serviceKey;

    /**
     * <code>Builder</code> implementation for <code>DirectoryDynamicConfigurationFactory</code>.
     */
    public static final class Builder extends OvalBuilder<ReMetGuiDynamicConfigurationFactory> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(ReMetGuiDynamicConfigurationFactory.class);
        }

        /**
         * Set the <code>URI</code>. Required. Cannot be null.
         *
         * @param value The <code>URI</code>.
         * @return This <code>Builder</code> instance.
         */
        public Builder setUri(final URI value) {
            _uri = value;
            return this;
        }

        /**
         * Set the cluster parameter key. Optional. Default is "cluster". Cannot be null or empty.
         *
         * @param value The cluster parameter key.
         * @return This <code>Builder</code> instance.
         */
        public Builder setClusterKey(final String value) {
            _clusterKey = value;
            return this;
        }

        /**
         * Set the service parameter key. Optional. Default is "service". Cannot be null or empty.
         *
         * @param value The service parameter key.
         * @return This <code>Builder</code> instance.
         */
        public Builder setServiceKey(final String value) {
            _serviceKey = value;
            return this;
        }

        @NotNull
        private URI _uri;
        @NotEmpty
        @NotNull
        private String _clusterKey = "cluster";
        @NotEmpty
        @NotNull
        private String _serviceKey = "service";
    }
}
