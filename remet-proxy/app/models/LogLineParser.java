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

import com.arpnetworking.tsdcore.parsers.Parser;
import models.messages.LogLine;
import org.joda.time.DateTime;
import play.Logger;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses logs to extract LogLines.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class LogLineParser implements Parser<LogLine> {
    //TODO(barp): Rework this class [MAI-409]
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
            Logger.error("Null data sent to the FilesSource Parser");
            return null;
        }

        // CHECKSTYLE.OFF: IllegalInstantiation - we need to turn the bytes into a String
        final String logLine = new String(data);
        // CHECKSTYLE.ON: IllegalInstantiation
        final Matcher timestampMatcher = CONCAT_PATTERN.matcher(logLine);

        if (timestampMatcher.find()) {
            final String timestamp = timestampMatcher.group(1); //print out the timestamp
            return new LogLine(_logFile, logLine, DateTime.parse(timestamp));
        }

        return new LogLine(_logFile, logLine, DateTime.now());
    }

    private final Path _logFile;

    //private static Pattern _timeStampPattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4}");
    //private static Pattern _ios8601Patter = Pattern.compile(
            // "^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])
                    // (\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))
                    // ([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|
                    // ([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$");
    private static final Pattern CONCAT_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4}| ^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])"
                    + "(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]"
                    + "\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]"
                    + "\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$");
}
