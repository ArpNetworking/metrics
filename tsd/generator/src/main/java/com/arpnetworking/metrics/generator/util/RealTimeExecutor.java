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

package com.arpnetworking.metrics.generator.util;

import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.generator.client.GeneratorSink;
import com.arpnetworking.metrics.generator.uow.UnitOfWorkSchedule;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

/**
 * Executes a scheduler in real-time.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class RealTimeExecutor {
    /**
     * Public constructor.
     *
     * @param generators List of UOW generators.
     * @param outputPath File to write metrics to.
     */
    public RealTimeExecutor(final List<UnitOfWorkSchedule> generators, final Path outputPath) {
        _generators = generators;
        _workEntries = new PriorityQueue<>(generators.size(), new WorkItemOrdering());
        _modifyingSink = new GeneratorSink(outputPath, DateTime.now());

        _metricsFactory = new TsdMetricsFactory.Builder().setSinks(Collections.<Sink>singletonList(_modifyingSink)).build();
    }

    /**
     * Generates metrics.
     */
    public void execute() {
        for (final UnitOfWorkSchedule generator : _generators) {
            final long unitStart = generator.getScheduler().next(
                    TimeUnit.NANOSECONDS.convert(DateTime.now().getMillis(), TimeUnit.MILLISECONDS));
            _workEntries.add(new WorkEntry(generator, unitStart));
        }
        while (true) {
            if (_workEntries.isEmpty()) {
                break;
            }
            final WorkEntry entry = _workEntries.peek();
            final DateTime executeTime = new DateTime(TimeUnit.MILLISECONDS.convert(entry.getCurrentValue(), TimeUnit.NANOSECONDS));
            if (executeTime.isAfterNow()) {
                try {
                    Thread.sleep(10);
                } catch (final InterruptedException ignored) {
                    Thread.interrupted();
                    return;
                }
                continue;
            }
            _workEntries.poll();
            _modifyingSink.setTime(new DateTime(TimeUnit.MILLISECONDS.convert(entry.getCurrentValue(), TimeUnit.NANOSECONDS)));
            entry.getSchedule().getGenerator().generate(_metricsFactory);
            final WorkEntry newEntry = new WorkEntry(
                    entry.getSchedule(),
                    entry.getSchedule().getScheduler().next(entry.getCurrentValue()));
            _workEntries.add(newEntry);
        }
    }

    private final GeneratorSink _modifyingSink;
    private final MetricsFactory _metricsFactory;
    private final PriorityQueue<WorkEntry> _workEntries;
    private final List<UnitOfWorkSchedule> _generators;
}
