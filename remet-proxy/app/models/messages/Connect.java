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

package models.messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import models.protocol.MessageProcessorsFactory;
import play.mvc.WebSocket;

/**
 * Akka message to hold new connection data.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Connect {
    /**
     * Public constructor.
     *
     * @param inChannel                Instance of <code>WebSocket.In</code>.
     * @param outChannel               Instance of <code>WebSocket.Out</code>.
     * @param messageProcessorsFactory Factory to create a <code>Metrics</code> object.
     */
    public Connect(
        final WebSocket.In<JsonNode> inChannel,
        final WebSocket.Out<JsonNode> outChannel,
        final MessageProcessorsFactory messageProcessorsFactory) {
        _outputChannel = outChannel;
        _inputChannel = inChannel;
        _messageProcessorsFactory = messageProcessorsFactory;
    }

    public WebSocket.Out<JsonNode> getOutputChannel() {
        return _outputChannel;
    }

    public WebSocket.In<JsonNode> getInputChannel() {
        return _inputChannel;
    }

    public MessageProcessorsFactory getMessageProcessorsFactory() {
        return _messageProcessorsFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("OutputChannel", _outputChannel)
                .add("InputChannel", _inputChannel)
                .add("MessageProcessorsFactory", _messageProcessorsFactory)
                .toString();
    }

    private final WebSocket.Out<JsonNode> _outputChannel;
    private final WebSocket.In<JsonNode> _inputChannel;
    private final MessageProcessorsFactory _messageProcessorsFactory;
}
