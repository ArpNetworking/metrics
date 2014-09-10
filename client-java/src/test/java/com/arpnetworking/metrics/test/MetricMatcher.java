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
package com.arpnetworking.metrics.test;

import com.arpnetworking.metrics.Quantity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of <code>Matcher</code> which matches a map of metrics
 * using the <code>QuantityMatcher</code> for each sample.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class MetricMatcher extends TypeSafeDiagnosingMatcher<Map<? extends String, ? extends List<Quantity>>> {

    /**
     * Create a new matcher for the expected metrics.
     * 
     * @param arguments Array of variable length tuples where each tuple begins
     * with a <code>String</code> for the metric name followed by zero or more
     * samples as <code>QuantityMatcher</code> instances.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Map<? extends String, ? extends List<Quantity>>> match(final Object... arguments) {
        return new MetricMatcher(arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(final Description description) {
        description.appendText(" was ")
                .appendValue(_expected);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean matchesSafely(
            final Map<? extends String, ? extends List<Quantity>> item,
            final Description mismatchDescription) {

        boolean matches = true;
        final Set<String> difference = setDifference(_expected.keySet(), item.keySet());
        if (!difference.isEmpty()) {
            mismatchDescription.appendText(String.format("Key sets differ: %s", difference));
            matches = false;
        }

        for (final Map.Entry<? extends String, List<Matcher<Quantity>>> entry : _expected.entrySet()) {
            final List<Matcher<Quantity>> expectedSamples = entry.getValue();
            final List<Quantity> actualSamples = item.get(entry.getKey());
            if (actualSamples != null) {
                if (expectedSamples.size() != actualSamples.size()) {
                    mismatchDescription.appendText(String.format(
                            "Sample sets differ in size: key=%s, expected=%d, actual=%d",
                            entry.getKey(),
                            Integer.valueOf(expectedSamples.size()),
                            Integer.valueOf(actualSamples.size())));
                    matches = false;
                } else {
                    final Iterator<Matcher<Quantity>> expectedIterator = expectedSamples.iterator();
                    final Iterator<? extends Quantity> actualIterator = actualSamples.iterator();
                    while (expectedIterator.hasNext()) {
                        assert actualIterator.hasNext() : "iterator mismatch";
                        final Matcher<Quantity> sampleMatcher = expectedIterator.next();
                        final Quantity actualSample = actualIterator.next();

                        if (!sampleMatcher.matches(actualSample)) {
                            mismatchDescription.appendText(String.format(
                                    "Samples differ: key=%s",
                                    entry.getKey()));
                            sampleMatcher.describeMismatch(actualSample, mismatchDescription);
                            matches = false;
                        }
                    }
                }
            }
        }

        return matches;
    }

    @SuppressWarnings("unchecked")
    private MetricMatcher(final Object... arguments) {
        List<Matcher<Quantity>> samples = null;
        for (final Object argument : arguments) {
            if (argument instanceof String) {
                assert !_expected.containsKey(argument) : "duplicate metric key";
                samples = new ArrayList<>();
                _expected.put((String) argument, samples);
            } else if (argument instanceof Matcher) {
                samples.add((Matcher<Quantity>) argument);
            } else {
                assert false : "invalid argument type";
            }
        }
    }

    private static <T> Set<T> setDifference(final Set<? extends T> set1, final Set<? extends T> set2) {
        // CHECKSTYLE.OFF: IllegalInstantiation - No Guava
        final Set<T> difference = new HashSet<>();
        final Set<T> onlyInSet1 = new HashSet<>(set1);
        final Set<T> onlyInSet2 = new HashSet<>(set2);
        // CHECKSTYLE.ON: IllegalInstantiation

        onlyInSet1.removeAll(set2);
        onlyInSet2.removeAll(set1);
        difference.addAll(onlyInSet1);
        difference.addAll(onlyInSet2);

        return difference;
    }

    // CHECKSTYLE.OFF: IllegalInstantiation - No Guava
    private final Map<String, List<Matcher<Quantity>>> _expected = new HashMap<String, List<Matcher<Quantity>>>();
    // CHECKSTYLE.ON: IllegalInstantiation
}
