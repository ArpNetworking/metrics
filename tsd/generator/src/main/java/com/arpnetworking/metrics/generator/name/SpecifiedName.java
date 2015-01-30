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

/**
 * Generator that returns the name provided on construction.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class SpecifiedName implements NameGenerator {
    /**
     * Public constructor.
     *
     * @param name Name of the metric to generate.
     */
    public SpecifiedName(final String name) {
        _name = name;
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
