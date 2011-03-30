import java.util.*;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.*;

import tsdaggregator.AggregationSpecifier;
import tsdaggregator.TSAggregation;

public class TSAggregationTests {
	@Test
	public void TestContruct() {
		TSAggregation agg = new TSAggregation(new AggregationSpecifier(new Period(0, 5, 0, 0)));
		agg.addSample(1d, new DateTime(2011, 1, 3, 15, 23, 38, 181, DateTimeZone.UTC));
		agg.addSample(2d, new DateTime(2011, 1, 3, 15, 23, 39, 181, DateTimeZone.UTC));
		agg.addSample(3d, new DateTime(2011, 1, 3, 15, 23, 40, 181, DateTimeZone.UTC));
		agg.addSample(4d, new DateTime(2011, 1, 3, 15, 23, 41, 181, DateTimeZone.UTC));
		agg.addSample(5d, new DateTime(2011, 1, 3, 15, 23, 42, 181, DateTimeZone.UTC));
		agg.addSample(2d, new DateTime(2011, 1, 3, 15, 26, 38, 181, DateTimeZone.UTC));
	}
}
