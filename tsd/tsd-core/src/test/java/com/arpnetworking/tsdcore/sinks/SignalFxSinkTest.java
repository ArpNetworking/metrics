package com.arpnetworking.tsdcore.sinks;

import akka.actor.ActorSystem;
import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.PeriodicData;
import com.arpnetworking.tsdcore.model.Quantity;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.InvalidProtocolBufferException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tests for the <code>SignalFxSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class SignalFxSinkTest {

    @AfterClass
    public static void afterClass() {
        ACTOR_SYSTEM.shutdown();
    }

    @Test
    public void testSignalFxSink() throws InvalidProtocolBufferException, InterruptedException {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/v2/datapoint"))
                .withQueryParam("orgid", WireMock.equalTo("MyOrganizationId"))
                .willReturn(
                        WireMock.aResponse()
                                .withBody("")
                                .withHeader("Content-Type", "text/html")
                                .withStatus(200)));

        final Sink sink = new SignalFxSink.Builder()
                .setApiToken("MyApiToken")
                .setOrganizationId("MyOrganizationId")
                .setSource("AINT")
                .setActorSystem(ACTOR_SYSTEM)
                .setName("SignalFxSinkTest.testSignalFxSink")
                .setUri(URI.create("http://localhost:8089/v2/datapoint"))
                .build();

        final DateTime start = new DateTime(1445313091000L);
        final PeriodicData periodicData = TestBeanFactory.createPeriodicDataBuilder()
                .setPeriod(Period.seconds(1))
                .setStart(start)
                .setDimensions(ImmutableMap.of("host", "app1.example.com"))
                .setData(
                        ImmutableList.of(
                                TestBeanFactory.createAggregatedDataBuilder()
                                        .setFQDSN(
                                                TestBeanFactory.createFQDSNBuilder()
                                                        .setCluster("MyCluster")
                                                        .setService("MyService")
                                                        .setMetric("MyMetric")
                                                        .setStatistic(STATISTICS_FACTORY.getStatistic("tp0"))
                                                        .build())
                                        .setValue(new Quantity.Builder().setValue(1.23).build())
                                        .build()))
                .build();
        sink.recordAggregateData(periodicData);

        Thread.sleep(5000);

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/v2/datapoint?orgid=MyOrganizationId"))
                .withHeader("Content-Type", WireMock.equalTo("application/x-protobuf"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withHeader("X-SF-TOKEN", WireMock.equalTo("MyApiToken")));

        // Parse post request body
        final List<LoggedRequest> requests = WireMock.findAll(WireMock.postRequestedFor(
                WireMock.urlEqualTo("/v2/datapoint?orgid=MyOrganizationId")));
        Assert.assertEquals(1, requests.size());

        // WireMock does not support binary payloads
        // Reference: https://github.com/tomakehurst/wiremock/issues/247
        /*
        final byte[] requestBody = Iterables.getOnlyElement(requests).getBody();
        final SignalFxProtocolBuffers.DataPointUploadMessage sfxMessage =
                SignalFxProtocolBuffers.DataPointUploadMessage.parseFrom(requestBody);

        // Validate message data
        Assert.assertEquals(1, sfxMessage.getDatapointsCount());
        final SignalFxProtocolBuffers.DataPoint sfxDataPoint = sfxMessage.getDatapoints(0);
        Assert.assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE, sfxDataPoint.getMetricType());
        Assert.assertEquals("PT1S_MyMetric_min", sfxDataPoint.getMetric());
        //Assert.assertEquals(start.getMillis() + 1000, sfxDataPoint.getTimestamp());
        Assert.assertEquals(1.23, sfxDataPoint.getValue().getDoubleValue(), 0.001);
        Assert.assertEquals(3, sfxDataPoint.getDimensionsCount());
        final Map<String, String> sfxDimensions = sfxDataPoint.getDimensionsList().stream()
                .map(sfxDimension -> new AbstractMap.SimpleEntry<>(sfxDimension.getKey(), sfxDimension.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Assert.assertEquals("app1.example.com", sfxDimensions.get("host"));
        Assert.assertEquals("MyService", sfxDimensions.get("service"));
        Assert.assertEquals("MyCluster", sfxDimensions.get("cluster"));
        */
    }

    @Test
    public void testSerialization() throws InvalidProtocolBufferException {
        // NOTE: This test is just here until we can deserialize the data received by WireMock above.
        final String dataAsString = "10,115,10,4,65,73,78,84,18,17,80,84,49,83,95,77,121,77,101,116,114,105,99,95,109,105,110,24,-96,-77,-2,-102,-120,42,34,9,17,-82,71,-31,122,20,-82,-13,63,40,0,50,24,10,4,104,111,115,116,18,16,97,112,112,49,46,101,120,97,109,112,108,101,46,99,111,109,50,20,10,7,115,101,114,118,105,99,101,18,9,77,121,83,101,114,118,105,99,101,50,20,10,7,99,108,117,115,116,101,114,18,9,77,121,67,108,117,115,116,101,114";
        final String[] dataAsStringArray = dataAsString.split(",");
        final byte[] bytes = new byte[dataAsStringArray.length];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte)Integer.parseInt(dataAsStringArray[i]);
        }
        final SignalFxProtocolBuffers.DataPointUploadMessage sfxMessage =
                SignalFxProtocolBuffers.DataPointUploadMessage.parseFrom(bytes);
    }

    @Rule
    public WireMockRule _wireMockRule = new WireMockRule(8089);

    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.apply();
    private static final StatisticFactory STATISTICS_FACTORY = new StatisticFactory();
}
