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

package com.arpnetworking.utility;

import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import com.google.common.collect.Lists;
import scala.Function0;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.runtime.AbstractFunction0;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects futures and provides them in a combined promise.
 *
 * @param <T> Return future type
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class CollectFutureBuilder<T> {
    /**
     * Method to create a {@code <T>} from the completed {@link scala.concurrent.Future}s.
     *
     * @param callback Callback function
     * @return this builder
     */
    public CollectFutureBuilder<T> map(final Function0<T> callback) {
        _callback = callback;
        return this;
    }

    /**
     * Registers a {@link scala.concurrent.Future} in the collection.  A future must be registered in order
     * to be waited on.
     *
     * @param future Future to register
     * @return this builder
     */
    public CollectFutureBuilder<T> addFuture(final Future<?> future) {
        _futures.add(future);
        return this;
    }

    /**
     * Sets the list of {@link scala.concurrent.Future}s to wait on.
     *
     * @param futures The list of futures
     * @return this builder
     */
    public CollectFutureBuilder<T> setFutures(final List<Future<?>> futures) {
        _futures.clear();
        _futures.addAll(futures);
        return this;
    }

    /**
     * Builds the final future.
     *
     * @param context context to execute the futures on
     * @return the new future
     */
    @SuppressWarnings("unchecked")
    public Future<T> build(final ExecutionContext context) {
        final Promise<T> result = Futures.promise();
        final AtomicInteger latch = new AtomicInteger(_futures.size());

        final OnComplete<?> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                if (failure != null) {
                    result.failure(failure);
                }
                final int count = latch.decrementAndGet();
                if (count == 0) {
                    result.success(_callback.apply());
                }
            }
        };

        for (final Future future : _futures) {
            future.onComplete(onComplete, context);
        }

        return result.future();
    }

    private Function0<T> _callback = new AbstractFunction0<T>() {
        @Override
        public T apply() {
            return null;
        }
    };

    private final List<Future<?>> _futures = Lists.newArrayList();
}

