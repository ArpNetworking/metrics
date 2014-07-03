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
import com.arpnetworking.metrics.Unit;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.IsNull;

/**
 * Implementation of <code>Matcher</code> which matches a <code>Quantity</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class QuantityMatcher extends TypeSafeDiagnosingMatcher<Quantity> {

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expected The expected <code>Quantity</code>.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final Quantity expected) {
        return match(expected.getValue(), expected.getUnit());
    }

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expectedValue The expected value.
     * @param expectedUnit The expected unit.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final Number expectedValue, final Unit expectedUnit) {
        if (expectedValue instanceof Double) {
            return new QuantityMatcher(
                    Matchers.closeTo(expectedValue.doubleValue(), 0.001),
                    Matchers.<Unit>equalTo(expectedUnit));
        }
        return new QuantityMatcher(
                Matchers.<Number>equalTo(expectedValue),
                Matchers.<Unit>equalTo(expectedUnit));
    }

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expectedValue The expected value.
     * @param expectedUnit The expected unit.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final long expectedValue, final Unit expectedUnit) {
        return match(Long.valueOf(expectedValue), expectedUnit);
    }

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expectedValue The expected value.
     * @param expectedUnit The expected unit.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final double expectedValue, final Unit expectedUnit) {
        return match(Double.valueOf(expectedValue), expectedUnit);
    }

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expectedValue The expected value.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final Number expectedValue) {
        if (expectedValue instanceof Double) {
            return match(Matchers.closeTo(expectedValue.doubleValue(), 0.001));
        }
        return match(Matchers.<Number>equalTo(expectedValue));
    }

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expectedValue The expected value.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final long expectedValue) {
        return match(Long.valueOf(expectedValue));
    }

    /**
     * Create a new matcher for the expected <code>Quantity</code>.
     * 
     * @param expectedValue The expected value.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final double expectedValue) {
        return match(Double.valueOf(expectedValue));
    }

    /**
     * Create a new matcher for a <code>Quantity</code> with a matcher for the
     * value and unit.
     * 
     * @param valueMatcher The expected value matcher.
     * @param unitMatcher The expected unit matcher.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final Matcher<? extends Number> valueMatcher, final Matcher<Unit> unitMatcher) {
        return new QuantityMatcher(valueMatcher, unitMatcher);
    }

    /**
     * Create a new matcher for a <code>Quantity</code> with a matcher for the
     * value.
     * 
     * @param valueMatcher The expected value matcher.
     * @param expectedUnit The expected unit.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final Matcher<? extends Number> valueMatcher, final Unit expectedUnit) {
        return match(valueMatcher, Matchers.equalTo(expectedUnit));
    }

    /**
     * Create a new matcher for a <code>Quantity</code> with a matcher for the
     * value.
     * 
     * @param valueMatcher The expected value matcher.
     * @return new matcher for the expected metrics.
     */
    public static Matcher<Quantity> match(final Matcher<? extends Number> valueMatcher) {
        return match(valueMatcher, IsNull.nullValue(Unit.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(final Description description) {
        description.appendText(" was ")
                .appendValue(_valueMatcher)
                .appendText(" in ")
                .appendValue(_unitMatcher);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean matchesSafely(
            final Quantity item,
            final Description mismatchDescription) {
        boolean matches = true;
        if (!_valueMatcher.matches(item.getValue())) {
            mismatchDescription.appendText(String.format(
                    "value differs: expected=%s, actual=%s",
                    _valueMatcher,
                    item.getValue()));
            matches = false;
        }
        if (!_valueMatcher.matches(item.getValue())) {
            mismatchDescription.appendText(String.format(
                    "unit differs: expected=%s, actual=%s",
                    _unitMatcher,
                    item.getUnit()));
            matches = false;
        }
        return matches;
    }

    private QuantityMatcher(final Matcher<? extends Number> valueMatcher, final Matcher<Unit> unitMatcher) {
        _valueMatcher = valueMatcher;
        _unitMatcher = unitMatcher;
    }

    private final Matcher<? extends Number> _valueMatcher;
    private final Matcher<Unit> _unitMatcher;
}
