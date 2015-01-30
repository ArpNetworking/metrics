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

package com.arpnetworking.tsdaggregator.perf;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.google.common.base.Function;

import java.util.Collection;

/**
 * Test helper to provide a callback for a sink.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ListeningSink implements Sink {
    /**
     * Public constructor.
     *
     * @param callback The callback function to execute.
     */
    public ListeningSink(final Function<Collection<AggregatedData>, Void> callback) {
        _callback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data) {
        if (_callback != null) {
            _callback.apply(data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordAggregateData(final Collection<AggregatedData> data, final Collection<Condition> conditions) {
        recordAggregateData(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }

    private final Function<Collection<AggregatedData>, Void> _callback;
}
