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
package com.arpnetworking.remet.gui.impl;

import com.arpnetworking.remet.gui.QueryResult;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Default implementation of <code>HostQueryResult</code>.
 *
 * @param <T> The result value type.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DefaultQueryResult<T> implements QueryResult<T> {

    /**
     * Public constructor.
     *
     * @param values The <code>List</code> of <code>Host</code> instances.
     * @param total The total number of matching <code>Host</code> instances.
     */
    public DefaultQueryResult(final List<? extends T> values, final long total) {
        _values = values;
        _total = total;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends T> values() {
        return _values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long total() {
        return _total;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Values", _values)
                .add("Total", _total)
                .toString();
    }

    private final List<? extends T> _values;
    private final long _total;
}
