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

package com.arpnetworking.metrics.generator.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A default ordering for a <code>WorkEntry</code>.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
class WorkItemOrdering implements Comparator<WorkEntry>, Serializable {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final WorkEntry o1, final WorkEntry o2) {
        return Long.valueOf(o1.getCurrentValue()).compareTo(o2.getCurrentValue());
    }

    private static final long serialVersionUID = -7812605920313005784L;
}
