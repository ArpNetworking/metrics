package com.arpnetworking.tsdaggregator.aggserver;

import com.google.common.base.Objects;
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
    @Nonnull
    private final NetSocket _socket;
    @Nonnull
    private final ClusterNameResolvedCallback _clusterNameResolvedCallback;
    @Nonnull
    private final AggregationArrivedCallback _aggregationArrivedCallback;
    private Buffer _buffer;
    private Optional<String> _hostName = Optional.absent();
    private Optional<String> _clusterName = Optional.absent();

    public AggregatorConnection(@Nonnull final NetSocket socket,
                                @Nonnull final ClusterNameResolvedCallback clusterNameResolvedCallback,
                                @Nonnull final AggregationArrivedCallback aggregationArrivedCallback) {
        _socket = socket;
        _clusterNameResolvedCallback = clusterNameResolvedCallback;
        _aggregationArrivedCallback = aggregationArrivedCallback;
        _buffer = new Buffer();
    }

    @Nonnull
    public NetSocket getSocket() {
        return _socket;
    }

    public Optional<String> getHostName() {
        return _hostName;
    }

    public Optional<String> getClusterName() {
        return _clusterName;
    }

    public void dataReceived(final Buffer data) {
        _buffer.appendBuffer(data);
        processMessages();
    }

    private void processMessages() {
        Buffer current = _buffer;
        Optional<Message> messageOptional;
        while (current.length() > 4 && (messageOptional = Message.parse(current)).isPresent()) {
            Message message = messageOptional.get();
            current = current.getBuffer(message.length(), current.length());
            @Nonnull GeneratedMessage gm = message.getMessage();
            if (gm instanceof Messages.HostIdentification) {
                @Nonnull Messages.HostIdentification hostIdent = (Messages.HostIdentification) gm;
                if (hostIdent.hasHostName()) {
                    _hostName = Optional.of(hostIdent.getHostName());
                }
                if (hostIdent.hasClusterName()) {
                    _clusterName = Optional.of(hostIdent.getClusterName());
                }
                LOGGER.info("Handshake from host " + _hostName.or("") + " in cluster " + _clusterName.or(""));
                _clusterNameResolvedCallback.clusterNameResolved(this, _hostName.or(""), _clusterName.or(""));
            } else if (gm instanceof Messages.AggregationRecord) {
                @Nonnull Messages.AggregationRecord aggRecord = (Messages.AggregationRecord) gm;
                LOGGER.info("Aggregation from host " + _hostName.or("") + " in cluster " + _clusterName.or(""));
                _aggregationArrivedCallback.aggregationArrived(this, aggRecord);
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
        void clusterNameResolved(AggregatorConnection connection, String hostName, String clusterName);
    }

    /**
     * Interface used to implement a callback when an aggregation record is received.
     */
    public interface AggregationArrivedCallback {
        void aggregationArrived(AggregatorConnection connection, Messages.AggregationRecord record);
    }

    /**
     * Class for building messages from the raw, on-the-wire bytes in the TCP stream.
     */
    public static class Message {
        private static final Logger LOGGER = LoggerFactory.getLogger(Message.class);
        @Nonnull
        private final GeneratedMessage _message;

        private Message(@Nonnull GeneratedMessage message) {
            _message = message;
        }

        @Nonnull
        GeneratedMessage getMessage() {
            return _message;
        }

        public int length() {
            return _message.getSerializedSize() + 5;
        }

        @Nonnull
        public Buffer getBuffer() {
            @Nonnull Buffer b = new Buffer();
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

        @Nonnull
        public static Message create(@Nonnull GeneratedMessage message) {
            return new Message(message);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("_hostName", _hostName).add("_clusterName", _clusterName)
                .add("_socket", _socket).toString();
    }
}
