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

package com.arpnetworking.metrics.generator.name;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Generates a single metric name and reuses it.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SingleNameGenerator implements NameGenerator {
    /**
     * Public constructor.
     *
     * @param generator Generator used to create the name.
     */
    public SingleNameGenerator(final RandomGenerator generator) {
        _name = new RandomDataGenerator(generator).nextHexString(16);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return _name;
    }

    private final String _name;
}
