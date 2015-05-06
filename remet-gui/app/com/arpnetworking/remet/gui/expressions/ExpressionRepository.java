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
package com.arpnetworking.remet.gui.expressions;

import com.arpnetworking.remet.gui.QueryResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for repository of expressions.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface ExpressionRepository {

    /**
     * Open the <code>ExpressionRepository</code>.
     */
    void open();

    /**
     * Close the <code>ExpressionRepository</code>.
     */
    void close();

    /**
     * Get the <code>Expression</code> by identifier.
     *
     * @param identifier The <code>Expression</code> identifier.
     * @return The matching <code>Expression</code> if found or <code>Optional.empty()</code>.
     */
    Optional<Expression> get(final UUID identifier);

    /**
     * Create a query against the expressions repository.
     *
     * @return Instance of <code>ExpressionQuery</code>.
     */
    ExpressionQuery createQuery();

    /**
     * Query expressions.
     *
     * @param query Instance of <code>ExpressionQuery</code>.
     * @return The <code>Collection</code> of all expressions.
     */
    QueryResult<Expression> query(ExpressionQuery query);

    /**
     * Retrieve the total number of expressions in the repository.
     *
     * @return The total number of expressions.
     */
    long getExpressionCount();
}
