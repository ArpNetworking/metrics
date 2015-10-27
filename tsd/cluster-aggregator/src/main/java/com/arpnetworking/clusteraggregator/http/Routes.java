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

package com.arpnetworking.clusteraggregator.http;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Member;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.CacheControl;
import akka.http.javadsl.model.headers.CacheDirectives;
import akka.japi.JavaPartialFunction;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.arpnetworking.clusteraggregator.Status;
import com.arpnetworking.clusteraggregator.models.StatusResponse;
import com.arpnetworking.configuration.jackson.akka.AkkaModule;
import com.arpnetworking.jackson.ObjectMapperFactory;
import com.arpnetworking.metrics.Metrics;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Timer;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Http server routes.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class Routes extends AbstractFunction1<HttpRequest, Future<HttpResponse>> {

    /**
     * Public constructor.
     *
     * @param actorSystem Instance of <code>ActorSystem</code>.
     * @param metricsFactory Instance of <code>MetricsFactory</code>.
     */
    public Routes(final ActorSystem actorSystem, final MetricsFactory metricsFactory) {
        _actorSystem = actorSystem;
        _metricsFactory = metricsFactory;
        _mapper.registerModule(new SimpleModule().addSerializer(Member.class, new MemberSerializer()));
        _mapper.registerModule(new AkkaModule(actorSystem));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<HttpResponse> apply(final HttpRequest request) {
        final Metrics metrics = _metricsFactory.create();
        final Timer timer = metrics.createTimer(createTimerName(request));
        LOGGER.trace()
                .setMessage("Request")
                .addData("request", request)
                .log();
        final Future<HttpResponse> futureResponse = process(request);
        futureResponse.onComplete(
                new OnComplete<HttpResponse>() {
                    @Override
                    public void onComplete(final Throwable failure, final HttpResponse response) {
                        timer.close();
                        metrics.close();
                        LOGGER.trace()
                                .setMessage("Response")
                                .addData("response", response)
                                .log();
                    }
                },
                _actorSystem.dispatcher());
        return futureResponse;
    }

    private Future<HttpResponse> process(final HttpRequest request) {
        if (HttpMethods.GET.equals(request.method())) {
            if (PING_PATH.equals(request.getUri().path())) {
                return ask("/user/status", new Status.HealthRequest(), Boolean.FALSE)
                        .map(
                                new Mapper<Boolean, HttpResponse>() {
                                    @Override
                                    public HttpResponse apply(final Boolean isHealthy) {
                                        return (HttpResponse) response()
                                                .withStatus(isHealthy ? StatusCodes.OK : StatusCodes.INTERNAL_SERVER_ERROR)
                                                .addHeader(PING_CACHE_CONTROL_HEADER)
                                                .withEntity(JSON_CONTENT_TYPE,
                                                            "{\"status\":\""
                                                                    + (isHealthy ? HEALTHY_STATE : UNHEALTHY_STATE)
                                                                    + "\"}");
                                    }
                                },
                                _actorSystem.dispatcher());
            } else if (STATUS_PATH.equals(request.getUri().path())) {
                return ask("/user/status", new Status.StatusRequest(), (StatusResponse) null)
                        .map(
                                new Mapper<StatusResponse, HttpResponse>() {
                                    @Override
                                    public HttpResponse checkedApply(final StatusResponse status) throws JsonProcessingException {
                                        return (HttpResponse) response()
                                                .withEntity(
                                                        JSON_CONTENT_TYPE,
                                                        _mapper.writeValueAsString(status));
                                    }
                                },
                                _actorSystem.dispatcher());
            }
        }
        return Futures.successful((HttpResponse) response().withStatus(404));
    }

    private HttpResponse response() {
        return HttpResponse.create();
    }

    private <T> Future<T> ask(final String actorPath, final Object request, final T defaultValue) {
        return _actorSystem.actorSelection(actorPath)
                .resolveOne(TIMEOUT)
                .flatMap(
                        new Mapper<ActorRef, Future<T>>() {
                            @Override
                            public Future<T> apply(final ActorRef actor) {
                                @SuppressWarnings("unchecked")
                                final Future<T> future = (Future<T>) Patterns.ask(
                                        actor,
                                        request,
                                        TIMEOUT);
                                return future;
                            }
                        },
                        _actorSystem.dispatcher())
                .recover(
                        new JavaPartialFunction<Throwable, T>() {
                            @Override
                            public T apply(final Throwable t, final boolean isCheck) throws Exception {
                                LOGGER.error()
                                        .setMessage("Error when asking actor")
                                        .setThrowable(t)
                                        .addData("actorPath", actorPath)
                                        .addData("request", request)
                                        .log();
                                return defaultValue;
                            }
                        },
                        _actorSystem.dispatcher());
    }

    private String createTimerName(final HttpRequest request) {
        final StringBuilder nameBuilder = new StringBuilder()
                .append("rest_service/")
                .append(request.method().value());
        if (!request.getUri().path().startsWith("/")) {
            nameBuilder.append("/");
        }
        nameBuilder.append(request.getUri().path());
        return nameBuilder.toString();
    }


    private final ActorSystem _actorSystem;
    private final MetricsFactory _metricsFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(Routes.class);

    // Ping
    private static final String PING_PATH = "/ping";
    private static final String STATUS_PATH = "/status";
    private static final HttpHeader PING_CACHE_CONTROL_HEADER = CacheControl.create(
            CacheDirectives.PRIVATE(),
            CacheDirectives.NO_CACHE,
            CacheDirectives.NO_STORE,
            CacheDirectives.MUST_REVALIDATE);
    private static final String UNHEALTHY_STATE = "UNHEALTHY";
    private static final String HEALTHY_STATE = "HEALTHY";

    private static final ContentType JSON_CONTENT_TYPE = ContentType.create(MediaTypes.APPLICATION_JSON);
    private static final Timeout TIMEOUT = Timeout.apply(5, TimeUnit.SECONDS);
    private final ObjectMapper _mapper = ObjectMapperFactory.createInstance();

    private static class MemberSerializer extends JsonSerializer<Member> {
        @Override
        public void serialize(
                final Member value,
                final JsonGenerator jgen,
                final SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeObjectField("address", value.address().toString());
            jgen.writeObjectField("roles", value.getRoles().toArray());
            jgen.writeEndObject();
        }
    }
}
