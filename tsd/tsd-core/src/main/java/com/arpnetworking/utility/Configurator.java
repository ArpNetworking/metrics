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
package com.arpnetworking.utility;

import com.arpnetworking.configuration.Configuration;
import com.arpnetworking.configuration.Listener;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

/**
 * Manages configuration and reconfiguration of a <code>Launchable</code> instance
 * using a POJO representation of its configuration. The <code>Launchable</code>
 * is instantiated with each new configuration. The configuration must validate
 * on construction and throw an exception if the configuration is invalid.
 *
 * @param <T> The <code>Launchable</code> type to configure.
 * @param <S> The type representing the validated configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class Configurator<T extends Launchable, S> implements Listener, Launchable {

    /**
     * Public constructor.
     *
     * @param factory The factory to create a launchable.
     * @param configurationClass The configuration class.
     */
    public Configurator(final ConfiguredLaunchableFactory<T, S> factory, final Class<? extends S> configurationClass) {
        _factory = factory;
        _configurationClass = configurationClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void offerConfiguration(final Configuration configuration) throws Exception {
        _offeredConfiguration = configuration.getAs(_configurationClass);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void applyConfiguration() {
        // Shutdown
        shutdown();

        // Swap configurations
        _configuration = _offeredConfiguration;
        _launchable = Optional.of(_factory.create(_configuration.get()));

        // (Re)launch
        launch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void launch() {
        if (_launchable.isPresent()) {
            _launchable.get().launch();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        if (_launchable.isPresent()) {
            _launchable.get().shutdown();
            _launchable = Optional.absent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("ConfigurationClass", _configurationClass)
                .add("Launchable", _launchable)
                .toString();
    }

    /* package private */ Optional<T> getLaunchable() {
        return _launchable;
    }

    /* package private */ Optional<S> getConfiguration() {
        return _configuration;
    }

    /* package private */ Optional<S> getOfferedConfiguration() {
        return _offeredConfiguration;
    }

    private final ConfiguredLaunchableFactory<T, S> _factory;
    private final Class<? extends S> _configurationClass;

    private Optional<T> _launchable = Optional.absent();
    private Optional<S> _configuration = Optional.absent();
    private Optional<S> _offeredConfiguration = Optional.absent();
}