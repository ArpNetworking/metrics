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
import com.arpnetworking.remet.gui.hosts.HostRepository;
import com.arpnetworking.remet.gui.hosts.impl.HostProviderScheduler;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import play.Configuration;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.concurrent.Callable;
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
                .annotatedWith(Names.named("HostProviderScheduler"))
                .toProvider(HostProviderSchedulerProvider.class)
                .asEagerSingleton();
    }

    @Provides
    @Singleton
    private HostRepository getHostRepository(
            final Configuration config,
            final Injector injector,
            final ApplicationLifecycle lifecycle) {
        final HostRepository hostRepository = injector.getInstance(
                ConfigurationHelper.<HostRepository>getType(config, "hostRepository.type"));
        hostRepository.open();
        lifecycle.addStopHook(new Callable<F.Promise<Void>>() {
            @Override
            public F.Promise<Void> call() throws Exception {
                hostRepository.close();
                return F.Promise.pure(null);
            }
        });
        return hostRepository;
    }

    @Singleton
    @Named("HostProviderProps")
    @Provides
    private Props getHostProviderProps(final Injector injector, final Configuration config) {
        return Props.create(
                new GuiceActorCreator(
                        injector,
                        ConfigurationHelper.<Actor>getType(config, "hostProvider.type")));
    }

    @Provides
    @Singleton
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

    private static final class HostProviderSchedulerProvider implements Provider<ActorRef> {
        @Inject
        public HostProviderSchedulerProvider(
                final ActorSystem system,
                @Named("HostProviderProps")
                final Props hostProviderProps,
                final Configuration configuration) {
            _system = system;
            _hostProviderProps = hostProviderProps;
            _configuration = configuration;
        }

        @Override
        public ActorRef get() {
            final FiniteDuration hostProviderInitialDelay =
                    ConfigurationHelper.getFiniteDuration(_configuration, "hostProvider.initialDelay");
            final FiniteDuration hostProviderInterval =
                    ConfigurationHelper.getFiniteDuration(_configuration, "hostProvider.interval");

            final Cluster cluster = Cluster.get(_system);
            // Start a singleton instance of the scheduler on a "host_indexer" node in the cluster.
            if (cluster.selfRoles().contains(INDEXER_ROLE)) {
                return _system.actorOf(ClusterSingletonManager.defaultProps(
                        HostProviderScheduler.props(
                                _hostProviderProps,
                                hostProviderInitialDelay,
                                hostProviderInterval),
                        "host-provider-scheduler",
                        PoisonPill.getInstance(),
                        INDEXER_ROLE));
            }
            return null;
        }

        private final ActorSystem _system;
        private final Props _hostProviderProps;
        private final Configuration _configuration;

        private static final String INDEXER_ROLE = "host_indexer";
    }
}
