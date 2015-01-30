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

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.scripting.Expression;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.tsdcore.statistics.CountStatistic;
import com.arpnetworking.tsdcore.statistics.SumStatistic;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

/**
 * Tests for the <code>LuaExpression</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class LuaExpressionTest {

    @Test
    public void testReturnValue() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnValue")
                .setScript("return 1\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.<AggregatedData>emptyList());
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testReturnQuantity() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnQuantity")
                .setScript("return {value = 1, unit = \"SECOND\"}\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.<AggregatedData>emptyList());
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(Unit.SECOND, result.get().getValue().getUnit().get());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testReturnQuantityNoUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnQuantityNoUnit")
                .setScript("return {value = 1}\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.<AggregatedData>emptyList());
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test(expected = ScriptingException.class)
    public void testReturnQuantityInvalidUnit() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnQuantityInvalidUnit")
                .setScript("return {value = 1, unit = \"INVALID\"}\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.<AggregatedData>emptyList());
    }

    @Test(expected = ScriptingException.class)
    public void testReturnQuantityInvalidUnitType() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnQuantityInvalidUnitType")
                .setScript("return {value = 1, unit = 2}\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.<AggregatedData>emptyList());
    }

    @Test(expected = ScriptingException.class)
    public void testReturnQuantityMissingValue() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnQuantityMissingValue")
                .setScript("return {unit = \"SECOND\"}\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.<AggregatedData>emptyList());
    }

    @Test(expected = ScriptingException.class)
    public void testReturnQuantityInvalidValueType() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testReturnQuantityInvalidValueType")
                .setScript("return {value = \"ABC\", unit = \"SECOND\"}\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.<AggregatedData>emptyList());
    }

    @Test
    public void testLuaQuantityCoercion() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityCoercion")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(Unit.SECOND, result.get().getValue().getUnit().get());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityAccessDataValue() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityAccessDataValue")
                .setScript("return {value = metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:getValue()"
                        + " * 1000, unit = \"MILLISECOND\"}\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(Unit.MILLISECOND, result.get().getValue().getUnit().get());
        Assert.assertEquals(1000.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityAccessDataUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityAccessDataUnit")
                .setScript("return {value = 0, unit = metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:getUnit()}\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(Unit.SECOND, result.get().getValue().getUnit().get());
        Assert.assertEquals(0.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityCreation() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityCreation")
                .setScript("return createQuantity(1, \"SECOND\")\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.<AggregatedData>emptyList());
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(Unit.SECOND, result.get().getValue().getUnit().get());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityCreationNilUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityCreationNilUnit")
                .setScript("return createQuantity(1, nil)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.<AggregatedData>emptyList());
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityCreationNoUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityCreationNoUnit")
                .setScript("return createQuantity(1)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.<AggregatedData>emptyList());
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityConversion() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityConversion")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:convertTo(\"SECOND\")\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.MINUTE)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(Unit.SECOND, result.get().getValue().getUnit().get());
        Assert.assertEquals(60.0, result.get().getValue().getValue(), 0.001);
    }

    @Test(expected = ScriptingException.class)
    public void testLuaQuantityConversionFromNil() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityConversionFromNil")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:convertTo(\"SECOND\")\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.singletonList(
                                new AggregatedData.Builder()
                                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                                .setCluster("MyCluster")
                                                .setService("MyService")
                                                .setMetric("MyMetric")
                                                .setStatistic(new TP99Statistic())
                                                .build())
                                        .setPeriod(new Period("PT5M"))
                                        .setValue(new Quantity(1, Optional.<Unit>absent()))
                                        .setHost("MyHost")
                                        .setStart(DateTime.now())
                                        .setPopulationSize(Long.valueOf(1))
                                        .setSamples(Collections.<Quantity>emptyList())
                                        .build()));
    }

    @Test(expected = ScriptingException.class)
    public void testLuaQuantityConversionToNil() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityConversionToNil")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:convertTo(nil)\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.singletonList(
                                new AggregatedData.Builder()
                                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                                .setCluster("MyCluster")
                                                .setService("MyService")
                                                .setMetric("MyMetric")
                                                .setStatistic(new TP99Statistic())
                                                .build())
                                        .setPeriod(new Period("PT5M"))
                                        .setValue(new Quantity(1, Optional.of(Unit.MINUTE)))
                                        .setHost("MyHost")
                                        .setStart(DateTime.now())
                                        .setPopulationSize(Long.valueOf(1))
                                        .setSamples(Collections.<Quantity>emptyList())
                                        .build()));
    }

    @Test(expected = ScriptingException.class)
    public void testLuaQuantityConversionToIncompatible() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityConversionToIncompatible")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:convertTo(\"BYTE\")\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.singletonList(
                                new AggregatedData.Builder()
                                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                                .setCluster("MyCluster")
                                                .setService("MyService")
                                                .setMetric("MyMetric")
                                                .setStatistic(new TP99Statistic())
                                                .build())
                                        .setPeriod(new Period("PT5M"))
                                        .setValue(new Quantity(1, Optional.of(Unit.MINUTE)))
                                        .setHost("MyHost")
                                        .setStart(DateTime.now())
                                        .setPopulationSize(Long.valueOf(1))
                                        .setSamples(Collections.<Quantity>emptyList())
                                        .build()));
    }

    @Test(expected = ScriptingException.class)
    public void testLuaQuantityConversionToMissingArgument() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityConversionToMissingArgument")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:convertTo()\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.singletonList(
                                new AggregatedData.Builder()
                                        .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                                .setCluster("MyCluster")
                                                .setService("MyService")
                                                .setMetric("MyMetric")
                                                .setStatistic(new TP99Statistic())
                                                .build())
                                        .setPeriod(new Period("PT5M"))
                                        .setValue(new Quantity(1, Optional.of(Unit.MINUTE)))
                                        .setHost("MyHost")
                                        .setStart(DateTime.now())
                                        .setPopulationSize(Long.valueOf(1))
                                        .setSamples(Collections.<Quantity>emptyList())
                                        .build()));
    }

    @Test
    public void testLuaQuantityComparisonLessThan() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityComparisonLessThan")
                .setScript(
                        "newQuantity = createQuantity(2, \"SECOND\")\n"
                        + "return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:compareTo(newQuantity)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(-1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityComparisonEqualTo() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityComparisonEqualTo")
                .setScript(
                        "newQuantity = createQuantity(1, \"SECOND\")\n"
                        + "return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:compareTo(newQuantity)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(0.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityComparisonGreaterThan() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityComparisonGreaterThan")
                .setScript(
                        "newQuantity = createQuantity(0, \"SECOND\")\n"
                        + "return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:compareTo(newQuantity)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityComparisonLessThanDifferentUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityComparisonLessThanDifferentUnit")
                .setScript(
                        "newQuantity = createQuantity(1, \"SECOND\")\n"
                        + "return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:compareTo(newQuantity)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.MILLISECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(-1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityComparisonEqualToDifferentUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityComparisonEqualToDifferentUnit")
                .setScript(
                        "newQuantity = createQuantity(1000, \"MILLISECOND\")\n"
                        + "return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:compareTo(newQuantity)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(0.0, result.get().getValue().getValue(), 0.001);
    }

    @Test
    public void testLuaQuantityComparisonGreaterThanDifferentUnit() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testLuaQuantityComparisonGreaterThanDifferentUnit")
                .setScript(
                        "newQuantity = createQuantity(1, \"MILLISECOND\")\n"
                        + "return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]:compareTo(newQuantity)\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new TP99Statistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().getValue().getUnit().isPresent());
        Assert.assertEquals(1.0, result.get().getValue().getValue(), 0.001);
    }

    @Test(expected = ScriptingException.class)
    public void testNoReturnStatement() throws ScriptingException {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testNoReturnStatement")
                .setScript("val = 1 + 1\n")
                .build()
                .evaluate(
                        "MyHost",
                        Period.minutes(1),
                        NOW,
                        Collections.<AggregatedData>emptyList());
    }

    @Test(expected = RuntimeException.class)
    public void testBadScript() {
        new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testBadScript")
                .setScript("this is invalid lua\n")
                .build();
    }

    @Test
    public void testDependencyDiscovery() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testDependencyDiscovery")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"tp99\"]\n")
                .build();
        final Set<FQDSN> dependencies = expression.getDependencies();
        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertTrue(dependencies.contains(new FQDSN.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(new TP99Statistic())
                .build()));
    }

    @Test
    public void testDependencyDiscoveryAlternative() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testDependencyDiscovery")
                .setScript("return metrics.MyCluster.MyService.MyMetric.tp99\n")
                .build();
        final Set<FQDSN> dependencies = expression.getDependencies();
        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertTrue(dependencies.contains(new FQDSN.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(new TP99Statistic())
                .build()));
    }

    @Test
    public void testDependencyDiscoveryMixedA() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testDependencyDiscovery")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"].MyMetric.tp99\n")
                .build();
        final Set<FQDSN> dependencies = expression.getDependencies();
        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertTrue(dependencies.contains(new FQDSN.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(new TP99Statistic())
                .build()));
    }

    @Test
    public void testDependencyDiscoveryMixedB() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testDependencyDiscovery")
                .setScript("return metrics.MyCluster.MyService[\"MyMetric\"][\"tp99\"]\n")
                .build();
        final Set<FQDSN> dependencies = expression.getDependencies();
        Assert.assertNotNull(dependencies);
        Assert.assertEquals(1, dependencies.size());
        Assert.assertTrue(dependencies.contains(new FQDSN.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(new TP99Statistic())
                .build()));
    }

    @Test
    public void testMultipleDependencyDiscovery() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testMultipleDependencyDiscovery")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"sum\"]:getValue() / "
                        + "metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"count\"]:getValue()\n")
                .build();
        final Set<FQDSN> dependencies = expression.getDependencies();
        Assert.assertNotNull(dependencies);
        Assert.assertEquals(2, dependencies.size());
        Assert.assertTrue(dependencies.contains(new FQDSN.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(new SumStatistic())
                .build()));
        Assert.assertTrue(dependencies.contains(new FQDSN.Builder()
                .setCluster("MyCluster")
                .setService("MyService")
                .setMetric("MyMetric")
                .setStatistic(new CountStatistic())
                .build()));
    }

    @Test
    public void testMissingDependency() throws ScriptingException {
        final Expression expression = new LuaExpression.Builder()
                .setCluster("MyClsuter")
                .setService("MyService")
                .setName("LuaExpressionTest.testMissingDependency")
                .setScript("return metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"sum\"]:getValue() / "
                        + "metrics[\"MyCluster\"][\"MyService\"][\"MyMetric\"][\"count\"]:getValue()\n")
                .build();
        final Optional<AggregatedData> result = expression.evaluate(
                "MyHost",
                Period.minutes(1),
                NOW,
                Collections.singletonList(
                        new AggregatedData.Builder()
                                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                                        .setCluster("MyCluster")
                                        .setService("MyService")
                                        .setMetric("MyMetric")
                                        .setStatistic(new SumStatistic())
                                        .build())
                                .setPeriod(new Period("PT5M"))
                                .setValue(new Quantity(1, Optional.of(Unit.SECOND)))
                                .setHost("MyHost")
                                .setStart(DateTime.now())
                                .setPopulationSize(Long.valueOf(1))
                                .setSamples(Collections.<Quantity>emptyList())
                                .build()));
        Assert.assertFalse(result.isPresent());
    }
    
    private static final DateTime NOW = DateTime.now();
}
