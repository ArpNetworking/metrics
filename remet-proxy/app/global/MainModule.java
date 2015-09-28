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

import actors.FileSourcesManager;
import actors.JvmMetricsCollector;
import actors.LogScanner;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.arpnetworking.guice.akka.GuiceActorCreator;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import models.StreamContext;
import play.Configuration;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Module that defines the main bindings.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public final class MainModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(ActorRef.class)
                .annotatedWith(Names.named("JvmMetricsCollector"))
                .toProvider(JvmMetricsCollectorProvider.class)
                .asEagerSingleton();
        bind(ActorRef.class).annotatedWith(Names.named("LogScanner")).toProvider(LogScannerProvider.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Named("StreamContext")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD") // Invoked reflectively by Guice
    private ActorRef getStreamContext(final ActorSystem system, final MetricsFactory metricsFactory) {
        return system.actorOf(Props.create(StreamContext.class, metricsFactory));
    }

    @Provides
    @Singleton
    @Named("FileSourceManager")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD") // Invoked reflectively by Guice
    private ActorRef getFileSourceManager(
            final Injector injector,
            final ActorSystem system) {
        return system.actorOf(
                GuiceActorCreator.props(
                        injector,
                        FileSourcesManager.class),
                "FileSourceManagerActor");
    }

    @Provides
    @Singleton
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD") // Invoked reflectively by Guice
    private MetricsFactory getMetricsFactory(final Configuration configuration) {
        return new TsdMetricsFactory.Builder()
                .setSinks(Collections.singletonList(
                        new TsdQueryLogSink.Builder()
                                .setName(configuration.getString("metrics.name"))
                                .setPath(configuration.getString("metrics.path"))
                                .build()
                ))
                .build();
    }

    private static final class LogScannerProvider implements Provider<ActorRef> {
        @Inject
        public LogScannerProvider(final Injector injector, final ActorSystem system) {
            _injector = injector;
            _system = system;
        }

        @Override
        public ActorRef get() {
            return _system.actorOf(GuiceActorCreator.props(_injector, LogScanner.class));
        }

        private final Injector _injector;
        private final ActorSystem _system;
    }

    private static final class JvmMetricsCollectorProvider implements Provider<ActorRef> {
        @Inject
        public JvmMetricsCollectorProvider(final Injector injector, final ActorSystem system) {
            _injector = injector;
            _system = system;
        }

        @Override
        public ActorRef get() {
            return _system.actorOf(GuiceActorCreator.props(_injector, JvmMetricsCollector.class));
        }

        private final Injector _injector;
        private final ActorSystem _system;
    }
}
