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
package controllers;

import com.arpnetworking.remet.gui.hosts.Host;
import com.arpnetworking.remet.gui.hosts.HostQuery;
import com.arpnetworking.remet.gui.hosts.HostQueryResult;
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.arpnetworking.remet.gui.hosts.MetricsSoftwareState;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import models.PagedContainer;
import models.Pagination;
import play.Configuration;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Map;

/**
 * Real time metrics web interface (ReMetGui) host controller. Provides the
 * application programming interface (API) for querying information about hosts
 * available in the network.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class HostController extends Controller {

    /**
     * Public constructor.
     *
     * @param configuration Instance of Play's <code>Configuration</code>.
     * @param hostRepository Instance of <code>HostRepository</code>.
     */
    @Inject
    public HostController(final Configuration configuration, final HostRepository hostRepository) {
        this(configuration.getInt("hosts.limit", Integer.valueOf(DEFAULT_LIMIT)).intValue(), hostRepository);
    }

    /**
     * Query for hosts.
     *
     * @param name The complete or partial name of the host. Optional.
     * @param state The state of the metrics software on the host. Optional.
     * @param limit The maximum number of results to return. Optional.
     * @param offset The number of results to skip. Optional.
     * @param sort_by The field to sort results by. Optional.
     * @return <code>Result</code> paginated matching hosts.
     */
    // CHECKSTYLE.OFF: ParameterNameCheck - Names must match query parameters.
    public Result query(
            final String name,
            final String state,
            final Integer limit,
            final Integer offset,
            final String sort_by) {
        // CHECKSTYLE.ON: ParameterNameCheck

        // Convert and validate parameters
        final MetricsSoftwareState stateValue;
        try {
            stateValue = state == null ? null : MetricsSoftwareState.valueOf(state);
        } catch (final IllegalArgumentException iae) {
            return badRequest("Invalid state argument");
        }
        final HostQuery.Field sortByValue;
        try {
            sortByValue = sort_by == null ? null : HostQuery.Field.valueOf(sort_by);
        } catch (final IllegalArgumentException iae) {
            return badRequest("Invalid sort_by argument");
        }
        final Optional<String> argName = Optional.fromNullable(name);
        final Optional<MetricsSoftwareState> argState = Optional.fromNullable(stateValue);
        final Optional<Integer> argLimit = Optional.of(MoreObjects.firstNonNull(limit, Integer.valueOf(_defaultLimit)));
        final Optional<Integer> argOffset = Optional.fromNullable(offset);
        final Optional<HostQuery.Field> argSortBy = Optional.fromNullable(sortByValue);
        if (argLimit.isPresent() && argLimit.get().intValue() < 0) {
            return badRequest("Invalid limit; must be greater than or equal to 0");
        }
        if (argOffset.isPresent() && argOffset.get().intValue() < 0) {
            return badRequest("Invalid offset; must be greater than or equal to 0");
        }

        // Build conditions map
        final Map<String, String> conditions = Maps.newHashMap();
        if (argName.isPresent()) {
            conditions.put("name", argName.get());
        }
        if (argState.isPresent()) {
            conditions.put("state", argState.get().toString());
        }
        if (argSortBy.isPresent()) {
            conditions.put("sort_by", argSortBy.get().toString());
        }

        // Build a host repository query
        final HostQuery query = _hostRepository.createQuery()
            .hostName(argName)
            .metricsSoftwareState(argState)
            .limit(argLimit)
            .offset(argOffset)
            .sortBy(argSortBy);

        // Execute the query
        final HostQueryResult result = query.execute();

        // Wrap the query results and return as JSON
        return ok(Json.toJson(new PagedContainer<Host>(
                result.hosts(),
                new Pagination(
                        "hosts/v1/query",
                        result.total(),
                        result.hosts().size(),
                        argLimit,
                        argOffset,
                        conditions))));
    }

    private HostController(final int defaultLimit, final HostRepository hostRepository) {
        _defaultLimit = defaultLimit;
        _hostRepository = hostRepository;
    }

    private final int _defaultLimit;
    private final HostRepository _hostRepository;

    private static final int DEFAULT_LIMIT = 1000;
}
