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
package models;

import com.google.common.base.MoreObjects;

import java.util.Collections;
import java.util.List;

/**
 * Model class containing paginated data and associated metadata.
 *
 * @param <T> The result type to paginate through.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class PagedContainer<T> {

    /**
     * Public constructor.
     *
     * @param data <code>List</code> of data elements.
     * @param pagination <code>Pagination</code> metadata.
     */
    public PagedContainer(final List<T> data, final Pagination pagination) {
        _data = data;
        _pagination = pagination;
    }

    public List<T> getData() {
        return Collections.unmodifiableList(_data);
    }

    public Pagination getPagination() {
        return _pagination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Data", _data)
                .add("Pagination", _pagination)
                .toString();
    }

    private final List<T> _data;
    private final Pagination _pagination;
}
