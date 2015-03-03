/**
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * Tests for the <code>CarbonSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class CarbonSinkTest {

    @Before
    public void before() {
        _carbonSinkBuilder = new CarbonSink.Builder()
                .setName("carbon_sink_test")
                .setServerAddress("my-carbon-server.example.com")
                .setServerPort(9999);
    }

    @Test
    public void testConnect() {
        final CarbonSink carbonSink = _carbonSinkBuilder.build();
        final NetSocket socket = Mockito.mock(NetSocket.class);
        carbonSink.onConnect(socket);
        Mockito.verifyZeroInteractions(socket);
    }

    @Test
    public void testSerialize() {
        final CarbonSink carbonSink = _carbonSinkBuilder.build();
        final AggregatedData datum = TestBeanFactory.createAggregatedData();
        final Buffer buffer = carbonSink.serialize(datum);
        Assert.assertNotNull(buffer);
        String bufferString = buffer.toString();
        Assert.assertTrue("Buffer=" + bufferString, bufferString.endsWith("\n"));
        bufferString = bufferString.substring(0, bufferString.length() - 1);
        final String[] keyValueParts = bufferString.split(" ");
        Assert.assertEquals("Buffer=" + bufferString, 3, keyValueParts.length);
        Assert.assertEquals("Buffer=" + bufferString, String.format("%f", datum.getValue().getValue()), keyValueParts[1]);
        Assert.assertEquals("Buffer=" + bufferString, String.valueOf(datum.getPeriodStart().getMillis() / 1000), keyValueParts[2]);
        final String[] keyParts = keyValueParts[0].split("\\.");
        Assert.assertEquals("Buffer=" + bufferString, 6, keyParts.length);
        Assert.assertEquals("Buffer=" + bufferString, datum.getFQDSN().getCluster(), keyParts[0]);
        Assert.assertEquals("Buffer=" + bufferString, datum.getHost(), keyParts[1]);
        Assert.assertEquals("Buffer=" + bufferString, datum.getFQDSN().getService(), keyParts[2]);
        Assert.assertEquals("Buffer=" + bufferString, datum.getFQDSN().getMetric(), keyParts[3]);
        Assert.assertEquals("Buffer=" + bufferString, datum.getPeriod().toString(), keyParts[4]);
        Assert.assertEquals("Buffer=" + bufferString, datum.getFQDSN().getStatistic().getName(), keyParts[5]);
    }

    private CarbonSink.Builder _carbonSinkBuilder;
}
