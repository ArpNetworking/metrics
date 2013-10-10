package com.arpnetworking.tsdaggregator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import com.arpnetworking.tsdaggregator.statistics.MeanStatistic;
import com.arpnetworking.tsdaggregator.statistics.SumStatistic;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

/**
 * Tests for the AggregatedData class
 *
 * @author barp
 */
@SuppressWarnings(value = "unchecked")
public class AggregatedDataTests {
	@Test
	public void testConstruct() {
		final SumStatistic statistic = new SumStatistic();
		final String serviceName = "service_name";
		final String host = "host";
		final String metric = "set/view";
		final double value = 2332d;
		final DateTime periodStart = new DateTime(2013, 9, 20, 8, 15, 0, 0);
		final Period period = Period.minutes(5);
		AggregatedData data = new AggregatedData(statistic, serviceName, host, metric, value, periodStart, period);

		assertThat(data.getStatistic(), (Matcher)sameInstance(statistic));
		assertThat(data.getService(), equalTo(serviceName));
		assertThat(data.getHost(), equalTo(host));
		assertThat(data.getMetric(), equalTo(metric));
		assertThat(data.getValue(), equalTo(value));
		assertThat(data.getPeriodStart(), equalTo(periodStart));
		assertThat(data.getPeriod(), equalTo(period));
	}

	@Test
	public void testEquals() {
		final SumStatistic statistic = new SumStatistic();
		final String serviceName = "service_name";
		final String host = "host";
		final String metric = "set/view";
		final double value = 2332d;
		final DateTime periodStart = new DateTime(2013, 9, 20, 8, 15, 0, 0);
		final Period period = Period.minutes(5);
		AggregatedData data = new AggregatedData(statistic, serviceName, host, metric, value, periodStart, period);

		final SumStatistic otherStatistic = new SumStatistic();
		final String otherServiceName = "service_name";
		final String otherHost = "host";
		final String otherMetric = "set/view";
		final double otherValue = 2332d;
		final DateTime otherPeriodStart = new DateTime(2013, 9, 20, 8, 15, 0, 0);
		final Period otherPeriod = Period.minutes(5);
		AggregatedData otherData = new AggregatedData(otherStatistic, otherServiceName, otherHost, otherMetric, otherValue, otherPeriodStart, otherPeriod);

        //noinspection ObjectEqualsNull
        assertThat(data.equals(null), equalTo(false));
        assertThat(data.equals(data), equalTo(true));
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(data.equals(new MeanStatistic()), equalTo(false));
		assertThat(otherData.equals(data), equalTo(true));
        assertThat(otherData.hashCode(), equalTo(data.hashCode()));
		assertThat(otherData, not(sameInstance(data)));

		otherData = new AggregatedData(new MeanStatistic(), otherServiceName, otherHost, otherMetric, otherValue, otherPeriodStart, otherPeriod);
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));

		otherData = new AggregatedData(otherStatistic, "service_two", otherHost, otherMetric, otherValue, otherPeriodStart, otherPeriod);
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));

		otherData = new AggregatedData(otherStatistic, otherServiceName, "another_host", otherMetric, otherValue, otherPeriodStart, otherPeriod);
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));

		otherData = new AggregatedData(otherStatistic, otherServiceName, otherHost, "another/metric", otherValue, otherPeriodStart, otherPeriod);
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));

		otherData = new AggregatedData(otherStatistic, otherServiceName, otherHost, otherMetric, 1891d, otherPeriodStart, otherPeriod);
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));

		otherData = new AggregatedData(otherStatistic, otherServiceName, otherHost, otherMetric, otherValue, new DateTime(), otherPeriod);
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));

		otherData = new AggregatedData(otherStatistic, otherServiceName, otherHost, otherMetric, otherValue, otherPeriodStart, Period.seconds(12));
		assertThat(otherData.equals(data), equalTo(false));
        assertThat(otherData.hashCode(), not(equalTo(data.hashCode())));
	}

    @Test
    public void testToString() {
        final SumStatistic statistic = new SumStatistic();
        final String serviceName = "service_name";
        final String host = "host";
        final String metric = "set/view";
        final double value = 2332d;
        final DateTime periodStart = new DateTime(2013, 9, 20, 8, 15, 0, 0);
        final Period period = Period.minutes(5);
        AggregatedData data = new AggregatedData(statistic, serviceName, host, metric, value, periodStart, period);
        assertThat(data.toString(), startsWith("AggregatedData{"));
    }


}
