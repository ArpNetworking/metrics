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
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;

/**
 * Tests for the {@link TimeThresholdSink}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class TimeThresholdSinkTest {
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void doesNotDropFreshData() {
        final TimeThresholdSink periodFilteringSink = new TimeThresholdSink.Builder()
                .setName("testKeepFresh")
                .setThreshold(Period.minutes(10))
                .setSink(_sink)
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .setStart(DateTime.now())
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(data, Collections.<Condition>emptyList());
    }

    @Test
    public void dropsOldDataByDefault() {
        final TimeThresholdSink periodFilteringSink = new TimeThresholdSink.Builder()
                .setName("testDropOld")
                .setThreshold(Period.minutes(10))
                .setSink(_sink)
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .setStart(DateTime.now().minus(Period.minutes(30)))
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(Collections.emptyList(), Collections.<Condition>emptyList());
    }

    @Test
    public void doesNotDropOldDataWhenLogOnly() {
        final TimeThresholdSink periodFilteringSink = new TimeThresholdSink.Builder()
                .setName("testKeepLogOnly")
                .setThreshold(Period.minutes(10))
                .setLogOnly(true)
                .setSink(_sink)
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(
                TestBeanFactory.createAggregatedDataBuilder()
                        .setPeriod(Period.minutes(1))
                        .setStart(DateTime.now().minus(Period.minutes(30)))
                        .build());
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(data, Collections.<Condition>emptyList());
    }

    @Test
    public void doesNotDropDataForExcludedServices() {
        final TimeThresholdSink periodFilteringSink = new TimeThresholdSink.Builder()
                .setName("testKeepsExcludedServices")
                .setThreshold(Period.minutes(10))
                .setExcludedServices(Collections.singleton("excluded"))
                .setSink(_sink)
                .build();
        final AggregatedData out = TestBeanFactory.createAggregatedDataBuilder()
                .setPeriod(Period.minutes(1))
                .setStart(DateTime.now().minus(Period.minutes(30)))
                .build();
        final AggregatedData in = TestBeanFactory.createAggregatedDataBuilder()
                .setPeriod(Period.minutes(1))
                .setStart(DateTime.now().minus(Period.minutes(30)))
                .setFQDSN(TestBeanFactory.createFQDSNBuilder().setService("excluded").build())
                .build();
        final Collection<AggregatedData> data = Lists.newArrayList(in, out);
        periodFilteringSink.recordAggregateData(data, Collections.<Condition>emptyList());
        Mockito.verify(_sink).recordAggregateData(Collections.singletonList(in), Collections.<Condition>emptyList());
    }

    @Mock
    private Sink _sink;
}
