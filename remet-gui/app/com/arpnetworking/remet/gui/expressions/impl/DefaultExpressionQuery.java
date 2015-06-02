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
package com.arpnetworking.remet.gui.expressions.impl;

import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.expressions.Expression;
import com.arpnetworking.remet.gui.expressions.ExpressionQuery;
import com.arpnetworking.remet.gui.expressions.ExpressionRepository;
import com.google.common.base.MoreObjects;

import java.util.Optional;

/**
 * Default implementation of <code>ExpressionQuery</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DefaultExpressionQuery implements ExpressionQuery {

    /**
     * Public constructor.
     *
     * @param repository The <code>ExpressionRepository</code>
     */
    public DefaultExpressionQuery(final ExpressionRepository repository) {
        _repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionQuery contains(final Optional<String> contains) {
        _contains = contains;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionQuery cluster(final Optional<String> cluster) {
        _cluster = cluster;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionQuery service(final Optional<String> service) {
        _service = service;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionQuery limit(final int limit) {
        _limit = limit;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionQuery offset(final Optional<Integer> offset) {
        _offset = offset;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<Expression> execute() {
        return _repository.query(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getContains() {
        return _contains;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getCluster() {
        return _cluster;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getService() {
        return _service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLimit() {
        return _limit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Integer> getOffset() {
        return _offset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Repository", _repository)
                .add("Contains", _contains)
                .add("Cluster", _cluster)
                .add("Service", _service)
                .add("Limit", _limit)
                .add("Offset", _offset)
                .toString();
    }

    private final ExpressionRepository _repository;
    private Optional<String> _contains = Optional.empty();
    private Optional<String> _cluster = Optional.empty();
    private Optional<String> _service = Optional.empty();
    private int _limit = DEFAULT_LIMIT;
    private Optional<Integer> _offset = Optional.empty();

    private static final int DEFAULT_LIMIT = 1000;
}
