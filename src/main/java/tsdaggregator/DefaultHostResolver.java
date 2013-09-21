package tsdaggregator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Description goes here
 *
 * @author barp
 */
public class DefaultHostResolver implements HostResolver {
	@Override
	public String getLocalHostName() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName();
	}
}
