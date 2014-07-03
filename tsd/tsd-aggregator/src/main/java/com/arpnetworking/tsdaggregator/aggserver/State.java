package com.arpnetworking.tsdaggregator.aggserver;

/**
 * Describes the state of a node or server.
 *
 * @author barp
 */
public enum State {
    Active,
    ComingOnline,
    ShuttingDown,
    Offline,
    PresumedDead
}
