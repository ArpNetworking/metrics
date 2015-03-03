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
package com.arpnetworking.play.metrics;

import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.Strings;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Simple action wrapper that wraps each call in a metrics timer.
 *
 * TODO(vkoskela): Add to shared library with Play dependency (tsd-core?) [MAI-?]
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class MetricsActionWrapper extends Action.Simple {

    /**
     * Public constructor.
     *
     * @param metricsFactory Instance of <code>MetricsFactory</code>.
     * @param action The <code>Action</code> to wrap.
     */
    public MetricsActionWrapper(final MetricsFactory metricsFactory, final Action<?> action) {
        _metricsFactory = metricsFactory;
        this.delegate = action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // CHECKSTYLE.OFF: IllegalThrow
    public F.Promise<Result> call(final Http.Context context) throws Throwable {
        // CHECKSTYLE.ON: IllegalThrow

        // TODO(vkoskela): Add sampled metrics action wrapper [MAI-340]
        /*
        final Metrics metrics = getMetrics(context);
        final Timer timer = metrics.createTimer(createTimerName(context));
        return delegate.call(context).map(new F.Function<Result, Result>() {
            @Override
            // CHECKSTYLE.OFF: IllegalThrow
            public Result apply(final Result result) throws Throwable {
                // CHECKSTYLE.ON: IllegalThrow
                timer.stop();
                metrics.close();
                return result;
            }
        });*/
        return delegate.call(context);
        // TODO(vkoskela): Add success/failure counter by mapping on return code. [MAI-279]
    }

    /**
     * Create the name of the timer from a <code>Http.Context</code>.
     *
     * @param context Context of the HTTP request/response.
     * @return Name of the timer for the request/response.
     */
    protected String createTimerName(final Http.Context context) {
        // TODO(vkoskela): Use routes information to replace variable path parts with variable names. [MAI-280]
        final Http.Request r = context.request();
        final StringBuilder metricNameBuilder = new StringBuilder("RestService/");
        metricNameBuilder.append(r.method());

        if (!Strings.isNullOrEmpty(r.path())) {
            if (!r.path().startsWith("/")) {
                metricNameBuilder.append("/");
            }
            metricNameBuilder.append(r.path());
        }

        return metricNameBuilder.toString();
    }

    private Metrics getMetrics(final Http.Context context) {
        Metrics metrics = (Metrics) context.args.get(METRICS_KEY);
        if (metrics == null) {
            metrics = _metricsFactory.create();
            context.args.put(METRICS_KEY, metrics);
        } else {
            LOGGER.warn()
                    .setMessage("Found metrics in request context; possible issue")
                    .addData("metrics", metrics)
                    .log();
        }
        return metrics;
    }

    private final MetricsFactory _metricsFactory;

    private static final String METRICS_KEY = "metrics";
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsActionWrapper.class);
}
