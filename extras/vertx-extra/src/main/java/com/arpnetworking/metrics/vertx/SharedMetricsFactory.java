/**
 * Copyright 2015 Groupon.com
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
package com.arpnetworking.metrics.vertx;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import org.vertx.java.core.shareddata.Shareable;

/**
 * An implementation of <code>MetricsFactory</code> that extends Vertx's <code>SharedData</code> which allows use in a
 * shared data map.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class SharedMetricsFactory implements MetricsFactory, Shareable {

    /**
     *  Constructs a new SharedMetricsFactory object that can be added to a vertx shared data map/set.
     *
     *  @param wrappedMetricsFactory - MetricsFactory object to wrap.
     */
    public SharedMetricsFactory(final MetricsFactory wrappedMetricsFactory) {
        if (wrappedMetricsFactory == null) {
            throw new IllegalArgumentException("MetricsFactory cannot be null.");
        }
        _wrappedMetricsFactory = wrappedMetricsFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics create() {
        return _wrappedMetricsFactory.create();
    }

    private final MetricsFactory _wrappedMetricsFactory;
}
