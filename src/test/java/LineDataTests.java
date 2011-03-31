import java.util.Map;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.junit.Test;

import tsdaggregator.LineData;


public class LineDataTests {
	@Test
	public void ConstructTest()	{
		LineData data = new LineData();
		Assert.assertNotNull(data);
	}
	
	@Test
	public void ParseSingleEntry() {
		LineData data = new LineData();
		data.parseLogLine("[initTimestamp=1300976184.02,set/view=26383.8768005,]");
		Map<String, Double> map = data.getVariables();
		Assert.assertEquals(1, map.size());
		Assert.assertEquals(26383.8768005, map.get("set/view"));
		DateTime timestamp = data.getTime();
		Assert.assertEquals(new DateTime((long)(1300976184.02d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}
	
	@Test
	public void ParseMultipleEntries() {
		LineData data = new LineData();
		data.parseLogLine("[initTimestamp=1300976164.85,passport/signin=395539.999008,passport/_signinValidatePassword=390913.009644,passport/signinValid=1,]");
		Map<String, Double> map = data.getVariables();
		Assert.assertEquals(3, map.size());
		Assert.assertEquals(1d, map.get("passport/signinValid"));
		DateTime timestamp = data.getTime();
		Assert.assertEquals(new DateTime((long)(1300976164.85d * 1000d), ISOChronology.getInstanceUTC()), timestamp);
	}
}
