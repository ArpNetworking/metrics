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
package com.arpnetworking.tsdaggregator;

import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdaggregator.configuration.PipelineConfiguration;
import com.arpnetworking.tsdcore.sinks.MultiSink;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.utility.DefaultHostResolver;
import com.arpnetworking.utility.HostResolver;
import com.arpnetworking.utility.Launchable;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single data pathway through the time series data aggregator. The pathway
 * consists of zero or more sources, typically at least one source is specified,
 * a <code>LineProcessor</code> and zero or more sinks, again typically at least
 * one sink is specified.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class Pipeline implements Launchable {

    /**
     * Public constructor.
     *
     * @param pipelineConfiguration Instance of <code>PipelineConfiguration</code>.
     */
    public Pipeline(final PipelineConfiguration pipelineConfiguration) {
        _pipelineConfiguration = pipelineConfiguration;
    }

    /**
     * Launch the pipeline.
     */
    @Override
    public void launch() {
        LOGGER.info()
                .setMessage("Launching pipeline")
                .addData("configuration", _pipelineConfiguration)
                .log();

        final String host;
        try {
            if (_pipelineConfiguration.getHost().isPresent()) {
                host = _pipelineConfiguration.getHost().get();
            } else {
                host = HOST_RESOLVER.getLocalHostName();
            }
        } catch (final UnknownHostException e) {
            LOGGER.error()
                    .setMessage("Failed to determine host name")
                    .setThrowable(e)
                    .log();
            throw Throwables.propagate(e);
        }

        final Sink rootSink = new MultiSink.Builder()
                .setName(_pipelineConfiguration.getName())
                .setSinks(_pipelineConfiguration.getSinks())
                .build();
        _sinks.add(rootSink);

        final Aggregator aggregator = new Aggregator.Builder()
                .setService(_pipelineConfiguration.getService())
                .setCluster(_pipelineConfiguration.getCluster())
                .setPeriods(_pipelineConfiguration.getPeriods())
                .setHost(host)
                .setTimerStatistics(_pipelineConfiguration.getTimerStatistic())
                .setCounterStatistics(_pipelineConfiguration.getCounterStatistic())
                .setGaugeStatistics(_pipelineConfiguration.getGaugeStatistic())
                .setSink(rootSink)
                .build();
        aggregator.launch();
        _aggregator.set(aggregator);

        for (final Source source : _pipelineConfiguration.getSources()) {
            source.attach(aggregator);
            source.start();
            _sources.add(source);
        }
    }

    /**
     * Shutdown the pipeline.
     */
    @Override
    public void shutdown() {
        LOGGER.info()
                .setMessage("Stopping pipeline")
                .addData("pipeline", _pipelineConfiguration.getName())
                .log();

        for (final Source source : _sources) {
            source.stop();
        }
        final Optional<Aggregator> aggregator = Optional.fromNullable(_aggregator.getAndSet(null));
        if (aggregator.isPresent()) {
            aggregator.get().shutdown();
        }
        for (final Sink sink : _sinks) {
            sink.close();
        }

        _sources.clear();
        _sinks.clear();
    }

    private final PipelineConfiguration _pipelineConfiguration;
    private final AtomicReference<Aggregator> _aggregator = new AtomicReference<>();
    private final List<Sink> _sinks = Lists.newArrayList();
    private final List<Source> _sources = Lists.newArrayList();

    private static final HostResolver HOST_RESOLVER = new DefaultHostResolver();
    private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);
}
