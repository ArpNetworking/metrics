package com.arpnetworking.tsdaggregator;

import java.net.UnknownHostException;

/**
 * Used to get the local hostname.
 *
 * @author barp
 */
public interface HostResolver {
    String getLocalHostName() throws UnknownHostException;
}
