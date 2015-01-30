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

package global;

import akka.cluster.Cluster;
import com.arpnetworking.jackson.ObjectMapperFactory;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import play.libs.Json;

/**
 * Setup the global application components.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class Global extends GlobalSettings {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final Application app) {
        Logger.info("Starting application...");

        // Configure Json serialization
        Json.setObjectMapper(ObjectMapperFactory.getInstance());

        // Start-up parent
        super.onStart(app);

        Logger.debug("Startup complete");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final Application app) {
        Logger.info("Shutting down application...");

        final Cluster cluster = Cluster.get(Akka.system());
        cluster.leave(cluster.selfAddress());
        // Give the message 3 seconds to propagate through the fleet
        try {
            Thread.sleep(3000);
        } catch (final InterruptedException ignored) {
            // Clear the interrupted status
            Thread.interrupted();
        }

        // Shutdown
        super.onStop(app);

        Logger.debug("Shutdown complete");
    }
}
