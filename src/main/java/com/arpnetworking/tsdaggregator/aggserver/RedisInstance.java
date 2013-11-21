package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Objects;

/**
* Description goes here
*
* @author barp
*/
class RedisInstance implements Comparable<RedisInstance> {
    private final String _hostName;
    private final String _ebName;

    public RedisInstance(final String hostname, final String ebName) {
        _hostName = hostname;
        _ebName = ebName;
    }

    public String getHostName() {
        return _hostName;
    }

    public String getEBName() {
        return _ebName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RedisInstance that = (RedisInstance) o;

        if (_ebName != null ? !_ebName.equals(that._ebName) : that._ebName != null) {
            return false;
        }
        if (_hostName != null ? !_hostName.equals(that._hostName) : that._hostName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = _hostName != null ? _hostName.hashCode() : 0;
        result = 31 * result + (_ebName != null ? _ebName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("_hostName", _hostName)
                .add("_ebName", _ebName)
                .toString();
    }


    @Override
    public int compareTo(final RedisInstance o) {
        if (!_hostName.equals(o._hostName)) {
            return _hostName.compareTo(o._hostName);
        } else {
            return _ebName.compareTo(o._ebName);
        }
    }
}
