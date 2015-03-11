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

package com.arpnetworking.clusteraggregator.configuration;

import com.arpnetworking.configuration.jackson.DynamicConfigurationFactory;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector;
import com.fasterxml.jackson.module.guice.GuiceInjectableValues;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Representation of an emitter configuration for cluster aggregator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class EmitterConfiguration {
    /**
     * Create an <code>ObjectMapper</code> for Emitter configuration.
     *
     * @param injector The Guice <code>Injector</code> instance.
     * @return An <code>ObjectMapper</code> for Pipeline configuration.
     */
    public static ObjectMapper createObjectMapper(final Injector injector) {
        final ObjectMapper objectMapper = ObjectMapperFactory.createInstance();

        final SimpleModule module = new SimpleModule("Emitter");
        BuilderDeserializer.addTo(module, EmitterConfiguration.class);

        final Set<Class<? extends Sink>> sinkClasses = INTERFACE_DATABASE.findClassesWithInterface(Sink.class);
        for (final Class<? extends Sink> sinkClass : sinkClasses) {
            BuilderDeserializer.addTo(module, sinkClass);
        }

        final Set<Class<? extends DynamicConfigurationFactory>> dcFactoryClasses =
                INTERFACE_DATABASE.findClassesWithInterface(DynamicConfigurationFactory.class);
        for (final Class<? extends DynamicConfigurationFactory> dcFactoryClass : dcFactoryClasses) {
            BuilderDeserializer.addTo(module, dcFactoryClass);
        }

        objectMapper.registerModules(module);

        final GuiceAnnotationIntrospector guiceIntrospector = new GuiceAnnotationIntrospector();
        objectMapper.setInjectableValues(new GuiceInjectableValues(injector));
        objectMapper.setAnnotationIntrospectors(
                new AnnotationIntrospectorPair(
                        guiceIntrospector, objectMapper.getSerializationConfig().getAnnotationIntrospector()),
                new AnnotationIntrospectorPair(
                        guiceIntrospector, objectMapper.getDeserializationConfig().getAnnotationIntrospector()));

        return objectMapper;
    }

    public List<Sink> getSinks() {
        return Collections.unmodifiableList(_sinks);
    }

    private EmitterConfiguration(final Builder builder) {
        _sinks = Lists.newArrayList(builder._sinks);
    }

    private final List<Sink> _sinks;

    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();

    /**
     * Implementation of builder pattern for {@link EmitterConfiguration}.
     *
     * @author Brandon Arp (barp at groupon dot com)
     */
    public static class Builder extends OvalBuilder<EmitterConfiguration> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(EmitterConfiguration.class);
        }

        /**
         * The sinks.
         * Required. Cannot be null. Cannot be empty.
         *
         * @param value The sinks.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setSinks(final List<Sink> value) {
            _sinks = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private List<Sink> _sinks;
    }
}
