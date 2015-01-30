/**
 * Copyright 2014 Brandon Arp
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

import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.google.common.collect.Sets;

import org.joda.time.Period;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

/**
 * Tests for the LineProcessor class.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LineProcessorTest {
    @Test
    public void testConstructor() {
        final Set<Statistic> timerStats = Sets.newHashSet();
        final Set<Statistic> counterStats = Sets.newHashSet();
        final Set<Statistic> gaugeStats = Sets.newHashSet();
        final String hostName = "host";
        final String service = "serviceName";
        final String cluster = "cluster";
        final Set<Period> periods = Sets.newHashSet();
        periods.add(Period.seconds(7));
        periods.add(Period.minutes(3));
        final Sink publisher = Mockito.mock(Sink.class);
        final LineProcessor processor = new LineProcessor(
                timerStats,
                counterStats,
                gaugeStats,
                hostName,
                service,
                cluster,
                periods,
                publisher
        );
        processor.shutdown();
    }
}
