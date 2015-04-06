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

import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Tests for the LexicalNumericComparator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LexicalNumericComparatorTest {
    @Test
    public void testNoNumeric() {
        final ArrayList<String> strings = Lists.newArrayList("this", "that");
        final ArrayList<String> expected = Lists.newArrayList("that", "this");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testNumeric() {
        final ArrayList<String> strings = Lists.newArrayList("128", "20");
        final ArrayList<String> expected = Lists.newArrayList("20", "128");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testAlphaNumeric() {
        final ArrayList<String> strings = Lists.newArrayList("test 128", "test 20");
        final ArrayList<String> expected = Lists.newArrayList("test 20", "test 128");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testAlphaNumericUnderscore() {
        final ArrayList<String> strings = Lists.newArrayList("test_100", "test_20");
        final ArrayList<String> expected = Lists.newArrayList("test_20", "test_100");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testAfterNumeric() {
        final ArrayList<String> strings = Lists.newArrayList("test 020 aab", "test 20 aaa");
        final ArrayList<String> expected = Lists.newArrayList("test 20 aaa", "test 020 aab");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testAfterNumericUnderscore() {
        final ArrayList<String> strings = Lists.newArrayList("test_020_aab", "test_20_aaa");
        final ArrayList<String> expected = Lists.newArrayList("test_20_aaa", "test_020_aab");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testShorterFirst() {
        final ArrayList<String> strings = Lists.newArrayList("test 20 aaa more", "test 20 aaa");
        final ArrayList<String> expected = Lists.newArrayList("test 20 aaa", "test 20 aaa more");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }

    @Test
    public void testShorterFirstDontSwap() {
        final ArrayList<String> strings = Lists.newArrayList("test 20 aaa", "test 20 aaa more");
        final ArrayList<String> expected = Lists.newArrayList("test 20 aaa", "test 20 aaa more");
        Collections.sort(strings, new LexicalNumericComparator());
        Assert.assertThat(strings, Matchers.contains(expected.toArray()));
        Assert.assertThat(strings, Matchers.containsInRelativeOrder(expected.toArray()));
    }
}
