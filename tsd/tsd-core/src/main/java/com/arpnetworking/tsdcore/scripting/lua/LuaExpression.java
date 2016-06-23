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

import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.FQDSN;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.model.Unit;
import com.arpnetworking.tsdcore.scripting.Expression;
import com.arpnetworking.tsdcore.scripting.ScriptingException;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.ast.Block;
import org.luaj.vm2.ast.Chunk;
import org.luaj.vm2.ast.Exp;
import org.luaj.vm2.ast.FuncArgs;
import org.luaj.vm2.ast.FuncBody;
import org.luaj.vm2.ast.Name;
import org.luaj.vm2.ast.NameScope;
import org.luaj.vm2.ast.ParList;
import org.luaj.vm2.ast.Stat;
import org.luaj.vm2.ast.TableConstructor;
import org.luaj.vm2.ast.TableField;
import org.luaj.vm2.ast.Visitor;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.parser.LuaParser;
import org.luaj.vm2.parser.ParseException;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Defines and supports evaluation of an expression using Lua.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class LuaExpression implements Expression {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AggregatedData> evaluate(
            final String host,
            final Period period,
            final DateTime periodStart,
            final Collection<AggregatedData> data) throws ScriptingException {
        return evaluate(new PeriodicData.Builder()
                .setPeriod(period)
                .setStart(periodStart)
                .setDimensions(ImmutableMap.of("host", host))
                .setData(ImmutableList.copyOf(data))
                .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AggregatedData> evaluate(final PeriodicData periodicData) throws ScriptingException {
        // Check dependencies
        for (final FQDSN dependency : _dependencies) {
            if (!periodicData.getDatumByFqdsn(dependency).isPresent()) {
                LOGGER.debug()
                        .setMessage("Data does not contain dependency")
                        .addData("expression", this)
                        .addData("dependency", dependency)
                        .addData("periodicData", periodicData)
                        .log();
                return Optional.absent();
            }
        }

        // Evaluate the expression
        final Quantity result;
        try {
            final LuaValue arguments = new LuaTable();
            arguments.set("metrics", createMetricsTable(periodicData));
            result = convertToQuantity(_expression.call(arguments));
            // CHECKSTYLE.OFF: IllegalCatch - Lua throws RuntimeExceptions
        } catch (final ScriptingException se) {
            throw se;
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new ScriptingException(String.format("Expression evaluation failed; expression=%s", this), e);
        }

        return Optional.of(new AggregatedData.Builder()
                .setFQDSN(_fqdsn)
                .setPopulationSize(1L)
                .setSamples(Collections.singletonList(result))
                .setValue(result)
                .setIsSpecified(true)
                // TODO(vkoskela): Remove these once we move to PeriodicData [MAI-448]
                .setHost(periodicData.getDimensions().get("host"))
                .setPeriod(periodicData.getPeriod())
                .setStart(periodicData.getStart())
                .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FQDSN getTargetFQDSN() {
        return _fqdsn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<FQDSN> getDependencies() {
        return Collections.unmodifiableSet(_dependencies);
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.builder(this)
                .put("fqdsn", _fqdsn)
                .put("script", _script.replace("\n", "\\n").replace("\r", "\\r"))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    private LuaValue createMetricsTable(final PeriodicData periodicData) {
        final LuaValue table = new LuaTable();
        for (final AggregatedData datum : periodicData.getData()) {
            addToTable(datum.getFQDSN(), datum, table);
        }
        return table;
    }

    private void addToTable(final FQDSN fqdsn, final AggregatedData datum, final LuaValue table) {
        // Insert cluster
        final String safeClusterName = fqdsn.getCluster();
        LuaValue clusterTable = table.get(safeClusterName);
        if (LuaValue.NIL.equals(clusterTable)) {
            clusterTable = new LuaTable();
            table.set(safeClusterName, clusterTable);
        }

        // Insert service
        final String safeServiceName = fqdsn.getService();
        LuaValue serviceTable = clusterTable.get(safeServiceName);
        if (LuaValue.NIL.equals(serviceTable)) {
            serviceTable = new LuaTable();
            clusterTable.set(safeServiceName, serviceTable);
        }

        // Insert metric
        final String safeMetricName = fqdsn.getMetric();
        LuaValue metricTable = serviceTable.get(safeMetricName);
        if (LuaValue.NIL.equals(metricTable)) {
            metricTable = new LuaTable();
            serviceTable.set(safeMetricName, metricTable);
        }

        // Insert statistic (value)
        if (!LuaValue.NIL.equals(metricTable.get(fqdsn.getStatistic().getName()))) {
            LOGGER.warn()
                    .setMessage("Duplicate statistic found")
                    .addData("duplicate", datum)
                    .log();
        } else {
            metricTable.set(
                    fqdsn.getStatistic().getName(),
                    CoerceJavaToLua.coerce(
                            new LuaQuantity(datum.getValue())));
        }
    }

    private Quantity convertToQuantity(final LuaValue result) throws ScriptingException {
        if (result.isnumber()) {
            // Construct and return a unit-less Quantity
            return new Quantity.Builder()
                    .setValue(result.todouble())
                    .build();

        } else if (result.istable()) {
            // Extract and validate the value
            final LuaValue value = result.get("value");
            value.checknumber();

            // Extract and validate the optional unit
            final LuaValue unitName = result.get("unit");
            if (!LuaValue.NIL.equals(unitName)) {
                unitName.checkstring();
            }

            // Determine the unit
            final Optional<Unit> unit = LuaValue.NIL.equals(unitName)
                    ? Optional.<Unit>absent() : Optional.of(Unit.valueOf(unitName.tojstring()));

            // Construct and return the Quantity
            return new Quantity.Builder()
                    .setValue(result.get("value").todouble())
                    .setUnit(unit.orNull())
                    .build();
        } else if (result.isuserdata(LuaQuantity.class)) {
            // Coerce the Java instance
            @SuppressWarnings("unchecked")
            final LuaQuantity quantity = (LuaQuantity) CoerceLuaToJava.coerce(result, LuaQuantity.class);

            // Return the Quantity
            return quantity.getQuantity();
        }

        throw new ScriptingException(
                String.format("Script returned an unsupported value; result=%s", result));
    }

    private Set<FQDSN> discoverDependencies(final String script) {
        try {
            final LuaParser parser = new LuaParser(new ByteArrayInputStream(script.getBytes(Charsets.UTF_8)));
            final Chunk chunk = parser.Chunk();
            final Set<FQDSN> dependencies = Sets.newHashSet();
            final DependencyVisitor visitor = new DependencyVisitor(dependencies);
            chunk.accept(visitor);
            return dependencies;
        } catch (final ParseException e) {
            throw Throwables.propagate(e);
        }
    }

    LuaExpression(final Builder builder) {
        _script = "arguments = ...\nmetrics = arguments.metrics\n\n" + builder._script;

        _fqdsn = new FQDSN.Builder()
                .setCluster(builder._cluster)
                .setMetric(builder._metric)
                .setService(builder._service)
                .setStatistic(EXPRESSION_STATISTIC)
                .build();

        _globals = JsePlatform.standardGlobals();
        _globals.set("createQuantity", CREATE_QUANTITY_LUA_FUNCTION);
        _expression = _globals.load(_script, builder._metric);

        _dependencies = discoverDependencies(_script);
    }

    private final FQDSN _fqdsn;
    private final String _script;
    private final Globals _globals;
    private final LuaValue _expression;
    private final Set<FQDSN> _dependencies;

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic EXPRESSION_STATISTIC = STATISTIC_FACTORY.getStatistic("expression");
    private static final CreateQuantityLuaFunction CREATE_QUANTITY_LUA_FUNCTION = new CreateQuantityLuaFunction();
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaExpression.class);

    private final class DependencyVisitor extends Visitor {

        private DependencyVisitor(final Set<FQDSN> dependencies) {
            _dependencies = dependencies;
            _tokens = Lists.newArrayList();
            _isMetric = false;
        }

        @Override
        public void visit(final Exp.NameExp exp) {
            if ("metrics".equals(exp.name.name)) {
                startDependency();
            } else {
                endDependency();
            }
            super.visit(exp);
        }

        @Override
        public void visit(final Chunk chunk) {
            endDependency();
            super.visit(chunk);
        }

        @Override
        public void visit(final Block block) {
            endDependency();
            super.visit(block);
        }

        @Override
        public void visit(final Stat.Assign stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.Break breakstat) {
            endDependency();
            super.visit(breakstat);
        }

        @Override
        public void visit(final Stat.FuncCallStat stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.FuncDef stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.GenericFor stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.IfThenElse stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.LocalAssign stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.LocalFuncDef stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.NumericFor stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.RepeatUntil stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.Return stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final Stat.WhileDo stat) {
            endDependency();
            super.visit(stat);
        }

        @Override
        public void visit(final FuncBody body) {
            endDependency();
            super.visit(body);
        }

        @Override
        public void visit(final FuncArgs args) {
            endDependency();
            super.visit(args);
        }

        @Override
        public void visit(final TableField field) {
            endDependency();
            super.visit(field);
        }

        @Override
        public void visit(final Exp.AnonFuncDef exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.BinopExp exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.Constant exp) {
            if (_isMetric) {
                _tokens.add(exp.value.tojstring());
                if (_tokens.size() == 4) {
                    final FQDSN fqdsn = new FQDSN.Builder()
                            .setCluster(_tokens.get(0))
                            .setService(_tokens.get(1))
                            .setMetric(_tokens.get(2))
                            .setStatistic(STATISTIC_FACTORY.getStatistic(_tokens.get(3)))
                            .build();

                    LOGGER.debug()
                            .setMessage("Expression dependency discovered")
                            .addData("expression", LuaExpression.this)
                            .addData("dependency", fqdsn)
                            .log();

                    _dependencies.add(fqdsn);
                    endDependency();
                }
            }
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.FieldExp exp) {
            // The fields are processed from outside in and thus the call to
            // super occurs before the processing the data (to reverse the
            // stack).
            super.visit(exp);
            if (_isMetric) {
                _tokens.add(exp.name.name);
                if (_tokens.size() == 4) {
                    final FQDSN fqdsn = new FQDSN.Builder()
                            .setCluster(_tokens.get(0))
                            .setService(_tokens.get(1))
                            .setMetric(_tokens.get(2))
                            .setStatistic(STATISTIC_FACTORY.getStatistic(_tokens.get(3)))
                            .build();

                    LOGGER.debug()
                            .setMessage("Expression dependency discovered")
                            .addData("expression", LuaExpression.this)
                            .addData("dependency", fqdsn)
                            .log();

                    _dependencies.add(fqdsn);
                    endDependency();
                }
            }
        }

        @Override
        public void visit(final Exp.FuncCall exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.IndexExp exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.MethodCall exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.ParensExp exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.UnopExp exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final Exp.VarargsExp exp) {
            endDependency();
            super.visit(exp);
        }

        @Override
        public void visit(final ParList pars) {
            endDependency();
            super.visit(pars);
        }

        @Override
        public void visit(final TableConstructor table) {
            endDependency();
            super.visit(table);
        }

        @Override
        public void visit(final Name name) {
            // The fields are reprocessed here; do not reset data!
            super.visit(name);
        }

        @Override
        public void visit(final String name) {
            endDependency();
            super.visit(name);
        }

        @Override
        public void visit(final NameScope scope) {
            endDependency();
            super.visit(scope);
        }

        @Override
        public void visit(final Stat.Goto gotostat) {
            endDependency();
            super.visit(gotostat);
        }

        @Override
        public void visit(final Stat.Label label) {
            endDependency();
            super.visit(label);
        }

        private void startDependency() {
            _isMetric = true;
            _tokens.clear();
        }

        private void endDependency() {
            _isMetric = false;
            _tokens.clear();
        }

        private final Set<FQDSN> _dependencies;
        private final List<String> _tokens;
        private boolean _isMetric;
    }

    private static final class CreateQuantityLuaFunction extends TwoArgFunction {

        @Override
        public LuaValue call(final LuaValue luaValue, final LuaValue luaUnit) {
            // Convert value
            luaValue.checknumber();
            final double value = luaValue.todouble();

            // Convert unit
            Unit unit = null;
            if (!LuaValue.NIL.equals(luaUnit)) {
                luaUnit.checkstring();
                unit = Unit.valueOf(luaUnit.tojstring());
            }

            // Create LuaQuantity
            return CoerceJavaToLua.coerce(new LuaQuantity(value, unit));
        }
    }

    private static final class LuaQuantity {

        private LuaQuantity(final double value, final String unit) {
            this(value, unit == null ? null : Unit.valueOf(unit));
        }

        private LuaQuantity(final double value, final Unit unit) {
            this(new Quantity.Builder()
                    .setValue(value)
                    .setUnit(unit)
                    .build());
        }

        private LuaQuantity(final Quantity quantity) {
            _quantity = quantity;
        }

        public double getValue() {
            return _quantity.getValue();
        }

        public String getUnit() {
            return _quantity.getUnit().isPresent() ? _quantity.getUnit().get().toString() : null;
        }

        public int compareTo(final LuaQuantity other) {
            return _quantity.compareTo(other._quantity);
        }

        public LuaQuantity convertTo(final String unitName) {
            return convertTo(Unit.valueOf(unitName));
        }

        public LuaQuantity convertTo(final Unit unit) {
            return new LuaQuantity(_quantity.convertTo(unit));
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof LuaQuantity)) {
                return false;
            }
            final LuaQuantity otherLuaQuantity = (LuaQuantity) other;
            if (otherLuaQuantity._quantity.getUnit().equals(_quantity.getUnit())) {
                return Double.compare(otherLuaQuantity._quantity.getValue(), _quantity.getValue()) == 0;
            } else if (otherLuaQuantity._quantity.getUnit().isPresent() && _quantity.getUnit().isPresent()) {
                final Unit smallerUnit = _quantity.getUnit().get().getSmallerUnit(
                        otherLuaQuantity._quantity.getUnit().get());

                final double convertedValue = smallerUnit.convert(
                        _quantity.getValue(),
                        _quantity.getUnit().get());
                final double otherConvertedValue = smallerUnit.convert(
                        otherLuaQuantity._quantity.getValue(),
                        otherLuaQuantity._quantity.getUnit().get());

                return Double.compare(convertedValue, otherConvertedValue) == 0;
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (_quantity.getUnit().isPresent()) {
                final Unit smallestUnit = _quantity.getUnit().get().getSmallestUnit();
                final double convertedValue = smallestUnit.convert(
                        _quantity.getValue(),
                        _quantity.getUnit().get());
                return Objects.hash(convertedValue, smallestUnit);
            }
            return Objects.hash(_quantity.getValue());
        }

        /* package private */ Quantity getQuantity() {
            return _quantity;
        }

        private final Quantity _quantity;
    }

    /**
     * <code>Builder</code> implementation for <code>LuaExpression</code>.
     */
    public static final class Builder extends OvalBuilder<LuaExpression> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(LuaExpression.class);
        }

        /**
         * Set the metric under which the expression is to be published.
         *
         * @param value The metric name.
         * @return This <code>Builder</code> instance.
         */
        public Builder setMetric(final String value) {
            _metric = value;
            return this;
        }

        /**
         * Set the cluster under which the expression is to be published.
         *
         * @param value The cluster name.
         * @return This <code>Builder</code> instance.
         */
        public Builder setCluster(final String value) {
            _cluster = value;
            return this;
        }

        /**
         * Set the service under which the expression is to be published.
         *
         * @param value The service name.
         * @return This <code>Builder</code> instance.
         */
        public Builder setService(final String value) {
            _service = value;
            return this;
        }

        /**
         * Set the Lua script.
         *
         * @param value The Lua script.
         * @return This <code>Builder</code> instance.
         */
        public Builder setScript(final String value) {
            _script = value;
            return this;
        }

        @NotNull
        @NotEmpty
        private String _metric;
        @NotNull
        @NotEmpty
        private String _cluster;
        @NotNull
        @NotEmpty
        private String _service;
        @NotNull
        @NotEmpty
        private String _script;
    }
}
