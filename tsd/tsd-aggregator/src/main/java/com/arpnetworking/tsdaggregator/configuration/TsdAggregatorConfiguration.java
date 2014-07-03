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
package com.arpnetworking.tsdaggregator.configuration;

import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.tsdcore.exceptions.ConfigurationException;
import com.arpnetworking.utility.OvalBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import net.sf.oval.constraint.NotNull;
import net.sf.oval.exception.ConstraintsViolatedException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Representation of TsdAggregator configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TsdAggregatorConfiguration {

    /**
     * Static factory for creating a <code>TsdAggregatorConfiguration</code>
     * instance from command line arguments.
     *
     * @param arguments The command line arguments to create the configuration
     * from.
     * @return Instance of <code>TsdAggregatorConfiguration</code>.
     * @throws ConfigurationException If the configuration cannot be created
     * for any reason.
     */
    public static TsdAggregatorConfiguration create(final List<String> arguments) throws ConfigurationException {
        final Options options = new Options().addOption(
                Option.builder("c")
                        .longOpt("config")
                        .hasArg()
                        .optionalArg(false)
                        .argName("configuration file")
                        .required(true)
                        .build());
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine;
        try {
            commandLine = parser.parse(options, arguments.toArray(new String[arguments.size()]));
        } catch (final ParseException e) {
            throw new ConfigurationException(String.format("Unable to parse configuration; arguments=%s", arguments), e);
        }
        final Optional<String> configurationFile = Optional.fromNullable(commandLine.getOptionValue("config"));
        if (configurationFile.isPresent()) {
            try {
                final URL url = new URI(configurationFile.get()).toURL();
                return OBJECT_MAPPER.readValue(url, TsdAggregatorConfiguration.class);
            } catch (final URISyntaxException | IllegalArgumentException | IOException | ConstraintsViolatedException e) {
                throw new ConfigurationException(String.format("Unable to parse configuration; file=%s", configurationFile), e);
            }
        }
        throw new ConfigurationException("No configuration file specified");
    }

    public File getLogDirectory() {
        return _logDirectory;
    }

    public File getPipelinesDirectory() {
        return _pipelinesDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("LogDirectory", _logDirectory)
                .add("PipelinesDirectory", _pipelinesDirectory)
                .toString();
    }

    private TsdAggregatorConfiguration(final Builder builder) {
        _logDirectory = builder._logDirectory;
        _pipelinesDirectory = builder._pipelinesDirectory;
    }

    private final File _logDirectory;
    private final File _pipelinesDirectory;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule("TsdAggregatorConfiguration");
        module.addDeserializer(TsdAggregatorConfiguration.class, BuilderDeserializer.of(TsdAggregatorConfiguration.Builder.class));
        OBJECT_MAPPER.registerModules(module);
    }

    /**
     * Implementation of builder pattern for <code>TsdAggregatorConfiguration</code>.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    public static class Builder extends OvalBuilder<TsdAggregatorConfiguration> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(TsdAggregatorConfiguration.class);
        }

        /**
         * The log directory. Cannot be null.
         *
         * @param value The log directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setLogDirectory(final File value) {
            _logDirectory = value;
            return this;
        }

        /**
         * The pipelines directory. Cannot be null.
         *
         * @param value The pipelines directory.
         * @return This instance of <code>Builder</code>.
         */
        public Builder setPipelinesDirectory(final File value) {
            _pipelinesDirectory = value;
            return this;
        }

        @NotNull
        private File _logDirectory;
        @NotNull
        private File _pipelinesDirectory;
    }
}
