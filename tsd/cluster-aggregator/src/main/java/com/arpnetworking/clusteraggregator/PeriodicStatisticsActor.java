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

package com.arpnetworking.clusteraggregator;

import akka.actor.Props;
import akka.actor.UntypedActor;
import com.arpnetworking.clusteraggregator.models.MetricsRequest;
import com.arpnetworking.clusteraggregator.models.PeriodMetrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.google.common.collect.Maps;
import org.joda.time.Period;

import java.util.Map;

/**
 * Actor that listens for metrics messages, updates internal state, and emits them.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class PeriodicStatisticsActor extends UntypedActor {
    /**
     * Creates a <code>Props</code> for construction in Akka.
     *
     * @param metricsFactory A <code>MetricsFactory</code> to use for metrics creation.
     * @return A new <code>Props</code>.
     */
    public static Props props(final MetricsFactory metricsFactory) {
        return Props.create(PeriodicStatisticsActor.class, metricsFactory);
    }

    /**
     * Public constructor.
     *
     * @param metricsFactory A <code>MetricsFactory</code> to use for metrics creation.
     */
    public PeriodicStatisticsActor(final MetricsFactory metricsFactory) {
        _metricsFactory = metricsFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof AggregatedData) {
            final AggregatedData report = (AggregatedData) message;
            final Period period = report.getPeriod();
            PeriodMetrics metrics = _periodMetrics.get(period);
            if (metrics == null) {
                metrics = new PeriodMetrics(_metricsFactory);
                _periodMetrics.put(period, metrics);
            }

            metrics.recordAggregation(report);
        } else if (message instanceof MetricsRequest) {
            getSender().tell(_periodMetrics, getSelf());
        } else {
            unhandled(message);
        }
    }

    private final Map<Period, PeriodMetrics> _periodMetrics = Maps.newHashMap();
    private final MetricsFactory _metricsFactory;
}
