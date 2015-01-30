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
package com.arpnetworking.play.configuration;

import com.google.common.base.Throwables;
import play.Configuration;
import play.api.Play;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;

/**
 * Utility methods that provide common patterns when interacting with Play's <code>Configuration</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class ConfigurationHelper {

    /**
     * Return the value of a configuration key as a <code>File</code> instance.
     *
     * @param configuration Play <code>Configuration</code> instance.
     * @param key The name of the configuration key to interpret as a <code>File</code> reference.
     * @return Instance of <code>File</code> as defined by key in configuration.
     */
    public static File getFile(final Configuration configuration, final String key) {
        final String pathAsString = configuration.getString(key);
        if (!pathAsString.startsWith("/")) {
            Play.current().getFile(pathAsString);
        }
        return new File(pathAsString);
    }

    /**
     * Return the value of a configuration key as a <code>FiniteDuration</code> instance.
     *
     * @param configuration Play <code>Configuration</code> instance.
     * @param key The name of the configuration key to interpret as a <code>FiniteDuration</code> reference.
     * @return Instance of <code>FiniteDuration</code> as defined by key in configuration.
     */
    public static FiniteDuration getFiniteDuration(final Configuration configuration, final String key) {
        final Duration duration = Duration.create(configuration.getString(key));
        return new FiniteDuration(
                duration.length(),
                duration.unit());
    }

    /**
     * Return the value of a configuration key as a <code>Class</code> instance.
     *
     * @param configuration Play <code>Configuration</code> instance.
     * @param key The name of the configuration key to interpret as a <code>Class</code> reference.
     * @param <T> The type parameter for the <code>Class</code> instance to return.
     * @return Instance of <code>Class</code> as defined by key in configuration.
     */
    public static <T> Class<? extends T> getType(final Configuration configuration, final String key) {
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends T> clazz = (Class<? extends T>) Class.forName(configuration.getString(key));
            return clazz;
        } catch (final ClassNotFoundException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    private ConfigurationHelper() {}
}
