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
package com.arpnetworking.metrics.jvm.collectors;

import java.util.Locale;

/**
 * An utility class of JVM metrics.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
/* package private */ final class MetricsUtil {

    /**
     * Converts blank spaced strings to camel case with upper case.
     *
     * @param string An instance of <code>String</code> to be converted.
     * @return A camel cased version of the given string.
     */
    /* package private */ static String convertToSnakeCase(final String string) {
        final StringBuilder result = new StringBuilder();
        final String[] parts = string.split(" ");
        for (final String part : parts) {
            result.append(part.substring(0, 1).toLowerCase(Locale.getDefault()))
                    .append(part.substring(1));
            result.append("_");
        }
        final String resultString = result.toString();
        return resultString.substring(0, resultString.length() - 1);
    }

    private MetricsUtil() {}
}
