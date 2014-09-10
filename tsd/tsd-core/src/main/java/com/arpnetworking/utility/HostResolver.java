/**
 * Copyright 2014 Brandon Arp
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

import java.net.UnknownHostException;

/**
 * Used to get the local hostname.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public interface HostResolver {

    /**
     * Provides the local host name.
     *
     * @return The local host name.
     * @throws UnknownHostException If the local host name cannot be resolved.
     */
    String getLocalHostName() throws UnknownHostException;
}
