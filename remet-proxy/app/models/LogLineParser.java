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

package models;

import com.arpnetworking.metrics.com.arpnetworking.steno.Logger;
import com.arpnetworking.metrics.com.arpnetworking.steno.LoggerFactory;
import com.arpnetworking.tsdcore.parsers.Parser;
import models.messages.LogLine;

import java.nio.file.Path;

/**
 * Represents raw log lines read from a file.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LogLineParser implements Parser<LogLine> {

    /**
     * Public constructor.
     *
     * @param logFile File the parser is attached to.
     */
    public LogLineParser(final Path logFile) {
        _logFile = logFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogLine parse(final byte[] data) throws IllegalArgumentException {
        if (null == data) {
            LOGGER.error().setMessage("Null data sent to the FilesSource Parser").log();
            return null;
        }

        return new LogLine(_logFile, data);
    }

    private final Path _logFile;

    private static final Logger LOGGER = LoggerFactory.getLogger(LogLineParser.class);
}
