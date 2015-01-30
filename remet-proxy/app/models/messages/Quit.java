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
import play.mvc.WebSocket;

/**
 * Message class to indicate a client disconnection.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Quit {

    /**
     * Public constructor.
     *
     * @param channel Instance of <code>WebSocket.Out</code>.
     */
    public Quit(final WebSocket.Out<JsonNode> channel) {
        _channel = channel;
    }

    public WebSocket.Out<JsonNode> getChannel() {
        return _channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Channel", _channel)
                .toString();
    }

    private final WebSocket.Out<JsonNode> _channel;
}
