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

/**
 * Describes the state of the metrics software on a host.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public enum MetricsSoftwareState {

    /**
     * The software is not installed.
     */
    NOT_INSTALLED,

    /**
     * The software is installed but it is not the latest version.
     */
    OLD_VERSION_INSTALLED,

    /**
     * The software is installed and it is the latest version.
     */
    LATEST_VERSION_INSTALLED,

    /**
     * The state of the software has not or can not be determined.
     */
    UNKNOWN;
}
