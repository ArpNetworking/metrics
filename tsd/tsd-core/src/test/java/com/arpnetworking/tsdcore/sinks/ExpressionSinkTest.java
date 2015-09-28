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
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.configuration.Configuration;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.scripting.Expression;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hamcrest.collection.IsEmptyIterable;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for the <code>ExpressionSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class ExpressionSinkTest {

    @Before
    public void setup() {
        _expressions = new AtomicReference<>();
        _listener = new ExpressionSink.ConfigurationListener(_expressions);
    }

    @Test
    public void testDependencyResolutionNoExpressions() throws Exception {
        final Configuration configuration = createConfiguration();

        _listener.offerConfiguration(configuration);
        _listener.applyConfiguration();

        Assert.assertNotNull(_expressions.get());
        Assert.assertThat(_expressions.get(), IsEmptyIterable.emptyIterable());
    }

    @Test
    public void testDependencyResolutionSingleExpressionWithNoDependencies() throws Exception {
        final Expression a = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-a")
                        .setMetric("m-a")
                        .setService("s-a")
                        .setStatistic(STATISTIC)
                        .build());

        assertApply(createConfiguration(a), sequence(a));
    }

    @Test
    public void testDependencyResolutionTwoExpressionWithNoDependencies() throws Exception {
        final Expression a = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-a")
                        .setMetric("m-a")
                        .setService("s-a")
                        .setStatistic(STATISTIC)
                        .build());
        final Expression b = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-b")
                        .setMetric("m-b")
                        .setService("s-b")
                        .setStatistic(STATISTIC)
                        .build());

        assertApply(createConfiguration(a, b), sequence(a), sequence(b));
        assertApply(createConfiguration(b, a), sequence(a), sequence(b));
    }

    @Test
    public void testDependencyResolutionTwoDependentExpressions() throws Exception {
        final Expression a = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-a")
                        .setMetric("m-a")
                        .setService("s-a")
                        .setStatistic(STATISTIC)
                        .build());
        final Expression b = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-b")
                        .setMetric("m-b")
                        .setService("s-b")
                        .setStatistic(STATISTIC)
                        .build(),
                a.getTargetFQDSN());

        assertApply(createConfiguration(a, b), sequence(a, b));
        assertApply(createConfiguration(b, a), sequence(a, b));
    }

    @Test
    public void testDependencyResolutionSharedDependentExpressions() throws Exception {
        final Expression a = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-a")
                        .setMetric("m-a")
                        .setService("s-a")
                        .setStatistic(STATISTIC)
                        .build());
        final Expression b = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-b")
                        .setMetric("m-b")
                        .setService("s-b")
                        .setStatistic(STATISTIC)
                        .build(),
                a.getTargetFQDSN());
        final Expression c = new TestExpression(
                new FQDSN.Builder()
                        .setCluster("c-c")
                        .setMetric("m-c")
                        .setService("s-c")
                        .setStatistic(STATISTIC)
                        .build(),
                a.getTargetFQDSN(),
                b.getTargetFQDSN());

        assertApply(createConfiguration(a, b, c), sequence(a, b, c));
        assertApply(createConfiguration(b, a, c), sequence(a, b, c));
        assertApply(createConfiguration(b, c, a), sequence(a, b, c));
        assertApply(createConfiguration(a, c, b), sequence(a, b, c));
        assertApply(createConfiguration(c, a, b), sequence(a, b, c));
        assertApply(createConfiguration(c, b, a), sequence(a, b, c));
    }

    @Test
    public void testDependencyResolutionNameCollision() throws Exception {
        final FQDSN fqdsn = new FQDSN.Builder()
                .setCluster("c-a")
                .setMetric("m-a")
                .setService("s-a")
                .setStatistic(STATISTIC)
                .build();
        final Expression a = new TestExpression(fqdsn);
        final Expression b = new TestExpression(fqdsn);

        assertException(createConfiguration(a, b));
        assertException(createConfiguration(b, a));
    }

    @Test
    public void testDependencyResolutionSelfReference() throws Exception {
        final FQDSN fqdsn = new FQDSN.Builder()
                .setCluster("c-a")
                .setMetric("m-a")
                .setService("s-a")
                .setStatistic(STATISTIC)
                .build();
        final Expression a = new TestExpression(fqdsn, fqdsn);

        assertException(createConfiguration(a));
    }

    @Test
    public void testDependencyResolutionTwoExpressionCycle() throws Exception {
        final FQDSN fqdsnA = new FQDSN.Builder()
                .setCluster("c-a")
                .setMetric("m-a")
                .setService("s-a")
                .setStatistic(STATISTIC)
                .build();
        final FQDSN fqdsnB = new FQDSN.Builder()
                .setCluster("c-b")
                .setMetric("m-b")
                .setService("s-b")
                .setStatistic(STATISTIC)
                .build();
        final Expression a = new TestExpression(fqdsnA, fqdsnB);
        final Expression b = new TestExpression(fqdsnB, fqdsnA);

        assertException(createConfiguration(a, b));
        assertException(createConfiguration(b, a));
    }

    @Test
    public void testDependencyResolutionPartialCycle() throws Exception {
        final FQDSN fqdsnA = new FQDSN.Builder()
                .setCluster("c-a")
                .setMetric("m-a")
                .setService("s-a")
                .setStatistic(STATISTIC)
                .build();
        final FQDSN fqdsnB = new FQDSN.Builder()
                .setCluster("c-b")
                .setMetric("m-b")
                .setService("s-b")
                .setStatistic(STATISTIC)
                .build();
        final FQDSN fqdsnC = new FQDSN.Builder()
                .setCluster("c-c")
                .setMetric("m-c")
                .setService("s-c")
                .setStatistic(STATISTIC)
                .build();
        final Expression a = new TestExpression(fqdsnA, fqdsnB, fqdsnC);
        final Expression b = new TestExpression(fqdsnB);
        final Expression c = new TestExpression(fqdsnA);

        assertException(createConfiguration(a, b, c));
        assertException(createConfiguration(b, a, c));
        assertException(createConfiguration(b, c, a));
        assertException(createConfiguration(a, c, b));
        assertException(createConfiguration(c, a, b));
        assertException(createConfiguration(c, b, a));
    }

    private Configuration createConfiguration(final Expression... expressions) {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final List<Expression> result = Lists.newArrayList();
        for (final Expression expression : expressions) {
            result.add(expression);
        }
        Mockito.doReturn(result).when(configuration).getAs(
                Matchers.any(ParameterizedType.class),
                Matchers.any(Map.class));
        return configuration;
    }

    private List<Expression> sequence(final Expression... sequence) {
        return Arrays.asList(sequence);
    }

    private void assertApply(final Configuration configuration, final List<Expression>... expectedSequences) throws Exception {
        _listener.offerConfiguration(configuration);
        _listener.applyConfiguration();

        Assert.assertNotNull(_expressions.get());
        for (final List<Expression> expectedSequence : expectedSequences) {
            final Iterator<Expression> iterator = _expressions.get().iterator();
            for (final Expression expectedExpression : expectedSequence) {
                boolean found = false;
                while (iterator.hasNext()) {
                    if (iterator.next().equals(expectedExpression)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Assert.fail(String.format(
                            "Expected sequence not found; sequence=%s, expressions=%s",
                            expectedSequence,
                            _expressions.get()));
                }
            }
        }
    }

    private void assertException(final Configuration configuration) throws Exception {
        try {
            _listener.offerConfiguration(configuration);
            Assert.fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException iae) {
            // Expected exception caught
        }
    }

    private ExpressionSink.ConfigurationListener _listener;
    private AtomicReference<List<Expression>> _expressions;

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic STATISTIC = STATISTIC_FACTORY.getStatistic("expression");

    private static final class TestExpression implements Expression {

        public TestExpression(final FQDSN target, final FQDSN... dependencies) {
            _target = target;
            _dependencies = Sets.newHashSet(dependencies);
        }

        @Override
        public Optional<AggregatedData> evaluate(
                final String host,
                final Period period,
                final DateTime start,
                final Collection<AggregatedData> data) throws ScriptingException {
            throw new UnsupportedOperationException("Operation not supported");
        }

        @Override
        public Optional<AggregatedData> evaluate(final PeriodicData periodicData) throws ScriptingException {
            throw new UnsupportedOperationException("Operation not supported");
        }

        @Override
        public FQDSN getTargetFQDSN() {
            return _target;
        }

        @Override
        public Set<FQDSN> getDependencies() {
            return Collections.unmodifiableSet(_dependencies);
        }

        private final FQDSN _target;
        private final Set<FQDSN> _dependencies;
    }
}
