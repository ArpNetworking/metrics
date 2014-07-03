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
package com.arpnetworking.tsdaggregator.sources;

import com.arpnetworking.tsdaggregator.model.MetricType;
import com.arpnetworking.tsdaggregator.model.Record;
import com.arpnetworking.tsdaggregator.sources.MappingSource.MergingMetric;
import com.arpnetworking.tsdaggregator.test.TestBeanFactory;
import com.arpnetworking.tsdaggregator.test.UnorderedRecordEquality;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.utility.observer.Observable;
import com.arpnetworking.utility.observer.Observer;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tests for the <code>MergingSource</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class MappingSourceTest {

    @Before
    public void setUp() {
        _mockObserver = Mockito.mock(Observer.class);
        _mockSource = Mockito.mock(Source.class);
        _mergingSourceBuilder = new MappingSource.Builder()
                .setName("MergingSourceTest")
                .setFindAndReplace(ImmutableMap.of(
                        "foo/([^/]*)/bar", ImmutableList.of("foo/bar"),
                        "cat/([^/]*)/dog", ImmutableList.of("cat/dog", "cat/dog/$1")))
                .setSource(_mockSource);
    }

    @Test
    public void testAttach() {
        _mergingSourceBuilder.build();
        Mockito.verify(_mockSource).attach(Matchers.any(Observer.class));
    }

    @Test
    public void testStart() {
        _mergingSourceBuilder.build().start();
        Mockito.verify(_mockSource).start();
    }

    @Test
    public void testStop() {
        _mergingSourceBuilder.build().stop();
        Mockito.verify(_mockSource).stop();
    }

    @Test
    public void testToString() {
        final String asString = _mergingSourceBuilder.build().toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }

    @Test
    public void testMergingObserverInvalidEvent() {
        new MappingSource.MappingObserver(_mockSource, Collections.<Pattern, List<String>>emptyMap()).notify(_mockSource, "Not a Record");
        Mockito.verifyZeroInteractions(_mockSource);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergingMetricMergeMismatchedTypes() {
        final MergingMetric mergingMetric = new MergingMetric(
                TestBeanFactory.createMetricBuilder()
                        .setType(MetricType.COUNTER)
                        .build());
        mergingMetric.merge(TestBeanFactory.createMetricBuilder()
                .setType(MetricType.GAUGE)
                .build());
    }

    @Test
    public void testMergeNotMatch() {
        final Record nonMatchingRecord = TestBeanFactory.createRecordBuilder()
                .setMetrics(ImmutableMap.of(
                        "does_not_match",
                        TestBeanFactory.createMetric()))
                .build();

        final Source mergingSource = _mergingSourceBuilder.build();
        mergingSource.attach(_mockObserver);
        notify(_mockSource, nonMatchingRecord);

        final ArgumentCaptor<Record> argument = ArgumentCaptor.forClass(Record.class);
        Mockito.verify(_mockObserver).notify(Matchers.same(mergingSource), argument.capture());
        final Record actualRecord = argument.getValue();

        Assert.assertTrue(
                String.format("expected=%s, actual=%s", nonMatchingRecord, actualRecord),
                UnorderedRecordEquality.equals(nonMatchingRecord, actualRecord));
    }

    @Test
    public void testMergeTwoGauges() {
        final Record matchingRecord = TestBeanFactory.createRecordBuilder()
                .setAnnotations(Collections.singletonMap("key", "value"))
                .setMetrics(ImmutableMap.of(
                        "foo/1/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(Collections.singletonList(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE))))
                                .build(),
                        "foo/2/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(Collections.singletonList(
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build()))
                .build();

        final Source mergingSource = _mergingSourceBuilder.build();
        mergingSource.attach(_mockObserver);
        notify(_mockSource, matchingRecord);

        final ArgumentCaptor<Record> argument = ArgumentCaptor.forClass(Record.class);
        Mockito.verify(_mockObserver).notify(Matchers.same(mergingSource), argument.capture());
        final Record actualRecord = argument.getValue();

        final Record expectedRecord = TestBeanFactory.createRecordBuilder()
                .setAnnotations(matchingRecord.getAnnotations())
                .setTime(matchingRecord.getTime())
                .setMetrics(ImmutableMap.of(
                        "foo/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(ImmutableList.of(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE)),
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build()))
                .build();
        Assert.assertTrue(
                String.format("expected=%s, actual=%s", expectedRecord, actualRecord),
                UnorderedRecordEquality.equals(expectedRecord, actualRecord));
    }

    @Test
    public void testDropMetricOfDifferentType() {
        final Record matchingRecord = TestBeanFactory.createRecordBuilder()
                .setMetrics(ImmutableMap.of(
                        "foo/1/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(Collections.singletonList(
                                        new Quantity(3.14f, Optional.<Unit>absent())))
                                .build(),
                        "foo/2/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.TIMER)
                                .setValues(Collections.singletonList(
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build()))
                .build();

        final Source mergingSource = _mergingSourceBuilder.build();
        mergingSource.attach(_mockObserver);
        notify(_mockSource, matchingRecord);

        final ArgumentCaptor<Record> argument = ArgumentCaptor.forClass(Record.class);
        Mockito.verify(_mockObserver).notify(Matchers.same(mergingSource), argument.capture());
        final Record actualRecord = argument.getValue();

        final Record expectedRecord1 = TestBeanFactory.createRecordBuilder()
                .setAnnotations(matchingRecord.getAnnotations())
                .setTime(matchingRecord.getTime())
                .setMetrics(ImmutableMap.of(
                        "foo/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(ImmutableList.of(
                                        new Quantity(3.14f, Optional.<Unit>absent())))
                                .build()))
                .build();
        final Record expectedRecord2 = TestBeanFactory.createRecordBuilder()
                .setAnnotations(matchingRecord.getAnnotations())
                .setTime(matchingRecord.getTime())
                .setMetrics(ImmutableMap.of(
                        "foo/bar",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.TIMER)
                                .setValues(ImmutableList.of(
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build()))
                .build();

        Assert.assertTrue(
                String.format("expected1=%s OR expected2=%s, actual=%s", expectedRecord1, expectedRecord2, actualRecord),
                UnorderedRecordEquality.equals(expectedRecord1, actualRecord)
                        || UnorderedRecordEquality.equals(expectedRecord2, actualRecord));
    }

    @Test
    public void testReplaceWithCapture() {
        final Record matchingRecord = TestBeanFactory.createRecordBuilder()
                .setAnnotations(Collections.singletonMap("key", "value"))
                .setMetrics(ImmutableMap.of(
                        "cat/sheep/dog",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(Collections.singletonList(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE))))
                                .build()))
                .build();

        final Source mergingSource = _mergingSourceBuilder.build();
        mergingSource.attach(_mockObserver);
        notify(_mockSource, matchingRecord);

        final ArgumentCaptor<Record> argument = ArgumentCaptor.forClass(Record.class);
        Mockito.verify(_mockObserver).notify(Matchers.same(mergingSource), argument.capture());
        final Record actualRecord = argument.getValue();

        final Record expectedRecord = TestBeanFactory.createRecordBuilder()
                .setAnnotations(matchingRecord.getAnnotations())
                .setTime(matchingRecord.getTime())
                .setMetrics(ImmutableMap.of(
                        "cat/dog/sheep",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(ImmutableList.of(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE))))
                                .build()))
                .build();
        Assert.assertTrue(
                String.format("expected=%s, actual=%s", expectedRecord, actualRecord),
                UnorderedRecordEquality.equals(expectedRecord, actualRecord));
    }

    @Test
    public void testMultipleMatches() {
        final Record matchingRecord = TestBeanFactory.createRecordBuilder()
                .setAnnotations(Collections.singletonMap("key", "value"))
                .setMetrics(ImmutableMap.of(
                        "cat/bear/dog",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(Collections.singletonList(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE))))
                                .build(),
                        "cat/sheep/dog",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(Collections.singletonList(
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build()))
                .build();

        final Source mergingSource = _mergingSourceBuilder.build();
        mergingSource.attach(_mockObserver);
        notify(_mockSource, matchingRecord);

        final ArgumentCaptor<Record> argument = ArgumentCaptor.forClass(Record.class);
        Mockito.verify(_mockObserver).notify(Matchers.same(mergingSource), argument.capture());
        final Record actualRecord = argument.getValue();

        final Record expectedRecord = TestBeanFactory.createRecordBuilder()
                .setAnnotations(matchingRecord.getAnnotations())
                .setTime(matchingRecord.getTime())
                .setMetrics(ImmutableMap.of(
                        "cat/dog",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(ImmutableList.of(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE)),
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build(),
                        "cat/dog/sheep",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(ImmutableList.of(
                                        new Quantity(6.28f, Optional.<Unit>absent())))
                                .build(),
                        "cat/dog/bear",
                        TestBeanFactory.createMetricBuilder()
                                .setType(MetricType.GAUGE)
                                .setValues(ImmutableList.of(
                                        new Quantity(3.14f, Optional.<Unit>of(Unit.BYTE))))
                                .build()))
                .build();
        Assert.assertTrue(
                String.format("expected=%s, actual=%s", expectedRecord, actualRecord),
                UnorderedRecordEquality.equals(expectedRecord, actualRecord));
    }

    private static void notify(final Observable observable, final Object event) {
        final ArgumentCaptor<Observer> argument = ArgumentCaptor.forClass(Observer.class);
        Mockito.verify(observable).attach(argument.capture());
        for (final Observer observer : argument.getAllValues()) {
            observer.notify(observable, event);
        }
    }

    private Observer _mockObserver;
    private Source _mockSource;
    private MappingSource.Builder _mergingSourceBuilder;
}
