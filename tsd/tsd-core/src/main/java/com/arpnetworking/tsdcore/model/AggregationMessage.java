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
import com.google.common.base.Optional;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

/**
 * Class for building messages from the raw, on-the-wire bytes in the TCP stream.
 * 
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class AggregationMessage {

    /**
     * Static factory.
     * 
     * @param message The message.
     * @return New <code>AggregationMessage</code> instance.
     */
    public static AggregationMessage create(final GeneratedMessage message) {
        return new AggregationMessage(message);
    }

    /**
     * Deserialize message from <code>Buffer</code>.
     * 
     * TODO(vkoskela): The header and message need to be versioned [MAI-133].
     * 
     * @param data The <code>Buffer</code> containing the serialized message.
     * @return The deserialized <code>AggregationMessage</code> or absent if
     * the <code>Buffer</code> could not be deserialized.
     */
    public static Optional<AggregationMessage> deserialize(final Buffer data) {
        int position = 0;

        // Deserialize and validate buffer length
        final int length = data.getInt(position);
        position += INTEGER_SIZE_IN_BYTES;
        if (data.length() != length) {
            return Optional.absent();
        }

        // Deserialize message type
        final byte type = data.getByte(position);
        position += BYTE_SIZE_IN_BYTES;

        // Obtain the serialized payload
        final byte[] payloadBytes = data.getBytes(position, length);

        // Deserialize the message based on the type
        try {
            switch (type) {
                case 0x01:
                    return Optional.of(new AggregationMessage(Messages.HostIdentification.parseFrom(payloadBytes)));
                case 0x02:
                    return Optional.of(new AggregationMessage(Messages.AggregationRecord.parseFrom(payloadBytes)));
                default:
                    LOGGER.warn("Unsupported message type; type=" + type);
                    return Optional.absent();
            }
        } catch (final InvalidProtocolBufferException e) {
            LOGGER.warn("Invalid protocol buffer; type=" + type + " bytes="
                    + Hex.encodeHexString(data.getBytes(0, length)), e);
            return Optional.absent();
        }
    }

    /**
     * Serialize the message into a <code>Buffer</code>.
     * 
     * @return <code>Buffer</code> containing serialized message.
     */
    public Buffer serialize() {
        final Buffer b = new Buffer();
        b.appendInt(0);
        if (_message instanceof Messages.HostIdentification) {
            b.appendByte((byte) 0x01);
        } else if (_message instanceof Messages.AggregationRecord) {
            b.appendByte((byte) 0x02);
        } else {
            throw new IllegalArgumentException("Unsupported message; message=" + _message);
        }
        b.appendBytes(_message.toByteArray());
        b.setInt(0, b.length());
        return b;
    }

    public GeneratedMessage getMessage() {
        return _message;
    }

    public int getLength() {
        return _message.getSerializedSize() + HEADER_SIZE_IN_BYTES;
    }

    private AggregationMessage(final GeneratedMessage message) {
        _message = message;
    }

    private final GeneratedMessage _message;

    private static final int BYTE_SIZE_IN_BYTES = 1;
    private static final int INTEGER_SIZE_IN_BYTES = Integer.SIZE / 8;
    private static final int HEADER_SIZE_IN_BYTES = INTEGER_SIZE_IN_BYTES + 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationMessage.class);

}
