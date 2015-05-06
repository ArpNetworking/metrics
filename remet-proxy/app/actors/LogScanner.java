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

package actors;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.arpnetworking.play.configuration.ConfigurationHelper;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import models.messages.LogFileAppeared;
import models.messages.LogFileDisappeared;
import play.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Actor responsible for discovering files that are created or removed.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class LogScanner extends UntypedActor {

    /**
     * Public constructor.
     *
     * @param configuration Play app configuration
     * @param fileSourceManager <code>ActorRef</code> for the singleton FileSourceManager actor
     */
    @Inject
    public LogScanner(final Configuration configuration, @Named("FileSourceManager") final ActorRef fileSourceManager) {

        _fileSourceManagerActor = fileSourceManager;
        _logs = FluentIterable
                .from(configuration.getStringList("logs"))
                .transform(Paths::get)
                .toList();

        LOGGER.debug()
                .setMessage("Created log scanner")
                .addData("actor", self().toString())
                .addData("fileSourceManagerActor", _fileSourceManagerActor.toString())
                .addData("logs", _logs)
                .log();

        _nonExistingLogs.addAll(_logs);

        context().system().scheduler().schedule(
                ConfigurationHelper.getFiniteDuration(configuration, "logScanner.initialDelay"),
                ConfigurationHelper.getFiniteDuration(configuration, "logScanner.interval"),
                self(),
                "tick",
                context().dispatcher(),
                self());
    }

    /* package private */ LogScanner(final List<Path> logs, final ActorRef fileSourceManager) {
        _fileSourceManagerActor = fileSourceManager;
        _logs = Lists.newArrayList(logs);
        _nonExistingLogs.addAll(_logs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Object message) throws Exception {
        if ("tick".equals(message)) {
            LOGGER.debug()
                    .setMessage("Searching for created/deleted logs")
                    .addData("actor", self().toString())
                    .addData("scanner", this.toString())
                    .log();
            for (final Path logFile : _logs) {
                if (_nonExistingLogs.contains(logFile) && Files.exists(logFile)) {
                    LOGGER.info()
                            .setMessage("Log file materialized")
                            .addData("actor", self().toString())
                            .addData("file", logFile)
                            .log();
                    _fileSourceManagerActor.tell(new LogFileAppeared(logFile), getSelf());
                    _nonExistingLogs.remove(logFile);
                    _existingLogs.add(logFile);
                } else if (_existingLogs.contains(logFile) && Files.notExists(logFile)) {
                    LOGGER.info()
                            .setMessage("Log file vanished")
                            .addData("actor", self().toString())
                            .addData("file", logFile)
                            .log();
                    _fileSourceManagerActor.tell(new LogFileDisappeared(logFile), getSelf());
                    _existingLogs.remove(logFile);
                    _nonExistingLogs.add(logFile);
                }
            }
        } else {
            unhandled(message);
        }
    }

    private final ActorRef _fileSourceManagerActor;
    private final List<Path> _logs;
    private final Set<Path> _existingLogs = Sets.newHashSet();
    private final Set<Path> _nonExistingLogs = Sets.newHashSet();

    private static final Logger LOGGER = LoggerFactory.getLogger(LogScanner.class);
}
