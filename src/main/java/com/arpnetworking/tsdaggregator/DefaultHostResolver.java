package com.arpnetworking.tsdaggregator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves a host name using the built-in java functions.
 *
 * @author barp
 */
public class DefaultHostResolver implements HostResolver {
    @Override
    public String getLocalHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}
