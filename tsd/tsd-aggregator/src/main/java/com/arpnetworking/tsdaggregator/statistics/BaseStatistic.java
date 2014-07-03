package com.arpnetworking.tsdaggregator.statistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A statistic base class.
 *
 * @author barp
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseStatistic implements Statistic {
    @Nonnull
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || !(o == null || getClass() != o.getClass());
    }
}
