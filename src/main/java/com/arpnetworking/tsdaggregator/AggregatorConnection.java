package com.arpnetworking.tsdaggregator;

import com.arpnetworking.tsdaggregator.aggserver.Messages;
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
    private final Buffer _buffer;
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
        _buffer.appendBuffer(data);
        processMessages();
    }

    private void processMessages() {
        Optional<? extends GeneratedMessage> messageOptional = Message.parse(_buffer);
        if (messageOptional.isPresent()) {
            GeneratedMessage gm = messageOptional.get();
            if (gm instanceof Messages.HostIdentification) {
                Messages.HostIdentification message = (Messages.HostIdentification) gm;
                if (message.hasHostName()) {
                    _hostName = Optional.of(message.getHostName());
                }
                if (message.hasClusterName()) {
                    _clusterName = Optional.of(message.getClusterName());
                }
            }
        }
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
    static class Message {
        private static final Logger LOGGER = LoggerFactory.getLogger(Message.class);

        @Nonnull
        public static Optional<? extends GeneratedMessage> parse(@Nonnull final Buffer data) {
            int length = data.getInt(0);
            if (data.length() < length) {
                return Optional.absent();
            }
            byte type = data.getByte(4);
            byte[] toDeserialize = data.getBytes(5, data.length());

            Message message;

            try {
                switch (type) {
                    case 0x01:
                        return Optional.of(Messages.HostIdentification.parseFrom(toDeserialize));
                    default:
                        LOGGER.error("Unknown message type: " + type);
                        return Optional.absent();
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Invalid protocol buffer, message type: " + type + ", bytes: " +
                        Hex.encodeHexString(data.getBytes()), e);
                return Optional.absent();
            }
        }
    }
}
