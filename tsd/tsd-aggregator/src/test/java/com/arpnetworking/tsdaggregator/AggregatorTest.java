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
package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.model.DefaultMetric;
import com.arpnetworking.tsdaggregator.model.DefaultRecord;
import com.arpnetworking.tsdaggregator.model.MetricType;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.TP100Statistic;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

/**
 * Tests for the <code>Aggregator</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class AggregatorTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCloseAfterElapsed() throws InterruptedException {
        final DateTime dataTimeInThePast = new DateTime(DateTimeZone.UTC)
                .minus(Duration.standardSeconds(10));

        // Create aggregator
        final Sink sink = Mockito.mock(Sink.class);
        final Aggregator aggregator = new Aggregator.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setHost("MyHost")
                .setSink(sink)
                .setCounterStatistics(Collections.singleton(MAX_STATISTIC))
                .setTimerStatistics(Collections.singleton(MAX_STATISTIC))
                .setGaugeStatistics(Collections.singleton(MAX_STATISTIC))
                .setPeriods(Collections.singleton(Period.seconds(1)))
                .build();
        try {
            aggregator.launch();

            // Send data to aggregator
            aggregator.notify(
                    null,
                    new DefaultRecord.Builder()
                            .setMetrics(Collections.singletonMap(
                                    "MyMetric",
                                    new DefaultMetric.Builder()
                                            .setType(MetricType.GAUGE)
                                            .setValues(Collections.singletonList(
                                                    new Quantity.Builder().setValue(1d).build()))
                                            .build()))
                            .setTime(dataTimeInThePast)
                            .build());

            // Wait for the period to close
            Thread.sleep(2000);

            // Verify the aggregation was emitted
            Mockito.verify(sink).recordAggregateData(_dataCaptor.capture(), _conditionsCaptor.capture());
            Mockito.verifyNoMoreInteractions(sink);

            final List<Condition> conditions = _conditionsCaptor.getValue();
            Assert.assertTrue(conditions.isEmpty());

            final List<AggregatedData> data = _dataCaptor.getValue();
            Assert.assertEquals(1, data.size());
            Assert.assertEquals(
                    new AggregatedData.Builder()
                            .setFQDSN(new FQDSN.Builder()
                                    .setCluster("MyCluster")
                                    .setService("MyService")
                                    .setMetric("MyMetric")
                                    .setStatistic(MAX_STATISTIC)
                                    .build())
                            .setHost("MyHost")
                            .setPeriod(Period.seconds(1))
                            .setPopulationSize(1L)
                            .setSamples(Collections.singletonList(new Quantity.Builder().setValue(1d).build()))
                            .setStart(dataTimeInThePast.withMillisOfSecond(0))
                            .setValue(new Quantity.Builder().setValue(1d).build())
                            .build(),
                    data.get(0));
        } finally {
            aggregator.shutdown();
        }
    }

    @Captor
    private ArgumentCaptor<List<AggregatedData>> _dataCaptor;
    @Captor
    private ArgumentCaptor<List<Condition>> _conditionsCaptor;

    private static final Statistic MAX_STATISTIC = new TP100Statistic();
}
