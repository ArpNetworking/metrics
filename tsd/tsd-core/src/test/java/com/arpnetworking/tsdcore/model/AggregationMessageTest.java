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
package com.arpnetworking.tsdcore.model;

import com.arpnetworking.tsdcore.Messages;
import com.arpnetworking.tsdcore.statistics.TP99Statistic;
import com.google.common.base.Optional;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.UnknownFieldSet;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.vertx.java.core.buffer.Buffer;

/**
 * Tests for the AggregationMessage class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class AggregationMessageTest {

    @Test
    public void testHostIdentification() {
        final GeneratedMessage protobufMessage = Messages.HostIdentification.getDefaultInstance();
        final AggregationMessage message = AggregationMessage.create(protobufMessage);
        Assert.assertNotNull(message);
        Assert.assertSame(protobufMessage, message.getMessage());

        final Buffer vertxBuffer = message.serialize();
        final byte[] messageBuffer = vertxBuffer.getBytes();
        final byte[] protobufBuffer = protobufMessage.toByteArray();

        // Assert length
        Assert.assertEquals(protobufBuffer.length + 5, messageBuffer.length);
        Assert.assertEquals(protobufBuffer.length + 5, vertxBuffer.getInt(0));
        Assert.assertEquals(protobufBuffer.length + 5, message.getLength());

        // Assert payload type
        Assert.assertEquals(1, messageBuffer[4]);

        // Assert the payload was not corrupted
        for (int i = 0; i < protobufBuffer.length; ++i) {
            Assert.assertEquals(protobufBuffer[i], messageBuffer[i + 5]);
        }

        // Deserialize the message
        final Optional<AggregationMessage> deserializedProtobufMessage = AggregationMessage.deserialize(vertxBuffer);
        Assert.assertTrue(deserializedProtobufMessage.isPresent());
        Assert.assertEquals(protobufMessage, deserializedProtobufMessage.get().getMessage());
        Assert.assertEquals(message.getLength(), deserializedProtobufMessage.get().getLength());
    }

    @Test
    public void testAggregationRecord() {
        final GeneratedMessage protobufMessage = Messages.AggregationRecord.getDefaultInstance()
                .toBuilder()
                .setMetric("MyMetric")
                .setPeriod(Period.days(1).toString())
                .setPeriodStart(new DateTime().toString())
                .setPopulationSize(1)
                .addSamples(3.14f)
                .setService("MyService")
                .setStatistic(new TP99Statistic().getName())
                .setStatisticValue(6.28f)
                .build();
        final AggregationMessage message = AggregationMessage.create(protobufMessage);
        Assert.assertNotNull(message);
        Assert.assertSame(protobufMessage, message.getMessage());

        final Buffer vertxBuffer = message.serialize();
        final byte[] messageBuffer = vertxBuffer.getBytes();
        final byte[] protobufBuffer = protobufMessage.toByteArray();

        // Assert length
        Assert.assertEquals(protobufBuffer.length + 5, messageBuffer.length);
        Assert.assertEquals(protobufBuffer.length + 5, vertxBuffer.getInt(0));
        Assert.assertEquals(protobufBuffer.length + 5, message.getLength());

        // Assert payload type
        Assert.assertEquals(2, messageBuffer[4]);

        // Assert the payload was not currupted
        for (int i = 0; i < protobufBuffer.length; ++i) {
            Assert.assertEquals(protobufBuffer[i], messageBuffer[i + 5]);
        }

        // Deserialize the message
        final Optional<AggregationMessage> deserializedProtobufMessage = AggregationMessage.deserialize(vertxBuffer);
        Assert.assertTrue(deserializedProtobufMessage.isPresent());
        Assert.assertEquals(protobufMessage, deserializedProtobufMessage.get().getMessage());
        Assert.assertEquals(message.getLength(), deserializedProtobufMessage.get().getLength());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSerializedUnsupportedMessage() {
        final GeneratedMessage mockMessage = Mockito.mock(GeneratedMessage.class);
        Mockito.doReturn(UnknownFieldSet.getDefaultInstance()).when(mockMessage).getUnknownFields();
        AggregationMessage.create(mockMessage).serialize();
    }

    @Test
    public void testDeserializeWrongLength() {
        Buffer buffer;
        Optional<AggregationMessage> message;

        // Too little data
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 - 1);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Too much data
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 + 1);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // No data
        buffer = new Buffer();
        buffer.appendInt(0);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Negative data
        buffer = new Buffer();
        buffer.appendInt(-1);
        message = AggregationMessage.deserialize(buffer);
    }

    @Test
    public void testDeserializeUnsupportedType() {
        Buffer buffer;
        Optional<AggregationMessage> message;

        // Type: 0
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 + 1);
        buffer.appendByte((byte) 0);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Type: 3
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 + 1);
        buffer.appendByte((byte) 3);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Type: -1
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 + 1);
        buffer.appendByte((byte) -1);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);
    }

    @Test
    public void testDeserializeInvalidPayload() {
        Buffer buffer;
        Optional<AggregationMessage> message;

        // No data
        // NOTE: Some messages will deserialize from an empty buffer.
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 + 1);
        buffer.appendByte((byte) 2);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Just one byte
        buffer = new Buffer();
        buffer.appendInt(Integer.SIZE / 8 + 2);
        buffer.appendByte((byte) 1);
        buffer.appendByte((byte) 0);
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);
    }
}
