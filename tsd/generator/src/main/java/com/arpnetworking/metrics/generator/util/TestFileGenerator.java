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

import com.arpnetworking.metrics.generator.metric.ConstantCountMetricGenerator;
import com.arpnetworking.metrics.generator.metric.ConstantMetricGenerator;
import com.arpnetworking.metrics.generator.metric.GaussianMetricGenerator;
import com.arpnetworking.metrics.generator.metric.MetricGenerator;
import com.arpnetworking.metrics.generator.name.SingleNameGenerator;
import com.arpnetworking.metrics.generator.name.SpecifiedName;
import com.arpnetworking.metrics.generator.schedule.ConstantTimeScheduler;
import com.arpnetworking.metrics.generator.uow.UnitOfWorkGenerator;
import com.arpnetworking.metrics.generator.uow.UnitOfWorkSchedule;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import org.apache.commons.math3.random.RandomGenerator;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to generate a file for use in performance testing.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class TestFileGenerator {
    private TestFileGenerator(final Builder builder) {
        _random = builder._random;
        _uowCount = builder._uowCount;
        _namesCount = builder._namesCount;
        _samplesCount = builder._samplesCount;
        _startTime = builder._startTime;
        _endTime = builder._endTime;
        _fileName = builder._fileName;
    }

    /**
     * Generates the test file.
     */
    public void generate() {
        try {
            Files.deleteIfExists(_fileName);
        } catch (final IOException e) {
            throw Throwables.propagate(e);
        }

        final long totalSampleCount = ((long) _uowCount) * _namesCount * _samplesCount;
        LOGGER.info(String.format("Generating file; file=%s, expectedSamples=%d", _fileName.toAbsolutePath(), totalSampleCount));

        final Duration duration = new Duration(_startTime, _endTime);

        final List<MetricGenerator> metricGenerators = Lists.newArrayList();
        for (int x = 0; x < _namesCount; ++x) {
            final GaussianMetricGenerator gaussian = new GaussianMetricGenerator(
                    50d, 8d, new SingleNameGenerator(_random));
            final ConstantCountMetricGenerator sampleGenerator = new ConstantCountMetricGenerator(_samplesCount, gaussian);
            metricGenerators.add(sampleGenerator);
        }
        final UnitOfWorkGenerator uowGenerator = new UnitOfWorkGenerator(metricGenerators);

        final List<UnitOfWorkSchedule> schedules = Lists.newArrayList();
        final long period = TimeUnit.NANOSECONDS.convert(duration.getMillis(), TimeUnit.MILLISECONDS) / _uowCount;
        schedules.add(new UnitOfWorkSchedule(uowGenerator, new ConstantTimeScheduler(period)));

        final MetricGenerator canary = new ConstantMetricGenerator(5, new SpecifiedName(CANARY));

        // Special canary unit of work schedulers
        // Each UOW generator is guaranteed to be executed once
        final UnitOfWorkGenerator canaryUOW = new UnitOfWorkGenerator(Collections.singletonList(canary));
        schedules.add(new UnitOfWorkSchedule(canaryUOW, new ConstantTimeScheduler(
                duration.plus(Duration.standardHours(1)).toPeriod())));
        schedules.add(new UnitOfWorkSchedule(canaryUOW, new ConstantTimeScheduler(
                duration.plus(Duration.standardHours(2)).toPeriod())));


        final IntervalExecutor executor = new IntervalExecutor(_startTime, _endTime, schedules, _fileName);
        executor.execute();
        try {
            final BasicFileAttributes attributes = Files.readAttributes(_fileName, BasicFileAttributes.class);
            LOGGER.info(String.format("Generation complete; size=%s", attributes.size()));
        } catch (final IOException e) {
            LOGGER.warn("Unable to read attributes of generated file", e);
        }
    }

    /**
     * Name of the ending canary metric.
     */
    public static final String CANARY = "endCanary";

    private final RandomGenerator _random;
    private final Integer _uowCount;
    private final Integer _namesCount;
    private final Integer _samplesCount;
    private final DateTime _startTime;
    private final DateTime _endTime;
    private final Path _fileName;
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFileGenerator.class);

    /**
     * Builder for a <code>TestFileGenerator</code>.
     */
    public static class Builder extends OvalBuilder<TestFileGenerator> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(TestFileGenerator.class);
        }

        /**
         * Sets the random generator.
         *
         * @param random The random generator.
         * @return This builder.
         */
        public Builder setRandom(final RandomGenerator random) {
            _random = random;
            return this;
        }

        /**
         * Sets the unit of work count.
         *
         * @param uowCount Unit of work count.
         * @return This builder.
         */
        public Builder setUnitOfWorkCount(final Integer uowCount) {
            _uowCount = uowCount;
            return this;
        }

        /**
         * Sets the names count.
         *
         * @param namesCount The names count
         * @return This builder.
         */
        public Builder setNamesCount(final Integer namesCount) {
            _namesCount = namesCount;
            return this;
        }

        /**
         * Sets the samples count.
         *
         * @param samplesCount The samples count
         * @return This builder.
         */
        public Builder setSamplesCount(final Integer samplesCount) {
            _samplesCount = samplesCount;
            return this;
        }

        /**
         * Sets the start time.
         *
         * @param startTime The start time
         * @return This builder.
         */
        public Builder setStartTime(final DateTime startTime) {
            _startTime = startTime;
            return this;
        }

        /**
         * Sets the end time.
         *
         * @param endTime The end time
         * @return This builder.
         */
        public Builder setEndTime(final DateTime endTime) {
            _endTime = endTime;
            return this;
        }

        /**
         * Sets the file name.
         *
         * @param fileName The file name
         * @return This builder.
         */
        public Builder setFileName(final Path fileName) {
            _fileName = fileName;
            return this;
        }

        /**
         * Build the <code>TestFileGenerator</code>.
         *
         * @return the new <code>TestFileGenerator</code>
         */
        public TestFileGenerator build() {
            return new TestFileGenerator(this);
        }

        @NotNull
        private RandomGenerator _random;
        @Min(1)
        private Integer _uowCount;
        @Min(1)
        private Integer _namesCount;
        @Min(1)
        private Integer _samplesCount;
        @NotNull
        private DateTime _startTime;
        @NotNull
        private DateTime _endTime;
        @NotNull
        private Path _fileName;
    }
}
