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
package com.arpnetworking.remet.gui.hosts;

import java.util.List;

/**
 * Interface describing the result of query to the <code>HostRepository</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface HostQueryResult {

    /**
     * The matching hosts.
     *
     * @return The matching hosts.
     */
    List<Host> hosts();

    /**
     * The total number of matching hosts. This may be greater than the number of hosts returned.
     *
     * @return The total number of matching hosts.
     */
    long total();
}
