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
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.inject.Inject;

/**
 * This is a placeholder actor that does not actually find any hosts.
 * The primary purpose of this class is to demonstrate how to implement a
 * host provider.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class NoHostProvider extends UntypedActor {

    /**
     * Public constructor.
     */
    @Inject
    public NoHostProvider() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if ("tick".equals(message)) {
            LOGGER.trace()
                    .setMessage("Searching for added/updated/deleted hosts")
                    .addData("actor", self().toString())
                    .log();
            LOGGER.debug().setMessage("No hosts found!").log();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NoHostProvider.class);
}
