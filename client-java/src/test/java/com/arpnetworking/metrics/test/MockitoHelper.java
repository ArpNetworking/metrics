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

import org.hamcrest.Matcher;
import org.mockito.Matchers;

/**
 * Additional helper methods for Mockito.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class MockitoHelper {

    /**
     * Provide more flexible integration with Hamcrest Matchers.
     * 
     * See:
     * https://code.google.com/p/mockito/issues/detail?id=361
     * 
     * @param <T> The type of the argument to match.
     * @param matcher The matcher to apply to the argument.
     * @return The Matcher for the argument.
     */
    @SuppressWarnings("unchecked")
    public static <T> T argThat(final Matcher<? super T> matcher) {
        return (T) Matchers.argThat(matcher);
    }

    private MockitoHelper() {}
}
