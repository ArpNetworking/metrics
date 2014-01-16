package com.arpnetworking.tsdaggregator.publishing;

import com.arpnetworking.tsdaggregator.AggregatedData;
import com.arpnetworking.tsdaggregator.statistics.MeanStatistic;
import com.arpnetworking.tsdaggregator.statistics.SumStatistic;
import com.google.common.collect.Maps;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import javax.annotation.Nonnull;

/**
 * Tests for the MonitordPublisher class
 *
 * @author barp
 */
public class MonitordPublisherTests {
	@Test
	public void testConstruct() {
		@SuppressWarnings("UnusedAssignment") MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host");
	}

	@Test
	public void testEmptyAggregationDoesNotPost() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		context.checking(new Expectations() {{
			try {
				never(client).execute(with(any(HttpUriRequest.class)));
			} catch (IOException ignored) { }
		}});

		AggregatedData[] data = new AggregatedData[0];
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	@Test
	public void testShortPeriodDoesNotPost() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		context.checking(new Expectations() {{
			try {
				never(client).execute(with(any(HttpUriRequest.class)));
			} catch (IOException ignored) { }
		}});

		AggregatedData[] data = new AggregatedData[1];
		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.seconds(1), new Double[]{});
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	@Test
	public void testClose() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);
		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		publisher.close();
	}

	@Test
	public void testExceptionOnRequest() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		context.checking(new Expectations() {{
			try {
				one(client).execute(with(any(HttpUriRequest.class))); will(throwException(new ClientProtocolException("big bad error")));
			} catch (IOException ignored) {
			}
		}});

		AggregatedData[] data = new AggregatedData[1];
		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.minutes(5), new Double[]{});
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	@Test
	public void testMultipleAggregationValuesPostsOnce() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);
		final String monitordHost = "uri";
		final String monitordPath = "/suffix";
		final String uri = "http://" + monitordHost + monitordPath;
		final String serviceName = "service_name";
		final String metric = "set/view";
		final String host = "host";
		final String cluster = "cluster";
		final Period period = Period.minutes(5);

		AggregatedData[] data = new AggregatedData[2];
		data[0] = new AggregatedData(new SumStatistic(), serviceName, host, metric, 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), period, new Double[]{});
		data[1] = new AggregatedData(new MeanStatistic(), serviceName, host, metric, 1007d, new DateTime(2013, 9, 20, 8, 15, 0, 0), period, new Double[]{});

		MonitordPublisher publisher = new MonitordPublisher(uri, cluster, host, client);

		mockResponse(context, client, 200, "OK", new BaseMatcher<HttpUriRequest>() {
			@Override
			public boolean matches(Object item) {
				@SuppressWarnings(value = "unchecked")
				HttpPost request = (HttpPost)item;
				if (!"POST".equals(request.getMethod())) {
					return false;
				}
				URI uri = request.getURI();
				if (!uri.getHost().equals(monitordHost)) {
					return false;
				}
				if (!uri.getPath().equals(monitordPath)) {
					return false;
				}
				HttpEntity entity = request.getEntity();
				try {
					final String content = IOUtils.toString(entity.getContent());
					String decoded = URLDecoder.decode(content, "utf-8");
					String[] split = decoded.split("&");
					HashMap<String, String> mapped = Maps.newHashMap();
					for (String s : split) {
						String[] vals = s.split("=");
						if (!vals[0].equals("output")) {
							mapped.put(vals[0], vals[1]);
						} else {
							mapped.put("output", s.substring(7));
						}
					}
					String monitor = mapped.get("monitor");
					String expectedMonitor = serviceName + "_" + period.toString() + "_" + metric;
					if (!monitor.equals(expectedMonitor)) {
						return false;
					}
					String runEvery = mapped.get("run_every");
					if (!runEvery.equals("300")) {
						return false;
					}
					String path = mapped.get("path");
					if (!path.equals(cluster + "/" + host)) {
						return false;
					}
					String output = mapped.get("output");
					String fullPath = output.split("\\|")[0];
					String values = output.split("\\|")[1];
					if (!fullPath.equals(expectedMonitor)) {
						return false;
					}
					for (String valPair : values.split(";")) {
						String key = valPair.split("=")[0];
						String value = valPair.split("=")[1];
						if (key.equals("sum") && !value.equals("2332.0")) {
							return false;
						}
						if (key.equals("mean") && !value.equals("1007.0")) {
							return false;
						}
					}
				} catch (IOException e) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(@Nonnull Description description) {
				description.appendText("checks for proper monitord request formatting");
			}
		});

		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	@Test
	public void testRecordSingleAggregation() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		mockResponse(context, client, 200, "OK", Matchers.any(HttpUriRequest.class));

		AggregatedData[] data = new AggregatedData[1];
		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.minutes(5), new Double[]{});
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	@Test
	public void testRecordLogsErrorOnFailure() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		mockResponse(context, client, 500, "something bad happened", Matchers.any(HttpUriRequest.class));

		AggregatedData[] data = new AggregatedData[1];
		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.minutes(5), new Double[]{});
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	@Test
	public void testDoesNotCrashOnIOErrorsOnContent() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		final HttpResponse response = context.mock(HttpResponse.class);
		final HttpEntity responseEntity = context.mock(HttpEntity.class);
		final StatusLine responseStatusLine = context.mock(StatusLine.class);
		final InputStream contentStream = new BrokenInputStream();

		context.checking(new Expectations() {{
			try {
				one(client).execute(with(any(HttpUriRequest.class))); will(returnValue(response));
				allowing(response).getEntity(); will(returnValue(responseEntity));
				allowing(response).getStatusLine(); will(returnValue(responseStatusLine));
				atLeast(1).of(responseStatusLine).getStatusCode(); will(returnValue(200));
				allowing(responseEntity).getContent(); will(returnValue(contentStream));
			} catch (IOException ignored) { }
		}});

		AggregatedData[] data = new AggregatedData[1];
		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.minutes(5), new Double[]{});
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	private static class ExceptionCloseInputStream extends CharSequenceInputStream {

        public ExceptionCloseInputStream(CharSequence s, String charset) {
			super(s, charset);
		}

		@Override
		public void close() throws IOException {
			throw new IOException("boom!");
		}
	}

	@Test
	public void testDoesNotCrashOnContextCloseException() {
		Mockery context = new Mockery();

		final HttpClient client = context.mock(HttpClient.class);

		MonitordPublisher publisher = new MonitordPublisher("uri", "cluster", "host", client);

		final HttpResponse response = context.mock(HttpResponse.class);
		final HttpEntity responseEntity = context.mock(HttpEntity.class);
		final StatusLine responseStatusLine = context.mock(StatusLine.class);
		final InputStream contentStream = new ExceptionCloseInputStream("test", "utf-8");

		context.checking(new Expectations() {{
			try {
				one(client).execute(with(any(HttpUriRequest.class))); will(returnValue(response));
				allowing(response).getEntity(); will(returnValue(responseEntity));
				allowing(response).getStatusLine(); will(returnValue(responseStatusLine));
				atLeast(1).of(responseStatusLine).getStatusCode(); will(returnValue(200));
				allowing(responseEntity).getContent(); will(returnValue(contentStream));
			} catch (IOException ignored) { }
		}});

		AggregatedData[] data = new AggregatedData[1];
		data[0] = new AggregatedData(new SumStatistic(), "service_name", "host", "set/view", 2332d, new DateTime(2013, 9, 20, 8, 15, 0, 0), Period.minutes(5), new Double[]{});
		publisher.recordAggregation(data);

		context.assertIsSatisfied();
	}

	private void mockResponse(@Nonnull final Mockery context, final HttpClient client, final int statusCode,
							  final String content, final Matcher<HttpUriRequest> matcher) {
		final HttpResponse response = context.mock(HttpResponse.class);
		final HttpEntity responseEntity = context.mock(HttpEntity.class);
		final StatusLine responseStatusLine = context.mock(StatusLine.class);
		final InputStream contentStream = new CharSequenceInputStream(content, Charsets.UTF_8);

		context.checking(new Expectations() {{
			try {
				one(client).execute(with(matcher)); will(returnValue(response));
				allowing(response).getEntity(); will(returnValue(responseEntity));
				allowing(response).getStatusLine(); will(returnValue(responseStatusLine));
				atLeast(1).of(responseStatusLine).getStatusCode(); will(returnValue(statusCode));
				allowing(responseEntity).getContent(); will(returnValue(contentStream));
			} catch (IOException ignored) { }
		}});
	}
}
