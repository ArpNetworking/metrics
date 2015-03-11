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
package com.arpnetworking.tsdcore.scripting;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Collection;
import java.util.Set;

/**
 * Interface for classes providing expression evaluation capabilities.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface Expression {

    /**
     * Evaluate the expression in the context of the <code>List</code> of
     * <code>AggregatedData</code>. If the data required to evaluate the
     * expression is not present an <code>Optional.absent</code> value is
     * returned. If evaluation fails for any other reason an exception is
     * thrown.
     *
     * <b>TO BE REMOVED!</b>
     *
     * @param host The target host of the evaluation.
     * @param period The target period of the evaluation.
     * @param start The target period start for the expression.
     * @param data The data input for the expression.
     * @return The resulting <code>AggregatedData</code> or <code>Optional.absent</code>.
     * @throws ScriptingException if evaluation fails for any reason.
     */
    Optional<AggregatedData> evaluate(
            final String host,
            final Period period,
            final DateTime start,
            final Collection<AggregatedData> data) throws ScriptingException;

    /**
     * Evaluate the expression in the context of the specified periodic
     * aggregated data. If the data required to evaluate the expression is not
     * present an <code>Optional.absent</code> value is returned. If evaluation
     * fails for any other reason an exception is thrown.
     *
     * @param periodicData The data input for the expression.
     * @return The resulting <code>AggregatedData</code> or <code>Optional.absent</code>.
     * @throws ScriptingException if evaluation fails for any reason.
     */
    Optional<AggregatedData> evaluate(final PeriodicData periodicData) throws ScriptingException;

    /**
     * Accessor for instance of <code>FQDSN</code> representing the data
     * produced by this <code>Expression</code>.
     *
     * @return Instance of <code>FQDSN</code> representing the data produced by
     * this <code>Expression</code>.
     */
    FQDSN getTargetFQDSN();

    /**
     * Retrieve the set of dependencies expressed as fully qualified data
     * space names (FQDSNs).
     *
     * @return <code>Set</code> of <code>FQDSN</code> instances.
     */
    Set<FQDSN> getDependencies();
}
