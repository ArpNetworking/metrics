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

package com.carrotsearch.junitbenchmarks;

import com.arpnetworking.test.junitbenchmarks.JsonBenchmarkConsumerTest;
import org.junit.runner.Description;

/**
 * A class that can create a {@link GCSnapshot}.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class DataCreator {
    private DataCreator() {}

    /**
     * Creates a new {@link GCSnapshot}.
     *
     * @return a new {@link GCSnapshot}
     */
    public static GCSnapshot createGCSnapshot() {
        return new GCSnapshot();
    }

    /**
     * Creates a new {@link Average}.
     *
     * @param average the average
     * @param stddev the standard deviation
     * @return a new {@link Average}
     */
    public static Average createAverage(final double average, final double stddev) {
        return new Average(average, stddev);
    }

    /**
     * Creates a new {@link Result}.
     *
     * @return a new {@link Result}
     */
    public static Result createResult() {
        final Description description = Description.createTestDescription(
                JsonBenchmarkConsumerTest.class,
                "testNormalBenchmarkCase");
        final Average roundAverage = createAverage(1500, 800);
        final Average blockedAverage = createAverage(500, 200);
        final Average gcAverage = createAverage(50, 10);
        final GCSnapshot gcSnapshot = createGCSnapshot();
        final int benchmarkRounds = 1;
        final int warmupRounds = 1;
        final int warmupTime = 5000;
        final int benchmarkTime = 2500;
        final int concurrency = 2;
        return new Result(
                description,
                benchmarkRounds,
                warmupRounds,
                warmupTime,
                benchmarkTime,
                roundAverage,
                blockedAverage,
                gcAverage,
                gcSnapshot,
                concurrency);
    }
}
