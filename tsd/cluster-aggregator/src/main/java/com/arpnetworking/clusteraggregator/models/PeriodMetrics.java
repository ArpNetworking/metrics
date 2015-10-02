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

package com.arpnetworking.clusteraggregator.models;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Holds the node metrics for a single period.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class PeriodMetrics {
    /**
     * Public constructor.
     *
     * @param metricsFactory metrics factory to create metrics from
     */
    public PeriodMetrics(final MetricsFactory metricsFactory) {
        _metricsFactory = metricsFactory;
    }

    /**
     * Stores the report of a metric into the counters.
     *
     * @param report Aggregated data to be consumed
     */
    public void recordAggregation(final AggregatedData report) {
        final String fqsn = new StringBuilder()
                .append(report.getFQDSN().getCluster())
                .append("/")
                .append(report.getFQDSN().getService())
                .append("/")
                .append(report.getFQDSN().getMetric())
                .append("/")
                .append(report.getPeriod())
                .append("/")
                .append(report.getFQDSN().getStatistic())
                .toString();

        final String metricName = new StringBuilder()
                .append(report.getFQDSN().getService())
                .append("/")
                .append(report.getFQDSN().getMetric())
                .toString();

        //TODO(barp): rework statistic collection [MAI-379]
        if (_latestPeriod == null || _latestPeriod.isBefore(report.getPeriodStart())) {
            dumpMetrics();
            clearForNextPeriod(report);
        }

        if (_latestPeriod.equals(report.getPeriodStart())) {
            if (!_latestPeriodStatsBloomFilter.mightContain(fqsn)) {
                _latestPeriodStatsBloomFilter.put(fqsn);
                ++_statisticsLatestPeriod;
            }

            if (!_latestPeriodServicesBloomFilter.mightContain(report.getFQDSN().getService())) {
                _latestPeriodServicesBloomFilter.put(report.getFQDSN().getService());
                ++_servicesLatestPeriod;
            }

            if (!_latestPeriodMetricsBloomFilter.mightContain(metricName)) {
                _latestPeriodMetricsBloomFilter.put(metricName);
                ++_metricsLatestPeriod;
            }

            final String serviceCluster = report.getFQDSN().getService() + "/" + report.getFQDSN().getCluster();
            Long sampleCount = _sampleCountLatestPeriod.getOrDefault(serviceCluster, 0L);
            sampleCount += report.getPopulationSize();
            _sampleCountLatestPeriod.put(serviceCluster, sampleCount);
        }
    }

    private void dumpMetrics() {
        if (_latestPeriod != null) {
            try (final Metrics metrics = _metricsFactory.create()) {
                long totalSamples = 0L;
                metrics.incrementCounter("cluster/period/metrics_seen", _metricsLatestPeriod);
                metrics.incrementCounter("cluster/period/statistics_seen", _statisticsLatestPeriod);
                metrics.incrementCounter("cluster/period/services_seen", _servicesLatestPeriod);
                for (final Map.Entry<String, Long> entry : _sampleCountLatestPeriod.entrySet()) {
                    metrics.incrementCounter("cluster/period/sample_count/" + entry.getKey(), entry.getValue());
                    totalSamples += entry.getValue();
                }
                metrics.incrementCounter("cluster/period/sample_count/total", totalSamples);
            }
        }
    }

    private void clearForNextPeriod(final AggregatedData report) {
        _latestPeriod = report.getPeriodStart();
        _metricsLatestPeriod = 0;
        _servicesLatestPeriod = 0;
        _statisticsLatestPeriod = 0;
        _latestPeriodMetricsBloomFilter = createMetricsBF();
        _latestPeriodServicesBloomFilter = createServicesBF();
        _latestPeriodStatsBloomFilter = createStatisticsBF();
        _sampleCountLatestPeriod.clear();
    }

    public DateTime getLatestPeriod() {
        return _latestPeriod;
    }

    public long getMetricsLatestPeriod() {
        return _metricsLatestPeriod;
    }

    public long getServicesLatestPeriod() {
        return _servicesLatestPeriod;
    }

    public long getStatisticsLatestPeriod() {
        return _statisticsLatestPeriod;
    }

    private BloomFilter<CharSequence> createServicesBF() {
        return BloomFilter.create(
                Funnels.stringFunnel(Charsets.UTF_8),
                100_000,
                0.0001);
    }

    private BloomFilter<CharSequence> createMetricsBF() {
        return BloomFilter.create(
                Funnels.stringFunnel(Charsets.UTF_8),
                10_000_000,
                0.001);
    }

    private BloomFilter<CharSequence> createStatisticsBF() {
        return BloomFilter.create(
                Funnels.stringFunnel(Charsets.UTF_8),
                100_000_000,
                0.005);
    }

    private final MetricsFactory _metricsFactory;

    private DateTime _latestPeriod = null;
    private long _metricsLatestPeriod = 0;
    private long _servicesLatestPeriod = 0;
    private long _statisticsLatestPeriod = 0;
    private Map<String, Long> _sampleCountLatestPeriod = Maps.newHashMap();
    private BloomFilter<CharSequence> _latestPeriodMetricsBloomFilter = createMetricsBF();
    private BloomFilter<CharSequence> _latestPeriodServicesBloomFilter = createServicesBF();
    private BloomFilter<CharSequence> _latestPeriodStatsBloomFilter = createStatisticsBF();
}
