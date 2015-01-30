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

package models.protocol.v1;

import com.google.common.collect.Lists;
import models.ConnectionContext;
import models.protocol.MessageProcessorsFactory;
import models.protocol.MessagesProcessor;

import java.util.List;

/**
 * Factory for creating all the processors needed to process a v1 message.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class ProcessorsV1Factory implements MessageProcessorsFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public List<MessagesProcessor> create(final ConnectionContext connectionContext) {
        return Lists.newArrayList(
                new HeartbeatMessagesProcessor(connectionContext),
                new LogMessagesDroppingProcessor(),
                new MetricMessagesProcessor(connectionContext));
    }
}
