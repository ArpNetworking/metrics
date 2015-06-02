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
package controllers;

import com.arpnetworking.remet.gui.QueryResult;
import com.arpnetworking.remet.gui.alerts.Alert;
import com.arpnetworking.remet.gui.alerts.AlertQuery;
import com.arpnetworking.remet.gui.alerts.AlertRepository;
import com.arpnetworking.remet.gui.alerts.Context;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import models.PagedContainer;
import models.Pagination;
import org.apache.http.HttpHeaders;
import play.Configuration;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Singleton;

/**
 * Real time metrics web interface (ReMetGui) alert controller. Exposes APIs to query and
 * manipulate alerts.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@Singleton
public class AlertController extends Controller {

    /**
     * Public constructor.
     *
     * @param configuration Instance of Play's <code>Configuration</code>.
     * @param alertRepository Instance of <code>AlertRepository</code>.
     */
    @Inject
    public AlertController(final Configuration configuration, final AlertRepository alertRepository) {
        this(configuration.getInt("alerts.limit", DEFAULT_MAX_LIMIT), alertRepository);
    }

    /**
     * Query for alerts.
     *
     * @param contains The text to search for. Optional.
     * @param context The context of the alert. Optional.
     * @param cluster The cluster of the statistic to evaluate as part of the alert. Optional.
     * @param service The service of the statistic to evaluate as part of the alert. Optional.
     * @param limit The maximum number of results to return. Optional.
     * @param offset The number of results to skip. Optional.
     * @return <code>Result</code> paginated matching alerts.
     */
    // CHECKSTYLE.OFF: ParameterNameCheck - Names must match query parameters.
    public Result query(
            final String contains,
            final String context,
            final String cluster,
            final String service,
            final Integer limit,
            final Integer offset) {
        // CHECKSTYLE.ON: ParameterNameCheck

        // Convert and validate parameters
        final Optional<String> argContains = Optional.ofNullable(contains);
        final Context contextValue;
        try {
            contextValue = context == null ? null : Context.valueOf(context);
        } catch (final IllegalArgumentException iae) {
            return badRequest("Invalid context argument");
        }
        final Optional<Context> argContext = Optional.ofNullable(contextValue);
        final Optional<String> argCluster = Optional.ofNullable(cluster);
        final Optional<String> argService = Optional.ofNullable(service);
        final Optional<Integer> argOffset = Optional.ofNullable(offset);
        final int argLimit = Math.min(_maxLimit, Optional.of(MoreObjects.firstNonNull(limit, _maxLimit)).get());
        if (argLimit < 0) {
            return badRequest("Invalid limit; must be greater than or equal to 0");
        }
        if (argOffset.isPresent() && argOffset.get() < 0) {
            return badRequest("Invalid offset; must be greater than or equal to 0");
        }

        // Build conditions map
        final Map<String, String> conditions = Maps.newHashMap();
        if (argContains.isPresent()) {
            conditions.put("contains", argContains.get());
        }
        if (argContext.isPresent()) {
            conditions.put("context", argContext.get().toString());
        }
        if (argCluster.isPresent()) {
            conditions.put("cluster", argCluster.get());
        }
        if (argService.isPresent()) {
            conditions.put("service", argService.get());
        }

        // Build a host repository query
        final AlertQuery query = _alertRepository.createQuery()
                .contains(argContains)
                .context(argContext)
                .service(argService)
                .cluster(argCluster)
                .limit(argLimit)
                .offset(argOffset);

        // Execute the query
        final QueryResult<Alert> result;
        try {
            result = query.execute();
            // CHECKSTYLE.OFF: IllegalCatch - Convert any exception to 500
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            LOGGER.error()
                    .setMessage("Alert query failed")
                    .setThrowable(e)
                    .log();
            return internalServerError();
        }

        // Wrap the query results and return as JSON
        if (result.etag().isPresent()) {
            response().setHeader(HttpHeaders.ETAG, result.etag().get());
        }
        return ok(Json.toJson(new PagedContainer<Alert>(
                result.values(),
                new Pagination(
                        request().path(),
                        result.total(),
                        result.values().size(),
                        argLimit,
                        argOffset,
                        conditions))));
    }

    /**
     * Get specific alert.
     *
     * @param id The identifier of the alert.
     * @return Matching alert.
     */
    public Result get(final String id) {
        final UUID identifier = UUID.fromString(id);
        final Optional<Alert> result = _alertRepository.get(identifier);
        if (!result.isPresent()) {
            return notFound();
        }
        // Return as JSON
        return ok(Json.toJson(result.get()));
    }

    private AlertController(final int maxLimit, final AlertRepository alertRepository) {
        _maxLimit = maxLimit;
        _alertRepository = alertRepository;
    }

    private final int _maxLimit;
    private final AlertRepository _alertRepository;

    private static final int DEFAULT_MAX_LIMIT = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertController.class);
}
