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
package com.arpnetworking.metrics;

/**
 * Interface for classes which create <code>Metrics</code> instances. Clients
 * should create a single instance of an implementing class for the entire
 * life of the application. Frameworks such as  
 * <a href="http://projects.spring.io/spring-framework/">Spring/</a> and 
 * <a href="https://code.google.com/p/google-guice/">Guice</a> may be used to inject the
 * <code>MetricsFactory</code> instance into various components within the 
 * application.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public interface MetricsFactory {

    /**
     * Return an instance of <code>Metrics</code>.
     * 
     * @return An instance of <code>Metrics</code>.
     */
    Metrics create();
}
