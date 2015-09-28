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

import actors.JvmMetricsCollector;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.contrib.pattern.ClusterSingletonManager;
import com.arpnetworking.guice.akka.GuiceActorCreator;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;
import com.arpnetworking.play.configuration.ConfigurationHelper;
import com.arpnetworking.remet.gui.alerts.AlertRepository;
import com.arpnetworking.remet.gui.expressions.ExpressionRepository;
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import play.Configuration;
import play.Environment;
import play.inject.ApplicationLifecycle;
import play.libs.F;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Module that defines the main bindings.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class MainModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(ActorRef.class)
                .annotatedWith(Names.named("JvmMetricsCollector"))
                .toProvider(JvmMetricsCollectorProvider.class)
                .asEagerSingleton();
        bind(HostRepository.class)
                .toProvider(HostRepositoryProvider.class)
                .asEagerSingleton();
        bind(AlertRepository.class)
                .toProvider(AlertRepositoryProvider.class)
                .asEagerSingleton();
        bind(ExpressionRepository.class)
                .toProvider(ExpressionRepositoryProvider.class)
                .asEagerSingleton();
        bind(ActorRef.class)
                .annotatedWith(Names.named("HostProviderScheduler"))
                .toProvider(HostProviderProvider.class)
                .asEagerSingleton();
    }

    @Singleton
    @Named("HostProviderProps")
    @Provides
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD") // Invoked reflectively by Guice
    private Props getHostProviderProps(final Injector injector, final Environment environment, final Configuration config) {
        return
                GuiceActorCreator.props(
                        injector,
                        ConfigurationHelper.<Actor>getType(environment, config, "hostProvider.type"));
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

    private static final class HostRepositoryProvider implements Provider<HostRepository> {

        @Inject
        public HostRepositoryProvider(
                final Injector injector,
                final Environment environment,
                final Configuration configuration,
                final ApplicationLifecycle lifecycle) {
            _injector = injector;
            _environment = environment;
            _configuration = configuration;
            _lifecycle = lifecycle;
        }

        @Override
        public HostRepository get() {
            final HostRepository hostRepository = _injector.getInstance(
                    ConfigurationHelper.<HostRepository>getType(_environment, _configuration, "hostRepository.type"));
            hostRepository.open();
            _lifecycle.addStopHook(
                    () -> {
                        hostRepository.close();
                        return F.Promise.pure(null);
                    });
            return hostRepository;
        }

        private final Injector _injector;
        private final Environment _environment;
        private final Configuration _configuration;
        private final ApplicationLifecycle _lifecycle;
    }

    private static final class ExpressionRepositoryProvider implements Provider<ExpressionRepository> {

        @Inject
        public ExpressionRepositoryProvider(
                final Injector injector,
                final Environment environment,
                final Configuration configuration,
                final ApplicationLifecycle lifecycle) {
            _injector = injector;
            _environment = environment;
            _configuration = configuration;
            _lifecycle = lifecycle;
        }

        @Override
        public ExpressionRepository get() {
            final ExpressionRepository expressionRepository = _injector.getInstance(
                    ConfigurationHelper.<ExpressionRepository>getType(_environment, _configuration, "expressionRepository.type"));
            expressionRepository.open();
            _lifecycle.addStopHook(
                    () -> {
                        expressionRepository.close();
                        return F.Promise.pure(null);
                    });
            return expressionRepository;
        }

        private final Injector _injector;
        private final Environment _environment;
        private final Configuration _configuration;
        private final ApplicationLifecycle _lifecycle;
    }

    private static final class AlertRepositoryProvider implements Provider<AlertRepository> {

        @Inject
        public AlertRepositoryProvider(
                final Injector injector,
                final Environment environment,
                final Configuration configuration,
                final ApplicationLifecycle lifecycle) {
            _injector = injector;
            _environment = environment;
            _configuration = configuration;
            _lifecycle = lifecycle;
        }

        @Override
        public AlertRepository get() {
            final AlertRepository alertRepository = _injector.getInstance(
                    ConfigurationHelper.<AlertRepository>getType(_environment, _configuration, "alertRepository.type"));
            alertRepository.open();
            _lifecycle.addStopHook(
                    () -> {
                        alertRepository.close();
                        return F.Promise.pure(null);
                    });
            return alertRepository;
        }

        private final Injector _injector;
        private final Environment _environment;
        private final Configuration _configuration;
        private final ApplicationLifecycle _lifecycle;
    }

    private static final class HostProviderProvider implements Provider<ActorRef> {
        @Inject
        public HostProviderProvider(
                final ActorSystem system,
                @Named("HostProviderProps")
                final Props hostProviderProps) {
            _system = system;
            _hostProviderProps = hostProviderProps;
        }

        @Override
        public ActorRef get() {
            final Cluster cluster = Cluster.get(_system);
            // Start a singleton instance of the scheduler on a "host_indexer" node in the cluster.
            if (cluster.selfRoles().contains(INDEXER_ROLE)) {
                return _system.actorOf(ClusterSingletonManager.defaultProps(
                                _hostProviderProps,
                                "host-provider-scheduler",
                                PoisonPill.getInstance(),
                                INDEXER_ROLE));
            }
            return null;
        }

        private final ActorSystem _system;
        private final Props _hostProviderProps;

        private static final String INDEXER_ROLE = "host_indexer";
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
