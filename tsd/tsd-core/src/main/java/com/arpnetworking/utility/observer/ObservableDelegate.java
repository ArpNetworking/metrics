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
package com.arpnetworking.utility.observer;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is an implementation of <code>Observable</code> that may be used by
 * classes which implement <code>Observable</code> to delegate common
 * functionality to. This class is thread safe and ensures that a notification
 * is sent all observers registered at the time it is posted.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class ObservableDelegate implements Observable {

    /**
     * Create a new instance of <code>Observable</code>.
     *
     * @return New instance of <code>Observable</code>
     */
    public static ObservableDelegate newInstance() {
        return new ObservableDelegate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attach(final Observer observer) {
        try {
            _lock.writeLock().lock();
            if (!_observers.add(observer)) {
                throw new IllegalArgumentException(String.format("Observer already registered; observer=%s", observer));
            }
        } finally {
            _lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void detach(final Observer observer) {
        try {
            _lock.writeLock().lock();
            if (!_observers.remove(observer)) {
                throw new IllegalArgumentException(String.format("Observer not registered; observer=%s", observer));
            }
        } finally {
            _lock.writeLock().unlock();
        }
    }

    /**
     * Notify all registered observers of an event.
     *
     * @param observable The observable raising the event.
     * @param event The even being raised.
     */
    public void notify(final Observable observable, final Object event) {
        try {
            _lock.readLock().lock();
            for (final Observer observer : _observers) {
                observer.notify(observable, event);
            }
        } finally {
            _lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            _lock.readLock().lock();
            return MoreObjects.toStringHelper(this)
                    .add("Observers", _observers)
                    .toString();
        } finally {
            _lock.readLock().unlock();
        }
    }

    private ObservableDelegate() {}

    private final Set<Observer> _observers = Sets.newHashSet();
    private final ReadWriteLock _lock = new ReentrantReadWriteLock();
}
