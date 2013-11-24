package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Stores the information related to a known agg server.
 *
 * @author barp
 */
public class AggServerStatus {
    private final String _name;
    private State _state;
    private DateTime _heartbeatTime;

    public AggServerStatus(final String name, final State state, final DateTime heartbeatTime) {
        _name = name;
        _state = state;
        _heartbeatTime = heartbeatTime;
    }

    public String getName() {
        return _name;
    }

    public State getState() {
        return _state;
    }

    public void setState(final State state) {
        _state = state;
    }

    public DateTime getHeartbeatTime() {
        return _heartbeatTime;
    }

    public void setHeartbeatTime(final DateTime heartbeatTime) {
        _heartbeatTime = heartbeatTime;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        @Nonnull final AggServerStatus that = (AggServerStatus) o;

        if (!_name.equals(that._name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("_name", _name).add("_state", _state)
                .add("_heartbeatTime", _heartbeatTime).toString();
    }
}
