/**
 * Copyright 2015 Groupon.com
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
package com.arpnetworking.metrics.vertx.test;

import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.vertx.SinkVerticle;
import com.google.common.collect.ImmutableList;
import org.mockito.Mockito;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import java.util.List;

/**
 * This is a trivial implementation of the abstract verticle <code>SinkVerticle</code>.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public final class TestSinkVerticleImpl extends SinkVerticle {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Sink> createSinks() {
        final Sink sink = Mockito.mock(Sink.class);
        Mockito.doNothing().when(sink).record(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap());
        return ImmutableList.of(sink);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Handler<Message<String>> initializeHandler() {
        return new SinkHandlerWithReply(_sinks);
    }

    /**
     * An extension of <code>SinkHandler</code> that sends a reply.
     */
    public static final class SinkHandlerWithReply extends SinkHandler {
        /**
         * Public constructor.
         *
         * @param sinks A <code>List</code> of sinks to be written to.
         */
        public SinkHandlerWithReply(final List<Sink> sinks) {
            super(sinks);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public void handle(final Message<String> message) {
            super.handle(message);
            message.reply(message.body());
        }
    }
}
