package com.arpnetworking.tsdaggregator.aggserver;

/**
 * Description goes here
 *
 * @author barp
 */
public enum KetamaLayers {
    REDIS("redis"),
    AGG("agg");

    private final String _val;

    KetamaLayers(String val) {
        _val = val;
    }

    public String getVal() {
        return _val;
    }
}
