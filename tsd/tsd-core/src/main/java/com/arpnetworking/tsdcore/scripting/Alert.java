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

import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.PeriodicData;

/**
 * Interface for classes providing alert condition evaluation capabilities. The
 * alert is considered "in alarm" or "triggered" when the evaluation returns
 * <code>true</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface Alert {

    /**
     * Evaluate the alert in the context of the specified periodic aggregated
     * data. If the data required to evaluate the alert is not present an
     * <code>Optional.absent</code> value is returned. If evaluation fails for
     * any other reason an exception is thrown.
     *
     * @param periodicData The data input for the expression.
     * @return Evaluated <code>Condition</code> or <code>Optional.absent</code>.
     * @throws ScriptingException if evaluation fails for any reason.
     */
    Condition evaluate(final PeriodicData periodicData) throws ScriptingException;
}
