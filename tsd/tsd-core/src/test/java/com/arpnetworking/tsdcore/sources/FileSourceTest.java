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
package com.arpnetworking.tsdcore.sources;

import com.arpnetworking.steno.LogBuilder;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.tsdcore.parsers.Parser;
import com.arpnetworking.tsdcore.parsers.exceptions.ParsingException;
import com.arpnetworking.tsdcore.tailer.InitialPosition;
import com.arpnetworking.utility.observer.Observer;
import com.google.common.base.Charsets;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Tests for the FileSource class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class FileSourceTest {

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        _observer = Mockito.mock(Observer.class);
        _parser = Mockito.mock(Parser.class);
        _logger = Mockito.mock(Logger.class);
        _logBuilder = Mockito.mock(LogBuilder.class);
        Mockito.when(_logger.trace()).thenReturn(_logBuilder);
        Mockito.when(_logger.debug()).thenReturn(_logBuilder);
        Mockito.when(_logger.info()).thenReturn(_logBuilder);
        Mockito.when(_logger.warn()).thenReturn(_logBuilder);
        Mockito.when(_logger.error()).thenReturn(_logBuilder);
        Mockito.when(_logBuilder.setMessage(Matchers.anyString())).thenReturn(_logBuilder);
        Mockito.when(_logBuilder.addData(Matchers.anyString(), Matchers.anyString())).thenReturn(_logBuilder);
        Mockito.when(_logBuilder.addContext(Matchers.anyString(), Matchers.anyString())).thenReturn(_logBuilder);
        Mockito.when(_logBuilder.setEvent(Matchers.anyString())).thenReturn(_logBuilder);
        Mockito.when(_logBuilder.setThrowable(Matchers.any(Throwable.class))).thenReturn(_logBuilder);
    }

    @Test
    public void testParseData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testParseData.log");
        final File state = new File(_directory, "testParseData.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData = "Expected Data";
        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);

        Mockito.verify(_parser).parse(expectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_observer).notify(source, expectedData);
        source.stop();
    }

    @Test
    public void testTailFromEnd() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailFromEnd.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData = "Expected Data";
        final String unexpectedData = "Unexpected Data";
        Files.write(
                file.toPath(),
                (unexpectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);

        Mockito.when(_parser.parse(unexpectedData.getBytes(Charsets.UTF_8)))
               .thenThrow(new AssertionError("should not tail from beginning of file"));

        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setInitialPosition(InitialPosition.END)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);

        Mockito.verify(_parser, Mockito.never()).parse(unexpectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_parser).parse(expectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_observer).notify(source, expectedData);
        source.stop();
    }

    @Test
    public void testTailerFileNotFound() throws InterruptedException, IOException {
        final File state = new File(_directory, "testTailerFileNotFound.log.state");
        Files.deleteIfExists(state.toPath());
        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(new File("/dne/" + UUID.randomUUID().toString() + ".log"))
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(INTERVAL / 2);
        Mockito.verify(_logger).warn();
        Mockito.verify(_logBuilder, Mockito.atLeastOnce()).setMessage("Tailer file not found");
        source.stop();
    }

    @Test
    public void testTailerFileNotFoundInterval() throws InterruptedException, IOException {
        final File state = new File(_directory, "testTailerFileNotFoundInterval.log.state");
        Files.deleteIfExists(state.toPath());
        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(new File("/dne/" + UUID.randomUUID().toString() + ".log"))
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        Mockito.verify(_logger).warn();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file not found"));
        Thread.sleep(SLEEP_INTERVAL * 2);
        source.stop();
    }

    @Test
    public void testTailerLogRotationRename() throws IOException, InterruptedException {
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationRename.log");
        final File state = new File(_directory, "testTailerLogRotationRename.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        Files.write(file.toPath(), "Existing data in the log file\n".getBytes(Charsets.UTF_8));

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        renameRotate(file);
        Files.createFile(file.toPath());
        Thread.sleep(2 * SLEEP_INTERVAL);

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage("Tailer file rotate");
        source.stop();
    }

    // TODO(vkoskela): Rotation from empty file to empty file not supported [MAI-189]
    @Ignore
    @Test
    public void testTailerLogRotationRenameFromEmpty() throws IOException, InterruptedException {
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationRenameFromEmpty.log");
        final File state = new File(_directory, "testTailerLogRotationRenameFromEmpty.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        renameRotate(file);
        Files.createFile(file.toPath());
        Thread.sleep(2 * SLEEP_INTERVAL);

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @Test
    public void testTailerLogRotationCopyTruncate() throws IOException, InterruptedException {
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationCopyTruncate.log");
        final File state = new File(_directory, "testTailerLogRotationCopyTruncate.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        Files.write(file.toPath(), "Existing data in the log file\n".getBytes(Charsets.UTF_8));

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        copyRotate(file);
        truncate(file);
        Thread.sleep(2 * SLEEP_INTERVAL);

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Rotation from empty file to empty file not supported [MAI-189]
    // TODO(vkoskela): Copy truncate not supported [MAI-188]
    @Ignore
    @Test
    public void testTailerLogRotationCopyTruncateFromEmpty() throws IOException, InterruptedException {
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationCopyTruncate.log");
        final File state = new File(_directory, "testTailerLogRotationCopyTruncate.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        copyRotate(file);
        truncate(file);
        Thread.sleep(2 * SLEEP_INTERVAL);

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @Test
    public void testTailerLogRotationRenameWithData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationRenameWithData.log");
        final File state = new File(_directory, "testTailerLogRotationRenameWithData.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData = "Expected Data";
        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        renameRotate(file);
        Files.createFile(file.toPath());
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        Thread.sleep(3 * sleepInterval);

        Mockito.verify(_parser).parse(expectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_observer).notify(source, expectedData);
        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder, Mockito.after(10000)).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Copy truncate not supported [MAI-188]
    // In this case the file is copied and truncated before the tailer is able
    // to read the data.  Since the tailer does not understand where the file
    // is copied to it has no chance to read it.
    @Ignore
    @Test
    public void testTailerLogRotationCopyTruncateWithData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationCopyTruncateWithData.log");
        final File state = new File(_directory, "testTailerLogRotationCopyTruncateWithData.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData = "Expected Data";
        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        copyRotate(file);
        truncate(file);
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        Thread.sleep(3 * sleepInterval);

        Mockito.verify(_parser).parse(expectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_observer).notify(source, expectedData);
        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @Test
    public void testTailerLogRotationRenameWithDataToOldAndNew() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationRenameWithDataToOldAndNew.log");
        final File state = new File(_directory, "testTailerLogRotationRenameWithDataToOldAndNew.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        renameRotate(file);
        Files.createFile(file.toPath());
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        Thread.sleep(3 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(2)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData1.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(1)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(2)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData1, notifyValues.get(0));
        Assert.assertEquals(expectedData2, notifyValues.get(1));

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Copy truncate not supported [MAI-188]
    // The tailer has no opportunity to see the data block written immediately
    // before the copy-truncate. This is probably the most difficult case to
    // fix for copy-truncate. Unfortunately, either the tailer needs knowledge
    // of the file rotation scheme (to look for the copied file) or may be able
    // to discover this file with a file system watcher.
    @Ignore
    @Test
    public void testTailerLogRotationCopyTruncateWithDataToOldAndNew() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationCopyTruncateWithDataToOldAndNew.log");
        final File state = new File(_directory, "testTailerLogRotationCopyTruncateWithDataToOldAndNew.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        copyRotate(file);
        truncate(file);
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        Thread.sleep(3 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(2)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData1.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(1)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(2)).notify(source, notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData1, notifyValues.get(0));
        Assert.assertEquals(expectedData2, notifyValues.get(1));

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogRotationRenameDroppedData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationRenameDroppedData.log");
        final File state = new File(_directory, "testTailerLogRotationRenameDroppedData.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2 plus";
        final String expectedData3 = "Expected Data 3";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);
        Mockito.when(_parser.parse(expectedData3.getBytes(Charsets.UTF_8))).thenReturn(expectedData3);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        renameRotate(file);
        Files.createFile(file.toPath());
        Files.write(
                file.toPath(),
                (expectedData3 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(3 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(3)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData1.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(1)));
        Assert.assertTrue("actual=" + new String(parserValues.get(2), Charsets.UTF_8),
                Arrays.equals(expectedData3.getBytes(Charsets.UTF_8), parserValues.get(2)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(3)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData1, notifyValues.get(0));
        Assert.assertEquals(expectedData2, notifyValues.get(1));
        Assert.assertEquals(expectedData3, notifyValues.get(2));

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Copy truncate not supported [MAI-188]
    // The tailer has no opportunity to see the data block written immediately
    // before the copy-truncate. This is probably the most difficult case to
    // fix for copy-truncate. Unfortunately, either the tailer needs knowledge
    // of the file rotation scheme (to look for the copied file) or may be able
    // to discover this file with a file system watcher.
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogCopyTruncateRenameDroppedData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogCopyTruncateRenameDroppedData.log");
        final File state = new File(_directory, "testTailerLogCopyTruncateRenameDroppedData.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2 plus";
        final String expectedData3 = "Expected Data 3";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);
        Mockito.when(_parser.parse(expectedData3.getBytes(Charsets.UTF_8))).thenReturn(expectedData3);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        copyRotate(file);
        truncate(file);
        Files.write(
                file.toPath(),
                (expectedData3 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(3 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(3)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData1.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(1)));
        Assert.assertTrue("actual=" + new String(parserValues.get(2), Charsets.UTF_8),
                Arrays.equals(expectedData3.getBytes(Charsets.UTF_8), parserValues.get(2)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(3)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData1, notifyValues.get(0));
        Assert.assertEquals(expectedData2, notifyValues.get(1));
        Assert.assertEquals(expectedData3, notifyValues.get(2));

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogRotationRenameSmallToLarge() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationRenameSmallToLarge.log");
        final File state = new File(_directory, "testTailerLogRotationRenameSmallToLarge.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData1 = "Expected Data 1 small";
        final String expectedData2 = "Expected Data 2 must be larger";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        renameRotate(file);
        Files.createFile(file.toPath());
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(3 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(2)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserCapture.getValue(), Charsets.UTF_8),
                Arrays.equals(expectedData1.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserCapture.getValue(), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(1)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(2)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData1, notifyValues.get(0));
        Assert.assertEquals(expectedData2, notifyValues.get(1));

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Copy truncate not supported [MAI-188]
    // The small data block is read but the larger block which replaces it
    // immediately after the copy-truncate operation appears to the tailer
    // to just be more data. There is a relatively simple fix to this problem,
    // add a check if the character just before the read position is not a new
    // line character then the file was rotated. This will not cover all cases
    // but should cover a large majority. Beyond this fix the only ways to
    // detect the copy truncate are hash prefix comparison or inode comparison
    // before every read.
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogRotationCopyTruncateSmallToLarge() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        Files.createDirectories(_directory.toPath());
        final File file = new File(_directory, "testTailerLogRotationCopyTruncateSmallToLarge.log");
        final File state = new File(_directory, "testTailerLogRotationCopyTruncateSmallToLarge.log.state");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        Files.deleteIfExists(state.toPath());

        final String expectedData1 = "Expected Data 1 small";
        final String expectedData2 = "Expected Data 2 must be larger";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<>(
                new FileSource.Builder<>()
                        .setSourceFile(file)
                        .setStateFile(state)
                        .setParser(_parser)
                        .setInterval(Duration.millis(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        copyRotate(file);
        truncate(file);
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(3 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(2)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData1.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(1)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(2)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData1, notifyValues.get(0));
        Assert.assertEquals(expectedData2, notifyValues.get(1));

        Mockito.verify(_logger).info();
        Mockito.verify(_logBuilder).setMessage(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    private void renameRotate(final File file) throws IOException {
        final File destination = new File(
                file.getParentFile(),
                file.toPath().getFileName().toString().replaceAll("\\.log", "")
                        + "."
                        + _dateFormat.format(new Date())
                        + ".log");
        Files.deleteIfExists(destination.toPath());
        Assert.assertTrue(file.renameTo(destination));
    }

    private void copyRotate(final File file) throws IOException {
        final File destination = new File(
                file.getParentFile(),
                file.toPath().getFileName().toString().replaceAll("\\.log", "")
                        + "."
                        + _dateFormat.format(new Date())
                        + ".log");
        Files.deleteIfExists(destination.toPath());
        Files.copy(
                file.toPath(),
                destination.toPath());
    }

    private void truncate(final File file) {
        try {
            new FileOutputStream(file).getChannel().truncate(0).close();
        } catch (final IOException e) {
            // Ignore
        }
    }

    private Observer _observer;
    private Logger _logger;
    private LogBuilder _logBuilder;
    private Parser<Object> _parser;
    private final SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH");
    private final File _directory = new File("./target/tmp/test/FileSourceTest");

    private static final long INTERVAL = 50;
    private static final long SLEEP_INTERVAL = INTERVAL + 25;
}
