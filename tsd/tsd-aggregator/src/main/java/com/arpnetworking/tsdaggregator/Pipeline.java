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

import com.arpnetworking.tsdaggregator.configuration.PipelineConfiguration;
import com.arpnetworking.tsdcore.sinks.MultiSink;
import com.arpnetworking.tsdcore.sinks.Sink;
import com.arpnetworking.tsdcore.sources.Source;
import com.arpnetworking.utility.DefaultHostResolver;
import com.arpnetworking.utility.HostResolver;
import com.arpnetworking.utility.Launchable;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.List;

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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Launching pipeline; configuration=%s", _pipelineConfiguration));
        } else {
            LOGGER.info(String.format("Launching pipeline; pipeline=%s", _pipelineConfiguration.getName()));
        }

        final String hostName;
        try {
            if (_pipelineConfiguration.getHost().isPresent()) {
                hostName = _pipelineConfiguration.getHost().get();
            } else {
                hostName = HOST_RESOLVER.getLocalHostName();
            }
        } catch (final UnknownHostException e) {
            LOGGER.error("Failed to determine host name", e);
            throw Throwables.propagate(e);
        }

        final Sink rootSink = new MultiSink.Builder()
                .setName(_pipelineConfiguration.getName())
                .setSinks(_pipelineConfiguration.getSinks())
                .build();
        _sinks.add(rootSink);

        final LineProcessor processor = new LineProcessor(
                _pipelineConfiguration.getTimerStatistic(),
                _pipelineConfiguration.getCounterStatistic(),
                _pipelineConfiguration.getGaugeStatistic(),
                hostName,
                _pipelineConfiguration.getService(),
                _pipelineConfiguration.getCluster(),
                _pipelineConfiguration.getPeriods(),
                rootSink
        );
        _processors.add(processor);

        for (final Source source : _pipelineConfiguration.getSources()) {
            source.attach(processor);
            source.start();
            _sources.add(source);
        }
    }

    /**
     * Shutdown the pipeline.
     */
    @Override
    public void shutdown() {
        LOGGER.info(String.format("Stopping pipeline; pipeline=%s", _pipelineConfiguration.getName()));

        for (final Source source : _sources) {
            source.stop();
        }
        for (final LineProcessor processor : _processors) {
            processor.closeAggregations();
            processor.shutdown();
        }
        for (final Sink sink : _sinks) {
            sink.close();
        }

        _sources.clear();
        _processors.clear();
        _sinks.clear();
    }

    private final PipelineConfiguration _pipelineConfiguration;
    private final List<LineProcessor> _processors = Lists.newArrayList();
    private final List<Sink> _sinks = Lists.newArrayList();
    private final List<Source> _sources = Lists.newArrayList();

    private static final HostResolver HOST_RESOLVER = new DefaultHostResolver();
    private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);
}
