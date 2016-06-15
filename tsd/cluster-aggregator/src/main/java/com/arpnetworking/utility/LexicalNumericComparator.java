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
package com.arpnetworking.utility;

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares strings lexically and numerically by splitting on digits and using the full number for numerical comparison.
 * NOTE: does not support negatives or decimal places
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LexicalNumericComparator implements Comparator<String>, Serializable {
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final String o1, final String o2) {
        final Matcher o1Matcher = MATCH_PATTERN.matcher(o1);
        final Matcher o2Matcher = MATCH_PATTERN.matcher(o2);


        boolean o1Found = o1Matcher.find();
        boolean o2Found = o2Matcher.find();
        while (o1Found && o2Found) {
            final String first = o1Matcher.group();
            final String second = o2Matcher.group();
            final int val;
            if (Character.isDigit(first.charAt(0)) && Character.isDigit(second.charAt(0))) {
                val = Long.valueOf(first).compareTo(Long.valueOf(second));
            } else {
                val = first.compareTo(second);
            }
            if (val != 0) {
                return val;
            }

            o1Found = o1Matcher.find();
            o2Found = o2Matcher.find();
        }

        // If o1Found, then o1 is longer and therefore "bigger"
        return o1Found ? 1 : -1;
    }

    private static final Pattern MATCH_PATTERN = Pattern.compile("\\d+|\\D+");
    private static final long serialVersionUID = 1L;
}
