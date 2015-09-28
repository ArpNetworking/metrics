/**
 * Copyright 2015 Groupon.com
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
package com.arpnetworking.remet.gui.alerts.impl;

import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeFileSource;
import com.arpnetworking.configuration.jackson.JsonNodeUriSource;
import com.arpnetworking.configuration.triggers.FileTrigger;
import com.arpnetworking.configuration.triggers.UriTrigger;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.logback.annotations.Loggable;
import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.alerts.Alert;
import com.arpnetworking.remet.gui.alerts.AlertQuery;
import com.arpnetworking.remet.gui.alerts.AlertRepository;
import com.arpnetworking.remet.gui.alerts.Context;
import com.arpnetworking.remet.gui.impl.DefaultQueryResult;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.utility.OvalBuilder;
import com.arpnetworking.utility.Reconfigurator;
import com.arpnetworking.utility.Relaunchable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import net.sf.oval.constraint.NotNull;
import play.Configuration;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Implementation of alert repository using dynamic configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class DynamicConfigurationAlertRepository
        implements AlertRepository, Relaunchable<DynamicConfigurationAlertRepository.AlertConfiguration> {

    /**
     * Public constructor.
     *
     * @param playConfiguration Instance of Play's <code>Configuration</code>.
     */
    @Inject
    public DynamicConfigurationAlertRepository(final Configuration playConfiguration) {
        try {
            _configurationUri = new URI(playConfiguration.getString("alertRepository.uri"));
        } catch (final URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        assertIsOpen(false);
        LOGGER.debug().setMessage("Opening alert repository").log();
        final Reconfigurator<DynamicConfigurationAlertRepository, AlertConfiguration> reconfigurator =
                new Reconfigurator<>(this, AlertConfiguration.class);
        if ("http".equalsIgnoreCase(_configurationUri.getScheme())
                || "https".equalsIgnoreCase(_configurationUri.getScheme())) {
            _dynamicConfiguration = new DynamicConfiguration.Builder()
                    .setObjectMapper(OBJECT_MAPPER)
                    .addSourceBuilder(new JsonNodeUriSource.Builder()
                            .setObjectMapper(OBJECT_MAPPER)
                            .setUri(_configurationUri))
                    .addTrigger(new UriTrigger.Builder()
                            .setUri(_configurationUri)
                            .build())
                    .addListener(reconfigurator)
                    .build();
        } else {
            final File configurationFile = new File(_configurationUri.getPath());
            _dynamicConfiguration = new DynamicConfiguration.Builder()
                    .setObjectMapper(OBJECT_MAPPER)
                    .addSourceBuilder(new JsonNodeFileSource.Builder()
                            .setObjectMapper(OBJECT_MAPPER)
                            .setFile(configurationFile))
                    .addTrigger(new FileTrigger.Builder()
                            .setFile(configurationFile)
                            .build())
                    .addListener(reconfigurator)
                    .build();
        }

        _dynamicConfiguration.launch();
        // NOTE: Do not mark the repository as open until the dynamic
        // configuration has been loaded (e.g. this instance has been
        // relaunched).
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        assertIsOpen();
        LOGGER.debug().setMessage("Closing alert repository").log();
        _dynamicConfiguration.shutdown();
        _isOpen.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Alert> get(final UUID identifier) {
        return _alerts.stream()
                .filter(expression -> identifier.equals(expression.getId()))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AlertQuery createQuery() {
        return new DefaultAlertQuery(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<Alert> query(final AlertQuery query) {
        assertIsOpen();
        final List<? extends Alert> alerts = _alerts.stream()
                .filter(alert -> query.getContext().map(context -> context.equals(alert.getContext())).orElse(true))
                .filter(alert -> query.getCluster().map(cluster -> cluster.equals(alert.getCluster())).orElse(true))
                .filter(alert -> query.getService().map(service -> service.equals(alert.getService())).orElse(true))
                .filter(alert -> query.getContains().map(contains -> {
                    return alert.getCluster().contains(contains)
                            || alert.getService().contains(contains)
                            || alert.getMetric().contains(contains)
                            || alert.getName().contains(contains);
                }).orElse(true))
                .sorted(ALERT_COMPARATOR)
                .collect(Collectors.toList());
        final int start = query.getOffset().orElse(0);
        final int end = start + Math.max(Math.min(alerts.size() - start, query.getLimit()), 0);
        return new DefaultQueryResult<>(
                alerts.subList(start, end),
                alerts.size(),
                Integer.toHexString(_alerts.toString().hashCode()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAlertCount() {
        assertIsOpen();
        return _alerts.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relaunch(final AlertConfiguration alertConfiguration) {
        LOGGER.info()
                .setMessage("Relaunching alert repository")
                .addData("configuration", alertConfiguration)
                .log();

        final List<Alert> newAlerts = Lists.newArrayList(alertConfiguration.getAlerts());
        newAlerts.sort(ALERT_COMPARATOR);

        LOGGER.info()
                .setMessage("Loaded alerts")
                .addData("newAlertsSize", newAlerts.size())
                .addData("previousAlertsSize", _alerts.size())
                .log();

        _alerts = newAlerts;
        _isOpen.set(true);
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("isOpen", _isOpen)
                .put("configurationUri", _configurationUri)
                .put("alerts", _alerts)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private void assertIsOpen() {
        assertIsOpen(true);
    }

    private void assertIsOpen(final boolean expectedState) {
        if (_isOpen.get() != expectedState) {
            throw new IllegalStateException(String.format("Alert repository is not %s", expectedState ? "open" : "closed"));
        }
    }

    private final AtomicBoolean _isOpen = new AtomicBoolean(false);
    private final URI _configurationUri;

    // Do not log the dynamic configuration since _this_ is a listener for changes.
    private DynamicConfiguration _dynamicConfiguration;
    private volatile List<Alert> _alerts = Collections.emptyList();

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationAlertRepository.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final Comparator<Alert> ALERT_COMPARATOR = new AlertComparator();

    static {
        final SimpleModule module = new SimpleModule("DynamicConfigurationAlertRepository");
        BuilderDeserializer.addTo(module, AlertConfiguration.class);
        BuilderDeserializer.addTo(module, DefaultAlert.class);
        module.addDeserializer(Alert.class, BuilderDeserializer.of(DefaultAlert.Builder.class));
        OBJECT_MAPPER.registerModules(module);
    }

    private static final class AlertComparator implements Comparator<Alert> {
        @Override
        public int compare(final Alert a1, final Alert a2) {
            if (a1.getContext().equals(a2.getContext())) {
                int result = a1.getCluster().compareToIgnoreCase(a2.getCluster());
                if (result != 0) {
                    return result;
                }
                result = a1.getService().compareToIgnoreCase(a2.getService());
                if (result != 0) {
                    return result;
                }
                result = a1.getMetric().compareToIgnoreCase(a2.getMetric());
                if (result != 0) {
                    return result;
                }
                result = a1.getStatistic().compareToIgnoreCase(a2.getStatistic());
                if (result != 0) {
                    return result;
                }
                result = a1.getPeriod().toStandardDuration().compareTo(a2.getPeriod().toStandardDuration());
                if (result != 0) {
                    return result;
                }
                result = a1.getName().compareToIgnoreCase(a2.getName());
                if (result != 0) {
                    return result;
                }
                result = a1.getOperator().compareTo(a2.getOperator());
                if (result != 0) {
                    return result;
                }
                result = a1.getValue().compareTo(a2.getValue());
                if (result != 0) {
                    return result;
                }
                LOGGER.warn()
                        .setMessage("Duplicate alert detected")
                        .addData("alert1", a1)
                        .addData("alert2", a2)
                        .log();
                return a1.getId().compareTo(a2.getId());
            }
            if (Context.CLUSTER.equals(a1.getContext())) {
                return -1;
            }
            return 1;
        }
    }

    /**
     * Alert configuration.
     */
    @Loggable
    public static final class AlertConfiguration {

        public List<Alert> getAlerts() {
            return Collections.unmodifiableList(_alerts);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", Integer.toHexString(System.identityHashCode(this)))
                    .add("Alerts", _alerts)
                    .toString();
        }

        private AlertConfiguration(final Builder builder) {
            _alerts = Lists.newArrayList(builder._alerts);
        }

        private final List<Alert> _alerts;

        /**
         * Implementation of builder pattern for <code>AlertConfiguration</code>.
         */
        public static final class Builder extends OvalBuilder<AlertConfiguration> {

            /**
             * Public constructor.
             */
            public Builder() {
                super(AlertConfiguration.class);
            }

            /**
             * The list of alerts. Required. Cannot be null.
             *
             * @param value The list of alerts.
             * @return This instance of <code>Builder</code>.
             */
            public Builder setAlerts(final List<Alert> value) {
                _alerts = value;
                return this;
            }

            @NotNull
            private List<Alert> _alerts;
        }
    }
}
