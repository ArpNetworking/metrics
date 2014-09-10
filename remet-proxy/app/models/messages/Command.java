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
import com.google.common.base.Objects;

/**
 * Message class to hold general command data.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class Command {

    /**
     * Public constructor.
     *
     * @param command Command to be executed.
     */
    public Command(final JsonNode command) {
        _command = command;
    }

    public JsonNode getCommand() {
        return _command;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("Command", _command)
                .toString();
    }

    private final JsonNode _command;

    /**
     * Command string for a getMetrics call.
     */
    public static final String COMMAND_GET_METRICS = "getMetrics";
    /**
     * Command string for a heartbeat.
     */
    public static final String COMMAND_HEARTBEAT = "heartbeat";
    /**
     * Command string for a subscribe call.
     */
    public static final String COMMAND_SUBSCRIBE = "subscribe";
    /**
     * Command string for an unsubscribe call.
     */
    public static final String COMMAND_UNSUBSCRIBE = "unsubscribe";

}
