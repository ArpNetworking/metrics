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

import com.arpnetworking.logback.annotations.Loggable;
import com.google.common.base.MoreObjects;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

/**
 * Model class containing metadata about paginated results.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@Loggable
public class Pagination {

    /**
     * Public constructor.
     *
     * @param path The base path for the query that produced the results.
     * @param total The total number of matching records available.
     * @param size The number of records returned in this page.
     * @param limit The maximum number of records to return in one page.
     * @param offset The offset, in records, of the first record in this page.
     * @param conditions The <code>Map</code> of query parameter key-value pairs.
     */
    public Pagination(
            final String path,
            final long total,
            final int size,
            final int limit,
            final Optional<Integer> offset,
            final Map<String, String> conditions) {
        _total = total;
        _size = size;
        _offset = offset.isPresent() ? offset.get() : 0;

        Optional<URI> previous = Optional.empty();
        Optional<URI> next = Optional.empty();
        if (_offset + _size < _total) {
            final int newOffset = _offset + _size;
            final int newLimit = limit;
            next = Optional.of(createReference(path, newLimit, newOffset, conditions));
        }
        if (_offset > 0) {
            final int newOffset = Math.max(_offset - limit, 0);
            final int newLimit = Math.min(_offset - newOffset, limit);
            previous = Optional.of(createReference(path, newLimit, newOffset, conditions));
        }
        _next = next;
        _previous = previous;
    }

    public long getTotal() {
        return _total;
    }

    public int getSize() {
        return _size;
    }

    public int getOffset() {
        return _offset;
    }

    public Optional<URI> getNext() {
        return _next;
    }

    public Optional<URI> getPrevious() {
        return _previous;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("class", this.getClass())
                .add("Total", _total)
                .add("Size", _size)
                .add("Offset", _offset)
                .add("Next", _next)
                .add("Previous", _previous)
                .toString();
    }

    private URI createReference(
            final String path,
            final int limit,
            final int offset,
            final Map<String, String> conditions) {
        final URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setPath(path);
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            uriBuilder.addParameter(entry.getKey(), entry.getValue());
        }
        uriBuilder.addParameter("limit", String.valueOf(limit));
        uriBuilder.addParameter("offset", String.valueOf(offset));
        try {
            return uriBuilder.build();
        } catch (final URISyntaxException e) {
            throw new RuntimeException("Failed building uri", e);
        }
    }

    private final long _total;
    private final int _size;
    private final int _offset;
    private final Optional<URI> _next;
    private final Optional<URI> _previous;
}
