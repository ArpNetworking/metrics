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

import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.arpnetworking.metrics.aggregation.protocol.Messages;
import com.arpnetworking.tsdcore.statistics.Statistic;
import com.arpnetworking.tsdcore.statistics.StatisticFactory;
import com.google.common.base.Optional;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.UnknownFieldSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.vertx.java.core.buffer.Buffer;

import java.nio.ByteOrder;

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
        final ByteString byteString = ByteString.fromArray(vertxBuffer.getBytes());

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
        final Optional<AggregationMessage> deserializedProtobufMessage = AggregationMessage.deserialize(byteString);
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
        ByteString buffer;
        Optional<AggregationMessage> message;

        // Too little data
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 - 1, ByteOrder.BIG_ENDIAN).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Too much data
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 + 1, ByteOrder.BIG_ENDIAN).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // No data
        buffer = new ByteStringBuilder().putInt(0, ByteOrder.BIG_ENDIAN).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Negative data
        buffer = ByteString.fromInts(-1);
        message = AggregationMessage.deserialize(buffer);
    }

    @Test
    public void testDeserializeUnsupportedType() {
        ByteString buffer;
        Optional<AggregationMessage> message;

        // Type: 0
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 + 1, ByteOrder.BIG_ENDIAN).putByte((byte) 0).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Type: 3
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 + 1, ByteOrder.BIG_ENDIAN).putByte((byte) 3).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Type: -1
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 + 1, ByteOrder.BIG_ENDIAN).putByte((byte) -1).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);
    }

    @Test
    public void testDeserializeInvalidPayload() {
        ByteString buffer;
        Optional<AggregationMessage> message;

        // No data
        // NOTE: Some messages will deserialize from an empty buffer.
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 + 1, ByteOrder.BIG_ENDIAN).putByte((byte) 3).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);

        // Just one byte
        buffer = new ByteStringBuilder().putInt(Integer.SIZE / 8 + 2, ByteOrder.BIG_ENDIAN).putByte((byte) 1).putByte((byte) 0).result();
        message = AggregationMessage.deserialize(buffer);
        Assert.assertEquals(Optional.absent(), message);
    }

    private static final StatisticFactory STATISTIC_FACTORY = new StatisticFactory();
    private static final Statistic TP99_STATISTIC = STATISTIC_FACTORY.getStatistic("tp99");
}
