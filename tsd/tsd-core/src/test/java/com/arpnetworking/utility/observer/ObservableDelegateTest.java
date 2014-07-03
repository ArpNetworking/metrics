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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for the ObservableDelegate class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class ObservableDelegateTest {

    @Test(expected = IllegalArgumentException.class)
    public void testAttachSameObserver() {
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer = Mockito.mock(Observer.class);
        observable.attach(observer);
        observable.attach(observer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetachUnattachedObserver() {
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer = Mockito.mock(Observer.class);
        observable.detach(observer);
    }

    @Test
    public void testDetachAttachedObserver() {
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer = Mockito.mock(Observer.class);
        observable.attach(observer);
        observable.detach(observer);
    }

    @Test
    public void testNotifyNoAttachedObservers() {
        final Object event = new Object();
        final Observable observable = ObservableDelegate.newInstance();
        observable.notify(observable, event);
    }

    @Test
    public void testNotifyOneAttachedObserver() {
        final Object event = new Object();
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer = Mockito.mock(Observer.class);
        observable.attach(observer);
        observable.notify(observable, event);
        Mockito.verify(observer).notify(observable, event);
    }

    @Test
    public void testNotifyDetachedObserver() {
        final Object event = new Object();
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer = Mockito.mock(Observer.class);
        observable.attach(observer);
        observable.detach(observer);
        observable.notify(observable, event);
        Mockito.verify(observer, Mockito.never()).notify(observable, event);
    }

    @Test
    public void testNotifyMultipleAttachedObservers() {
        final Object event = new Object();
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer1 = Mockito.mock(Observer.class, "observer1");
        final Observer observer2 = Mockito.mock(Observer.class, "observer2");
        observable.attach(observer1);
        observable.attach(observer2);
        observable.notify(observable, event);
        Mockito.verify(observer1).notify(observable, event);
        Mockito.verify(observer2).notify(observable, event);
    }

    @Test
    public void testToString() {
        final Observable observable = ObservableDelegate.newInstance();
        final Observer observer = Mockito.mock(Observer.class);
        observable.attach(observer);
        final String asString = observable.toString();
        Assert.assertNotNull(asString);
        Assert.assertFalse(asString.isEmpty());
    }
}
