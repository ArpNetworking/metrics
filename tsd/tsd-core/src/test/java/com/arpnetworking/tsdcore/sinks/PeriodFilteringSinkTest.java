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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.collect.Lists;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for the <code>PeriodFilteringSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class PeriodFilteringSinkTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDefaultInclude() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testDefaultInclude")
                .setSink(_sink)
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(data, Collections.<Condition>emptyList());
    }

    @Test
    public void testExclude() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testExclude")
                .setSink(_sink)
                .setExclude(Collections.singleton(Period.minutes(5)))
                .build();
        final Collection<AggregatedData> excludedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(5))
                        .build());
        final Collection<AggregatedData> includedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .build());
        periodFilteringSink.recordAggregateData(
                Stream.concat(excludedData.stream(), includedData.stream())
                        .collect(Collectors.toList()),
                Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(includedData, Collections.<Condition>emptyList());
    }

    @Test
    public void testAllExclude() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testAllExclude")
                .setSink(_sink)
                .setExclude(Collections.singleton(Period.minutes(5)))
                .build();
        final Collection<AggregatedData> excludedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(5))
                        .build());
        final Collection<AggregatedData> includedData = Lists.newArrayList();
        periodFilteringSink.recordAggregateData(
                Stream.concat(excludedData.stream(), includedData.stream())
                        .collect(Collectors.toList()),
                Collections.<Condition>emptyList());
        Mockito.verifyZeroInteractions(_sink);
    }

    @Test
    public void testExcludeLessThan() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testExcludeLessThan")
                .setSink(_sink)
                .setExcludeLessThan(Period.minutes(5))
                .build();
        final Collection<AggregatedData> excludedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .build());
        final Collection<AggregatedData> includedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(5))
                        .build());
        periodFilteringSink.recordAggregateData(
                Stream.concat(excludedData.stream(), includedData.stream())
                        .collect(Collectors.toList()),
                Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(includedData, Collections.<Condition>emptyList());
    }

    @Test
    public void testAllExcludeLessThan() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testAllExcludeLessThan")
                .setSink(_sink)
                .setExcludeLessThan(Period.minutes(5))
                .build();
        final Collection<AggregatedData> excludedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .build());
        final Collection<AggregatedData> includedData = Lists.newArrayList();
        periodFilteringSink.recordAggregateData(
                Stream.concat(excludedData.stream(), includedData.stream())
                        .collect(Collectors.toList()),
                Collections.<Condition>emptyList());
        Mockito.verifyZeroInteractions(_sink);
    }

    @Test
    public void testExcludeGreaterThan() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testExcludeGreaterThan")
                .setSink(_sink)
                .setExcludeGreaterThan(Period.minutes(5))
                .build();
        final Collection<AggregatedData> excludedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(10))
                        .build());
        final Collection<AggregatedData> includedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(5))
                        .build());
        periodFilteringSink.recordAggregateData(
                Stream.concat(excludedData.stream(), includedData.stream())
                        .collect(Collectors.toList()),
                Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(includedData, Collections.<Condition>emptyList());
    }

    @Test
    public void testAllExcludeGreaterThan() {
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testAllExcludeGreaterThan")
                .setSink(_sink)
                .setExcludeGreaterThan(Period.minutes(5))
                .build();
        final Collection<AggregatedData> excludedData = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(10))
                        .build());
        final Collection<AggregatedData> includedData = Lists.newArrayList();
        periodFilteringSink.recordAggregateData(
                Stream.concat(excludedData.stream(), includedData.stream())
                        .collect(Collectors.toList()),
                Collections.<Condition>emptyList());
        Mockito.verifyZeroInteractions(_sink);
    }

    @Test
    public void testIncludeOverExclude() {
        final Period includePeriod = Period.minutes(5);
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testIncludeOverExclude")
                .setSink(_sink)
                .setInclude(Collections.singleton(includePeriod))
                .setExclude(Collections.singleton(includePeriod))
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(includePeriod)
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(data, Collections.<Condition>emptyList());
    }

    @Test
    public void testIncludeOverLessThanExclude() {
        final Period includePeriod = Period.minutes(5);
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testIncludeOverLessThanExclude")
                .setSink(_sink)
                .setInclude(Collections.singleton(includePeriod))
                .setExcludeLessThan(Period.minutes(10))
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(includePeriod)
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(data, Collections.<Condition>emptyList());
    }

    @Test
    public void testIncludeOverGreaterThanExclude() {
        final Period includePeriod = Period.minutes(5);
        final PeriodFilteringSink periodFilteringSink = new PeriodFilteringSink.Builder()
                .setName("testIncludeOverGreaterThanExclude")
                .setSink(_sink)
                .setInclude(Collections.singleton(includePeriod))
                .setExcludeLessThan(Period.minutes(1))
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(includePeriod)
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(data, Collections.<Condition>emptyList());
    }

    @Mock
    private Sink _sink;
}
