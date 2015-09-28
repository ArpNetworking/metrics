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
package com.arpnetworking.remet.gui.hosts.impl;

import akka.actor.UntypedActor;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.play.configuration.ConfigurationHelper;
import com.arpnetworking.remet.gui.hosts.Host;
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.inject.Inject;
import org.joda.time.Duration;
import play.Configuration;

/**
 * This is an actor that finds "random" hosts. The primary purpose of this
 * class is to for development and testing. This is <b>not</b> intended for
 * production usage.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class RandomHostProvider extends UntypedActor {

    /**
     * Public constructor.
     *
     * @param hostRepository The <code>HostRepository</code> instance.
     * @param configuration Play configuration.
     */
    @Inject
    public RandomHostProvider(final HostRepository hostRepository, final Configuration configuration) {
        _hostRepository = hostRepository;
        getContext().system().scheduler().schedule(
                ConfigurationHelper.getFiniteDuration(configuration, "hostProvider.initialDelay"),
                ConfigurationHelper.getFiniteDuration(configuration, "hostProvider.interval"),
                getSelf(),
                "tick",
                getContext().dispatcher(),
                getSelf());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if ("tick".equals(message)) {
            LOGGER.trace()
                    .setMessage("Searching for added/updated/deleted hosts")
                    .addData("actor", self())
                    .log();

            if (System.currentTimeMillis() - _lastTime > INTERVAL.getMillis()) {
                final Host newHost = new DefaultHost.Builder()
                        .setHostname("test-app" + _hostAdd + ".example.com")
                        .setMetricsSoftwareState(MetricsSoftwareState.NOT_INSTALLED)
                        .setCluster("cluster" + (_hostAdd / 10 + 1))
                        .build();
                LOGGER.debug()
                        .setMessage("Found a new host")
                        .addData("actor", self())
                        .addData("hostname", newHost.getHostname())
                        .log();
                _hostRepository.addOrUpdateHost(newHost);
                if (_hostUpdateOne > 0) {
                    final Host updatedHost = new DefaultHost.Builder()
                            .setHostname("test-app" + _hostUpdateOne + ".example.com")
                            .setMetricsSoftwareState(MetricsSoftwareState.OLD_VERSION_INSTALLED)
                            .setCluster("cluster" + (_hostUpdateOne / 10 + 1))
                            .build();
                    LOGGER.debug()
                            .setMessage("Found an updated host")
                            .addData("actor", self())
                            .addData("hostname", updatedHost.getHostname())
                            .log();
                    _hostRepository.addOrUpdateHost(updatedHost);
                }
                if (_hostUpdateTwo > 0) {
                    final Host updatedHost = new DefaultHost.Builder()
                            .setHostname("test-app" + _hostUpdateTwo + ".example.com")
                            .setCluster("cluster" + (_hostUpdateTwo / 10 + 1))
                            .setMetricsSoftwareState(MetricsSoftwareState.LATEST_VERSION_INSTALLED)
                            .build();
                    LOGGER.debug()
                            .setMessage("Found an updated host")
                            .addData("actor", self())
                            .addData("hostname", updatedHost.getHostname())
                            .log();
                    _hostRepository.addOrUpdateHost(updatedHost);
                }
                if (_hostRemove > 0) {
                    final String deletedHostName = "test-app" + _hostRemove + ".example..com";
                    LOGGER.debug()
                            .setMessage("Found host to delete")
                            .addData("actor", self())
                            .addData("hostname", deletedHostName)
                            .log();
                    _hostRepository.deleteHost(deletedHostName);
                }
                ++_hostAdd;
                ++_hostUpdateOne;
                ++_hostUpdateTwo;
                ++_hostRemove;
                _lastTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("hostRepository", _hostRepository)
                .put("lastTime", _lastTime)
                .put("hostAdd", _hostAdd)
                .put("hostUpdateOne", _hostUpdateOne)
                .put("hostUpdateTwo", _hostUpdateTwo)
                .put("hostRemove", _hostRemove)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private final HostRepository _hostRepository;
    private long _lastTime = 0;
    private long _hostAdd = 1;
    private long _hostUpdateOne = -5;
    private long _hostUpdateTwo = -10;
    private long _hostRemove = -15;

    private static final Duration INTERVAL = Duration.standardSeconds(10);
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomHostProvider.class);
}
