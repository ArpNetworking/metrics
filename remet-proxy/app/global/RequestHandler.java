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

package global;

import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.play.metrics.MetricsActionWrapper;
import play.http.DefaultHttpRequestHandler;
import play.mvc.Action;
import play.mvc.Http;

import java.lang.reflect.Method;
import javax.inject.Inject;

/**
 * Request handler for the application.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class RequestHandler extends DefaultHttpRequestHandler {
    /**
     * Public constructor.
     *
     * @param metricsFactory The metrics factory
     */
    @Inject
    public RequestHandler(final MetricsFactory metricsFactory) {
        _metricsFactory = metricsFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Action<?> createAction(final Http.Request request, final Method method) {
        return new MetricsActionWrapper(_metricsFactory, (Action<Object>) super.createAction(request, method));
    }

    private final MetricsFactory _metricsFactory;
}
