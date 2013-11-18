package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Optional;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import javax.annotation.Nonnull;

/**
 * Represents a connection to the aggregation server.
 *
 * @author barp
 */
public class AggregatorConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorConnection.class);
    private final NetSocket _socket;
    private final ClusterNameResolvedCallback _clusterNameResolvedCallback;
    private Buffer _buffer;
    private Optional<String> _hostName = Optional.absent();
    private Optional<String> _clusterName = Optional.absent();

    public AggregatorConnection(@Nonnull final NetSocket socket,
                                @Nonnull final ClusterNameResolvedCallback clusterNameResolvedCallback) {
        _socket = socket;
        _clusterNameResolvedCallback = clusterNameResolvedCallback;
        _buffer = new Buffer();
    }

    @Nonnull
    public NetSocket getSocket() {
        return _socket;
    }

    public void dataReceived(final Buffer data) {
        LOGGER.info("dataReceived");
        _buffer.appendBuffer(data);
        processMessages();
    }

    private void processMessages() {
        Buffer current = _buffer;
        Optional<Message> messageOptional;
        while (current.length() > 4 && (messageOptional = Message.parse(current)).isPresent()) {
            Message message = messageOptional.get();
            current = current.getBuffer(message.length(), current.length());
            GeneratedMessage gm = message.getMessage();
            if (gm instanceof Messages.HostIdentification) {
                Messages.HostIdentification hostIdent = (Messages.HostIdentification) gm;
                if (hostIdent.hasHostName()) {
                    _hostName = Optional.of(hostIdent.getHostName());
                }
                if (hostIdent.hasClusterName()) {
                    _clusterName = Optional.of(hostIdent.getClusterName());
                }
                LOGGER.info("Handshake from host " + _hostName.or("") + " in cluster " + _clusterName.or(""));
                _clusterNameResolvedCallback.clusterNameResolved(_clusterName.or(""), this);
            } else if (gm instanceof Messages.AggregationRecord) {
                Messages.AggregationRecord aggRecord = (Messages.AggregationRecord) gm;
            } else {
                LOGGER.warn("Unknown message type!");
            }
        }
        _buffer = new Buffer();
        _buffer.appendBuffer(current);
    }

    private void writeData(final Buffer data) {
        if (_socket.writeQueueFull()) {
            _socket.pause();
        }
    }

    /**
     * Interface used to implement a callback when a client sends it's host and cluster name.
     */
    public interface ClusterNameResolvedCallback {
        void clusterNameResolved(String clusterName, AggregatorConnection connection);
    }

    /**
     * Class for building messages from the raw, on-the-wire bytes in the TCP stream.
     */
    public static class Message {
        private static final Logger LOGGER = LoggerFactory.getLogger(Message.class);
        private final GeneratedMessage _message;

        private Message(@Nonnull GeneratedMessage message) {
            _message = message;
        }

        GeneratedMessage getMessage() {
            return _message;
        }

        public int length() {
            return _message.getSerializedSize() + 5;
        }

        public Buffer getBuffer() {
            Buffer b = new Buffer();
            b.appendInt(0);
            if (_message instanceof Messages.HostIdentification) {
                b.appendByte((byte) 0x01);
            } else if (_message instanceof Messages.AggregationRecord) {
                b.appendByte((byte) 0x02);
            }
            b.appendBytes(_message.toByteArray());
            b.setInt(0, b.length());
            return b;
        }

        @Nonnull
        public static Optional<Message> parse(@Nonnull final Buffer data) {
            int length = data.getInt(0);
            if (data.length() < length) {
                LOGGER.info("Incomplete message. Expected " + length + " bytes, got " + data.length());
                return Optional.absent();
            }
            byte type = data.getByte(4);
            byte[] toDeserialize = data.getBytes(5, length);

            try {
                switch (type) {
                    case 0x01:
                        return Optional.of(new Message(Messages.HostIdentification.parseFrom(toDeserialize)));
                    case 0x02:
                        return Optional.of(new Message(Messages.AggregationRecord.parseFrom(toDeserialize)));
                    default:
                        LOGGER.error("Unknown message type: " + type);
                        return Optional.absent();
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Invalid protocol buffer, message type: " + type + ", bytes: " +
                        Hex.encodeHexString(data.getBytes(0, length)), e);
                return Optional.absent();
            }
        }

        public static Message create(GeneratedMessage message) {
            return new Message(message);
        }
    }
}
