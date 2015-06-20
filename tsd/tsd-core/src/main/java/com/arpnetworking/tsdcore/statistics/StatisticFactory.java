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
package com.arpnetworking.tsdcore.statistics;

import com.arpnetworking.utility.InterfaceDatabase;
import com.arpnetworking.utility.ReflectionsDatabase;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

/**
 * Creates statistics.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StatisticFactory {

    /**
     * Creates a statistic from a name.
     *
     * @param statistic The name of the desired statistic.
     * @return A new <code>Statistic</code>.
     */
    public Optional<Statistic> createStatistic(final String statistic) {
        return Optional.fromNullable(STATISTICS.get(statistic));
    }

    private static void checkedPut(final Map<String, Statistic> map, final Statistic statistic, final String key) {
        final Statistic existingStatistic =  map.get(key);
        if (existingStatistic != null) {
            if (!existingStatistic.equals(statistic)) {
                LOGGER.error(String.format(
                        "Statistic already registered; key=%s, existing=%s, new=%s",
                        key,
                        existingStatistic,
                        statistic));
            }
            return;
        }
        map.put(key, statistic);
    }

    private static final Map<String, Statistic> STATISTICS = Maps.newHashMap();
    private static final InterfaceDatabase INTERFACE_DATABASE = ReflectionsDatabase.newInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticFactory.class);

    static {
        final Set<Class<? extends Statistic>> statisticClasses = INTERFACE_DATABASE.findClassesWithInterface(Statistic.class);
        for (final Class<? extends Statistic> statisticClass : statisticClasses) {
            LOGGER.debug(String.format("Considering statistic; class=%s", statisticClass));
            if (!statisticClass.isInterface() && !Modifier.isAbstract(statisticClass.getModifiers())) {
                try {
                    final Statistic statistic = statisticClass.newInstance();
                    checkedPut(STATISTICS, statistic, statistic.getName());
                    for (final String alias : statistic.getAliases()) {
                        checkedPut(STATISTICS, statistic, alias);
                    }
                    LOGGER.info(String.format(
                            "Registered statistic; name=%s, aliases=%s, class=%s",
                            statistic.getName(),
                            statistic.getAliases(),
                            statisticClass));
                    // CHECKSTYLE.OFF: IllegalCatch - All exceptions fail.
                } catch (final Exception e) {
                    // CHECKSTYLE.ON: IllegalCatch
                    LOGGER.warn(String.format("Unable to load statistic; class=%s", statisticClass), e);
                }
            }
        }
    }
}
