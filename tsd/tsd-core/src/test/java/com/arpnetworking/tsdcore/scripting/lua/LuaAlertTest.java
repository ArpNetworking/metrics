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
package com.arpnetworking.tsdcore.scripting.lua;

import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.scripting.Alert;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Tests for the <code>LuaAlert</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class LuaAlertTest {

    @Before
    public void setUp() {
        _periodicDataBuilder = new PeriodicData.Builder()
                .setPeriod(Period.minutes(1))
                .setStart(NOW)
                .setDimensions(ImmutableMap.of("host", "MyHost"));

        _aggregatedDataBuilder = new AggregatedData.Builder()
                .setFQDSN(new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setMetric("MyMetric")
                        .setService("MyService")
                        .setStatistic(TP99_STATISTIC)
                        .build())
                .setHost("MyHost")
                .setPeriod(Period.minutes(1))
                .setStart(NOW)
                .setIsSpecified(true)
                .setPopulationSize(1L)
                .setSamples(Collections.<Quantity>emptyList());
    }

    @Test
    public void testAlertMissing() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertMissing")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder.build());
        Assert.assertEquals("testAlertMissing", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertFalse(result.isTriggered().isPresent());
    }

    @Test
    public void testAlertTrueWithEquals() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertTrueWithEquals")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertTrueWithEquals", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    @Test
    public void testAlertFalseWithEquals() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertFalseWithEquals")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(0d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertFalseWithEquals", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertFalse(result.isTriggered().get());
    }

    @Test
    public void testAlertTrueWithNotEquals() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertTrueWithNotEquals")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.NOT_EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(0d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertTrueWithNotEquals", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    @Test
    public void testAlertFalseWithNotEquals() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertFalseWithNotEquals")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.NOT_EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertFalseWithNotEquals", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertFalse(result.isTriggered().get());
    }

    @Test
    public void testAlertTrueWithLessThan() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertTrueWithLessThan")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.LESS_THAN)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(0d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertTrueWithLessThan", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    @Test
    public void testAlertFalseLessThan() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertFalseLessThan")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.LESS_THAN)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(2d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertFalseLessThan", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertFalse(result.isTriggered().get());
    }

    @Test
    public void testAlertTrueWithLessThanOrEqual() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertTrueWithLessThanOrEqual")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.LESS_THAN_OR_EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertTrueWithLessThanOrEqual", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    @Test
    public void testAlertFalseLessThanOrEqual() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertFalseLessThanOrEqual")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.LESS_THAN_OR_EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(2d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertFalseLessThanOrEqual", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertFalse(result.isTriggered().get());
    }

    @Test
    public void testAlertTrueWithGreaterThan() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertTrueWithGreaterThan")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.GREATER_THAN)
                .setValue(new Quantity.Builder().setValue(0d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertTrueWithGreaterThan", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(0d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    @Test
    public void testAlertFalseGreaterThan() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertFalseGreaterThan")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.GREATER_THAN)
                .setValue(new Quantity.Builder().setValue(2d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertFalseGreaterThan", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(2d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertFalse(result.isTriggered().get());
    }

    @Test
    public void testAlertTrueWithGreaterThanOrEqual() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertTrueWithGreaterThanOrEqual")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.GREATER_THAN_OR_EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertTrueWithGreaterThanOrEqual", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(1d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    @Test
    public void testAlertFalseGreaterThanOrEqual() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testAlertFalseGreaterThanOrEqual")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.GREATER_THAN_OR_EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(2d).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
        Assert.assertEquals("testAlertFalseGreaterThanOrEqual", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(new Quantity.Builder().setValue(2d).build(), result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertFalse(result.isTriggered().get());
    }

    @Test(expected = ScriptingException.class)
    public void testMissingValueUnit() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testMissingValueUnit")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.SECOND).build())
                .build();
        alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).build())
                        .build()))
                .build());
    }

    @Test(expected = ScriptingException.class)
    public void testMissingAlertUnit() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testMissingAlertUnit")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).build())
                .build();
        alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.SECOND).build())
                        .build()))
                .build());
    }

    @Test(expected = ScriptingException.class)
    public void testInvalidUnitConversion() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testInvalidUnitConversion")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.MEGABIT).build())
                .build();
        alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.SECOND).build())
                        .build()))
                .build());
    }

    @Test
    public void testUnitConversion() throws ScriptingException {
        final Alert alert = new LuaAlert.Builder()
                .setName("testUnitConversion")
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(TP99_STATISTIC)
                .setPeriod(Period.minutes(1))
                .setExtensions(ImmutableMap.of("severity", "critical"))
                .setOperator(LuaRelationalOperator.EQUAL_TO)
                .setValue(new Quantity.Builder().setValue(1d).setUnit(Unit.MINUTE).build())
                .build();
        final Condition result = alert.evaluate(_periodicDataBuilder
                .setData(ImmutableList.of(_aggregatedDataBuilder
                        .setValue(new Quantity.Builder().setValue(60d).setUnit(Unit.SECOND).build())
                        .build()))
                .build());
        Assert.assertEquals("testUnitConversion", result.getName());
        Assert.assertEquals("critical", result.getExtensions().get("severity"));
        Assert.assertEquals(
                new Quantity.Builder()
                        .setValue(1d)
                        .setUnit(Unit.MINUTE)
                        .build(),
                result.getThreshold());
        Assert.assertEquals(
                new FQDSN.Builder()
                        .setCluster("MyCluster")
                        .setService("MyService")
                        .setMetric("MyMetric")
                        .setStatistic(TP99_STATISTIC)
                        .build(),
                result.getFQDSN());
        Assert.assertTrue(result.isTriggered().isPresent());
        Assert.assertTrue(result.isTriggered().get());
    }

    private PeriodicData.Builder _periodicDataBuilder;
    private AggregatedData.Builder _aggregatedDataBuilder;

    private static final DateTime NOW = DateTime.now();
    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic TP99_STATISTIC = STATISTIC_FACTORY.getStatistic("tp99");
}
