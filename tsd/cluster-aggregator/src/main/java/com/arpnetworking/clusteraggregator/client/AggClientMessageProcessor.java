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

package com.arpnetworking.clusteraggregator.client;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.arpnetworking.tsdcore.Messages;
import com.arpnetworking.tsdcore.model.AggregationMessage;
import com.google.protobuf.GeneratedMessage;

/**
 * Processes messages from a client.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class AggClientMessageProcessor extends UntypedActor {
    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof AggregationMessage) {
            _log.info("Got aggregation message in AggClientMessageProcessor");
            final GeneratedMessage aggMessage = ((AggregationMessage) message).getMessage();
            if (aggMessage instanceof Messages.AggregationRecord) {
                final Messages.AggregationRecord aggRecord = (Messages.AggregationRecord) aggMessage;
                final String metric = aggRecord.getMetric();
                final String periodStart = aggRecord.getPeriodStart();
                _log.info(String.format("   %s, %s", metric, periodStart));
            } else {
                _log.warning(String.format("   unknown message of type %s: %s", aggMessage.getClass(), aggMessage));
            }
        } else {
            unhandled(message);
        }

    }

    private final LoggingAdapter _log = Logging.getLogger(getContext().system(), this);
}
