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

package com.arpnetworking.metrics.generator;

import com.arpnetworking.metrics.generator.metric.GaussianMetricGenerator;
import com.arpnetworking.metrics.generator.metric.MetricGenerator;
import com.arpnetworking.metrics.generator.name.SingleNameGenerator;
import com.arpnetworking.metrics.generator.schedule.ConstantTimeScheduler;
import com.arpnetworking.metrics.generator.uow.UnitOfWorkGenerator;
import com.arpnetworking.metrics.generator.uow.UnitOfWorkSchedule;
import com.arpnetworking.metrics.generator.util.RealTimeExecutor;
import com.arpnetworking.metrics.generator.util.TestFileGenerator;
import com.google.common.collect.Lists;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main class for the metrics generator.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Generator {
    /**
     * Main entry point for the generator.
     *
     * @param args Command line arguments.
     */
    public static void main(final String[] args) {
        boolean continuous = false;
        if (args.length > 0 && args[0].equals("--continuous")) {
            continuous = true;
        }

        final Generator generator = new Generator(continuous);
        generator.run();
    }

    private Generator(final boolean continuous) {
        _continuous = continuous;
    }

    private void run() {
        final MersenneTwister mersenneTwister = new MersenneTwister(88);
        try {
            if (_continuous) {
                generateContinuous(mersenneTwister);
            } else {
                generateTestFiles(mersenneTwister);
            }
        } catch (final IOException e) {
            LOGGER.error("Error generating files", e);
        }
    }

    private void generateTestFiles(final RandomGenerator mersenneTwister) throws IOException {
        //TODO(barp): Set these parameters from command line args [MAI-416]
        final List<Integer> metricSamplesPerUOW = Lists.newArrayList(1, 5, 25);
        final List<Integer> uowPerInterval = Lists.newArrayList(10000, 50000, 250000);
        final List<Integer> metricNamesPerUOW = Lists.newArrayList(1, 10, 100);

        final DateTime start = DateTime.now().hourOfDay().roundFloorCopy();
        final DateTime stop = start.plusMinutes(10);
        for (final Integer uowCount : uowPerInterval) {
            for (final Integer namesCount : metricNamesPerUOW) {
                for (final Integer samplesCount : metricSamplesPerUOW) {
                    final Path fileName = Paths.get(
                            String.format("logs/r_%08d_m_%03d_s_%03d", uowCount, namesCount, samplesCount));

                    final TestFileGenerator testFileGenerator = new TestFileGenerator.Builder()
                            .setRandom(mersenneTwister)
                            .setUnitOfWorkCount(uowCount)
                            .setNamesCount(namesCount)
                            .setSamplesCount(samplesCount)
                            .setStartTime(start)
                            .setEndTime(stop)
                            .setFileName(fileName)
                            .build();
                    testFileGenerator.generate();
                }
            }
        }
    }


    private void generateContinuous(final RandomGenerator mersenneTwister) {
        final List<MetricGenerator> metricGenerators = Lists.newArrayList();
        for (int x = 0; x < 5; x++) {
            metricGenerators.add(new GaussianMetricGenerator(50d, 8d, new SingleNameGenerator(mersenneTwister)));
        }
        final UnitOfWorkGenerator uowGenerator = new UnitOfWorkGenerator(metricGenerators);

        final List<UnitOfWorkSchedule> schedules = Lists.newArrayList();
        schedules.add(new UnitOfWorkSchedule(uowGenerator, new ConstantTimeScheduler(Period.millis(500))));

        //TODO(barp): The file name should come from command line args [MAI-416]
        final RealTimeExecutor executor = new RealTimeExecutor(schedules, Paths.get("logs/generated-query"));
        executor.execute();
    }

    private final boolean _continuous;
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);
}
