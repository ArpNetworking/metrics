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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;
import java.util.Set;

/**
 * Generates a set of names to use.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class NameSetGenerator implements NameGenerator {
    /**
     * Public constructor.
     *
     * @param setSize Number of names in the set.
     * @param generator Generator used to create the name.
     */
    public NameSetGenerator(final int setSize, final RandomGenerator generator) {
        final Set<String> names = Sets.newHashSetWithExpectedSize(setSize);
        _dataGenerator = new RandomDataGenerator(generator);
        while (names.size() < setSize) {
            names.add(_dataGenerator.nextHexString(16));
        }
        _names = Lists.newArrayList(names);
    }

    /**
     * Public constructor.
     *
     * @param names The list of names to use.
     * @param generator Generator used to create the name.
     */
    public NameSetGenerator(final Set<String> names, final RandomGenerator generator) {
        _dataGenerator = new RandomDataGenerator(generator);
        _names = Lists.newArrayList(names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return _names.get(_dataGenerator.nextInt(0, _names.size() - 1));
    }

    private final List<String> _names;
    private final RandomDataGenerator _dataGenerator;
}
