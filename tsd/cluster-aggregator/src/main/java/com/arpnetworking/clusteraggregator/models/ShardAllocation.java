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
package com.arpnetworking.clusteraggregator.models;

import akka.actor.ActorRef;
import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.utility.LexicalNumericComparator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a shard allocation.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class ShardAllocation {
    public String getHost() {
        return _host;
    }

    public ActorRef getShardRegion() {
        return _shardRegion;
    }

    @JsonSerialize(using = CountingSetSerializer.class)
    public Set<String> getCurrentShards() {
        return Collections.unmodifiableSet(_currentShards);
    }

    @JsonSerialize(using = CountingSetSerializer.class)
    public Set<String> getIncomingShards() {
        return Collections.unmodifiableSet(_incomingShards);
    }

    @JsonSerialize(using = CountingSetSerializer.class)
    public Set<String> getOutgoingShards() {
        return Collections.unmodifiableSet(_outgoingShards);
    }

    private ShardAllocation(final Builder builder) {
        _host = builder._host;
        _shardRegion = builder._shardRegion;
        _currentShards = Sets.newTreeSet(new LexicalNumericComparator());
        _currentShards.addAll(builder._currentShards);
        _incomingShards = Sets.newTreeSet(new LexicalNumericComparator());
        _incomingShards.addAll(builder._incomingShards);
        _outgoingShards = Sets.newTreeSet(new LexicalNumericComparator());
        _outgoingShards.addAll(builder._outgoingShards);
    }

    private final String _host;
    private final ActorRef _shardRegion;
    private final Set<String> _currentShards;
    private final Set<String> _incomingShards;
    private final Set<String> _outgoingShards;

    /**
     * Implementation of builder pattern for {@link ShardAllocation}.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static class Builder extends OvalBuilder<ShardAllocation> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(ShardAllocation.class);
        }

        /**
         * The shards currently on the node. Required. Cannot be null.
         *
         * @param value The shards.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setCurrentShards(final Set<String> value) {
            _currentShards = value;
            return this;
        }

        /**
         * The shards in the process of being rebalanced away from this node. Required. Cannot be null.
         *
         * @param value The shards.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setOutgoingShards(final Set<String> value) {
            _outgoingShards = value;
            return this;
        }

        /**
         * The shards in the process of being rebalanced to this node. Required. Cannot be null.
         *
         * @param value The shards.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setIncomingShards(final Set<String> value) {
            _incomingShards = value;
            return this;
        }

        /**
         * The shard region owning the shards. Required. Cannot be null.
         *
         * @param value The shard region.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setShardRegion(final ActorRef value) {
            _shardRegion = value;
            return this;
        }

        /**
         * The name of the host. Required. Cannot be null.
         *
         * @param value The host.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setHost(final String value) {
            _host = value;
            return this;
        }

        @NotNull
        private Set<String> _currentShards;
        @NotNull
        private Set<String> _incomingShards;
        @NotNull
        private Set<String> _outgoingShards;
        @NotNull
        private String _host;
        @NotNull
        private ActorRef _shardRegion;
    }

    private static class CountingSetSerializer extends JsonSerializer<Set<?>> {

        /**
         * Method that can be called to ask implementation to serialize values of type this serializer handles.
         *
         * @param value Value to serialize; can <b>not</b> be null.
         * @param gen Generator used to output resulting Json content
         * @param serializers Provider that can be used to get serializers for
         */
        @Override
        public void serialize(
                final Set<?> value,
                final JsonGenerator gen,
                final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("count", value.size());
            gen.writeEndObject();
        }
    }
}
