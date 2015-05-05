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
package com.arpnetworking.remet.gui.expressions.impl;

import com.arpnetworking.configuration.jackson.DynamicConfiguration;
import com.arpnetworking.configuration.jackson.JsonNodeUrlSource;
import com.arpnetworking.configuration.triggers.UrlTrigger;
import com.arpnetworking.jackson.BuilderDeserializer;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.expressions.Expression;
import com.arpnetworking.remet.gui.expressions.ExpressionQuery;
import com.arpnetworking.remet.gui.expressions.ExpressionRepository;
import com.arpnetworking.remet.gui.impl.DefaultQueryResult;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Implementation of expression repository using dynamic configuration.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class DynamicConfigurationExpressionRepository
        implements ExpressionRepository, Relaunchable<DynamicConfigurationExpressionRepository.ExpressionConfiguration> {

    /**
     * Public constructor.
     *
     * @param playConfiguration Instance of Play's <code>Configuration</code>.
     */
    @Inject
    public DynamicConfigurationExpressionRepository(final Configuration playConfiguration) {
        try {
            _configurationUrl = new URL(playConfiguration.getString("expressionRepository.url"));
        } catch (final MalformedURLException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        assertIsOpen(false);
        LOGGER.debug().setMessage("Opening expression repository").log();
        final Reconfigurator<DynamicConfigurationExpressionRepository, ExpressionConfiguration> reconfigurator =
                new Reconfigurator<>(this, ExpressionConfiguration.class);
        _dynamicConfiguration = new DynamicConfiguration.Builder()
                .setObjectMapper(OBJECT_MAPPER)
                .addSourceBuilder(new JsonNodeUrlSource.Builder()
                        .setObjectMapper(OBJECT_MAPPER)
                        .setUrl(_configurationUrl))
                .addTrigger(new UrlTrigger.Builder()
                        .setUrl(_configurationUrl)
                        .build())
                .addListener(reconfigurator)
                .build();

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
        LOGGER.debug().setMessage("Closing expression repository").log();
        _dynamicConfiguration.shutdown();
        _isOpen.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Expression> get(final UUID identifier) {
        return _expressions.stream()
                .filter(expression -> identifier.equals(expression.getId()))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionQuery createQuery() {
        return new DefaultExpressionQuery(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<Expression> query(final ExpressionQuery query) {
        assertIsOpen();
        final List<? extends Expression> expressions = _expressions.stream()
                .filter(expression -> query.getCluster().map(cluster -> cluster.equals(expression.getCluster())).orElse(true))
                .filter(expression -> query.getService().map(service -> service.equals(expression.getService())).orElse(true))
                .sorted(EXPRESSION_COMPARATOR)
                .collect(Collectors.toList());
        final int start = query.getOffset().orElse(0);
        final int end = start + Math.max(Math.min(expressions.size() - start, query.getLimit()), 0);
        return new DefaultQueryResult<>(expressions.subList(start, end), expressions.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExpressionCount() {
        assertIsOpen();
        return _expressions.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relaunch(final ExpressionConfiguration expressionConfiguration) {
        LOGGER.info()
                .setMessage("Relaunching expression repository")
                .addData("configuration", expressionConfiguration)
                .log();

        final List<Expression> newExpressions = Lists.newArrayList(expressionConfiguration.getExpressions());
        newExpressions.sort(EXPRESSION_COMPARATOR);

        LOGGER.info()
                .setMessage("Loaded expressions")
                .addData("newExpressionsSize", newExpressions.size())
                .addData("previousExpressionsSize", _expressions.size())
                .log();

        _expressions = newExpressions;
        _isOpen.set(true);
    }

    private void assertIsOpen() {
        assertIsOpen(true);
    }

    private void assertIsOpen(final boolean expectedState) {
        if (_isOpen.get() != expectedState) {
            throw new IllegalStateException(String.format("Expression repository is not %s", expectedState ? "open" : "closed"));
        }
    }

    private final AtomicBoolean _isOpen = new AtomicBoolean(false);
    private final URL _configurationUrl;

    private DynamicConfiguration _dynamicConfiguration;
    private volatile List<Expression> _expressions = Collections.emptyList();

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationExpressionRepository.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createInstance();
    private static final Comparator<Expression> EXPRESSION_COMPARATOR = new ExpressionComparator();

    static {
        final SimpleModule module = new SimpleModule("DynamicConfigurationExpressionRepository");
        BuilderDeserializer.addTo(module, ExpressionConfiguration.class);
        BuilderDeserializer.addTo(module, DefaultExpression.class);
        module.addDeserializer(Expression.class, BuilderDeserializer.of(DefaultExpression.Builder.class));
        OBJECT_MAPPER.registerModules(module);
    }

    private static final class ExpressionComparator implements Comparator<Expression> {
        @Override
        public int compare(final Expression e1, final Expression e2) {
            int result = e1.getCluster().compareToIgnoreCase(e2.getCluster());
            if (result != 0) {
                return result;
            }
            result = e1.getService().compareToIgnoreCase(e2.getService());
            if (result != 0) {
                return result;
            }
            result = e1.getMetric().compareToIgnoreCase(e2.getMetric());
            if (result != 0) {
                return result;
            }
            LOGGER.warn()
                    .setMessage("Duplicate expression detected")
                    .addData("expression1", e1)
                    .addData("expression2", e2)
                    .log();
            return e1.getId().compareTo(e2.getId());
        }
    }

    /**
     * Expression configuration.
     */
    public static final class ExpressionConfiguration {

        public List<Expression> getExpressions() {
            return Collections.unmodifiableList(_expressions);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", Integer.toHexString(System.identityHashCode(this)))
                    .add("Expressions", _expressions)
                    .toString();
        }

        private ExpressionConfiguration(final Builder builder) {
            _expressions = Lists.newArrayList(builder._expressions);
        }

        private final List<Expression> _expressions;

        /**
         * Implementation of builder pattern for <code>ExpressionConfiguration</code>.
         */
        public static final class Builder extends OvalBuilder<ExpressionConfiguration> {

            /**
             * Public constructor.
             */
            public Builder() {
                super(ExpressionConfiguration.class);
            }

            /**
             * The list of expressions. Required. Cannot be null.
             *
             * @param value The list of expressions.
             * @return This instance of <code>Builder</code>.
             */
            public Builder setExpressions(final List<Expression> value) {
                _expressions = value;
                return this;
            }

            @NotNull
            private List<Expression> _expressions;
        }
    }
}
