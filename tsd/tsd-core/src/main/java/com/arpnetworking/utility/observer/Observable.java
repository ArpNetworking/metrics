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

/**
 * Interface for classes which are observable. This interface was used in place
 * of Java's <code>Observable</code> for two main reasons:
 * 
 * 1) Java's <code>Observable</code> is an implementation and not an interface.
 * 2) Java's <code>Observable</code> does not permit cascading events; that is
 * an observer triggering further events.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface Observable {

    /**
     * Add an observer to be notified of events.
     * 
     * @param observer The observer to add.
     */
    void attach(final Observer observer);

    /**
     * Remove the observer. The observer will no longer be notified of events.
     * 
     * @param observer The observer to remove.
     */
    void detach(final Observer observer);

    /**
     * Notify all registered observers of an event.
     * 
     * @param observable The observable raising the event.
     * @param event The even being raised.
     */
    void notify(final Observable observable, final Object event);
}
