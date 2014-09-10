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

import com.arpnetworking.tsdcore.parsers.Parser;
import com.arpnetworking.tsdcore.parsers.exceptions.ParsingException;
import com.arpnetworking.utility.observer.Observer;
import com.google.common.base.Charsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
    }

    @Test
    public void testParseData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testParseData.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData = "Expected Data";
        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
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
    public void testTailerFileNotFound() throws InterruptedException {
        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath("/path/does/not/exist.log")
                        .setParser(_parser)
                        .setInterval(Long.valueOf(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(INTERVAL / 2);
        Mockito.verify(_logger).warn(Mockito.anyString());
        source.stop();
    }

    @Test
    public void testTailerFileNotFoundInterval() throws InterruptedException {
        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath("/path/does/not/exist.log")
                        .setParser(_parser)
                        .setInterval(Long.valueOf(INTERVAL)),
                _logger);

        Mockito.reset(_logger);
        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        Mockito.verify(_logger).warn(Mockito.contains("Tailer file not found"));
        Mockito.reset(_logger);
        Thread.sleep(SLEEP_INTERVAL * 2);
        Mockito.verifyZeroInteractions(_logger);
        source.stop();
    }

    @Test
    public void testTailerLogRotationRename() throws IOException, InterruptedException {
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationRename.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        // TODO(vkoskela): Rotation not detected on empty file [MAI-189]
        Files.write(file.toPath(), "Existing data in the log file\n".getBytes(Charsets.UTF_8));

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        Mockito.reset(_logger);
        renameRotate(file);
        Files.createFile(file.toPath());
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * SLEEP_INTERVAL);

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @Test
    public void testTailerLogRotationCopyTruncate() throws IOException, InterruptedException {
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationCopyTruncate.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());
        // TODO(vkoskela): Rotation not detected on empty file [MAI-189]
        Files.write(file.toPath(), "Existing data in the log file\n".getBytes(Charsets.UTF_8));

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(INTERVAL)),
                _logger);

        source.start();
        Thread.sleep(SLEEP_INTERVAL);
        Mockito.reset(_logger);
        copyRotate(file);
        truncate(file);
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * SLEEP_INTERVAL);

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Intermittent failures caused by race condition [MAI-190]
    @Ignore
    @Test
    public void testTailerLogRotationRenameWithData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationRenameWithData.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData = "Expected Data";
        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
        Files.write(
                file.toPath(),
                (expectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        renameRotate(file);
        Files.createFile(file.toPath());
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

        Mockito.verify(_parser).parse(expectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_observer).notify(source, expectedData);
        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): FileSource support reopen reads [MAI-188]
    @Ignore
    @Test
    public void testTailerLogRotationCopyTruncateWithData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationCopyTruncateWithData.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData = "Expected Data";
        Mockito.when(_parser.parse(expectedData.getBytes(Charsets.UTF_8))).thenReturn(expectedData);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
        Files.write(
                file.toPath(),
                (expectedData + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        copyRotate(file);
        truncate(file);
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        Thread.sleep(2 * sleepInterval);

        Mockito.verify(_parser).parse(expectedData.getBytes(Charsets.UTF_8));
        Mockito.verify(_observer).notify(source, expectedData);
        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    @Test
    public void testTailerLogRotationRenameWithDataToOldAndNew() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationRenameWithDataToOldAndNew.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
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
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

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

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): FileSource support reopen reads [MAI-188]
    @Ignore
    @Test
    public void testTailerLogRotationCopyTruncateWithDataToOldAndNew() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationCopyTruncateWithDataToOldAndNew.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
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
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

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

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Premature log rotation [MAI-187]
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogRotationRenameDroppedData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationRenameDroppedData.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2 plus";
        final String expectedData3 = "Expected Data 3";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);
        Mockito.when(_parser.parse(expectedData3.getBytes(Charsets.UTF_8))).thenReturn(expectedData3);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
        Mockito.reset(_parser);
        Mockito.reset(_observer);
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
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(2)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData3.getBytes(Charsets.UTF_8), parserValues.get(1)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(2)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData2, notifyValues.get(0));
        Assert.assertEquals(expectedData3, notifyValues.get(1));

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Premature log rotation [MAI-187]
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogCopyTruncateRenameDroppedData() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogCopyTruncateRenameDroppedData.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData1 = "Expected Data 1 must be larger";
        final String expectedData2 = "Expected Data 2 plus";
        final String expectedData3 = "Expected Data 3";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);
        Mockito.when(_parser.parse(expectedData3.getBytes(Charsets.UTF_8))).thenReturn(expectedData3);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
        Mockito.reset(_parser);
        Mockito.reset(_observer);
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
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser, Mockito.times(2)).parse(parserCapture.capture());
        final List<byte[]> parserValues = parserCapture.getAllValues();
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserValues.get(0), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserValues.get(0)));
        Assert.assertTrue("actual=" + new String(parserValues.get(1), Charsets.UTF_8),
                Arrays.equals(expectedData3.getBytes(Charsets.UTF_8), parserValues.get(1)));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer, Mockito.times(2)).notify(Matchers.eq(source), notifyCapture.capture());
        final List<Object> notifyValues = notifyCapture.getAllValues();
        Assert.assertEquals(expectedData2, notifyValues.get(0));
        Assert.assertEquals(expectedData3, notifyValues.get(1));

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): Misses rotation on small file to larger file [MAI-189]
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogRotationRenameSmallToLarge() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationRenameSmallToLarge.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData1 = "Expected Data 1 small";
        final String expectedData2 = "Expected Data 2 must be larger";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
        Mockito.reset(_parser);
        Mockito.reset(_observer);
        renameRotate(file);
        Files.createFile(file.toPath());
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser).parse(parserCapture.capture());
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserCapture.getValue(), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserCapture.getValue()));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer).notify(Matchers.eq(source), notifyCapture.capture());
        Assert.assertEquals(expectedData2, notifyCapture.getValue());

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
        source.stop();
    }

    // TODO(vkoskela): FileSource support reopen reads [MAI-188]
    // TODO(vkoskela): Misses rotation on small file to larger file [MAI-189]
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testTailerLogRotationCopyTruncateSmallToLarge() throws IOException, InterruptedException, ParsingException {
        final long interval = 500;
        final long sleepInterval = 600;
        final File directory = new File("./target/FileSourceTest");
        Files.createDirectories(directory.toPath());
        final File file = new File(directory, "testTailerLogRotationCopyTruncateSmallToLarge.log");
        Files.deleteIfExists(file.toPath());
        Files.createFile(file.toPath());

        final String expectedData1 = "Expected Data 1 small";
        final String expectedData2 = "Expected Data 2 must be larger";
        Mockito.when(_parser.parse(expectedData1.getBytes(Charsets.UTF_8))).thenReturn(expectedData1);
        Mockito.when(_parser.parse(expectedData2.getBytes(Charsets.UTF_8))).thenReturn(expectedData2);

        final FileSource<Object> source = new FileSource<Object>(
                new FileSource.Builder<Object>()
                        .setFilePath(file.getAbsolutePath())
                        .setParser(_parser)
                        .setInterval(Long.valueOf(interval)),
                _logger);

        source.attach(_observer);
        source.start();

        Files.write(
                file.toPath(),
                (expectedData1 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Thread.sleep(sleepInterval);
        Mockito.reset(_logger);
        Mockito.reset(_parser);
        Mockito.reset(_observer);
        copyRotate(file);
        truncate(file);
        Files.write(
                file.toPath(),
                (expectedData2 + "\n").getBytes(Charsets.UTF_8),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        Mockito.verifyZeroInteractions(_parser);
        Mockito.verifyZeroInteractions(_observer);
        // Two writes across a file rotation are always processed in separate
        // intervals by the Apache IO Tailer.
        Thread.sleep(2 * sleepInterval);

        final ArgumentCaptor<byte[]> parserCapture = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Object> notifyCapture = ArgumentCaptor.forClass(Object.class);

        Mockito.verify(_parser).parse(parserCapture.capture());
        // CHECKSTYLE.OFF: IllegalInstantiation - This is ok for String from byte[]
        Assert.assertTrue("actual=" + new String(parserCapture.getValue(), Charsets.UTF_8),
                Arrays.equals(expectedData2.getBytes(Charsets.UTF_8), parserCapture.getValue()));
        // CHECKSTYLE.ON: IllegalInstantiation

        Mockito.verify(_observer).notify(Matchers.eq(source), notifyCapture.capture());
        Assert.assertEquals(expectedData2, notifyCapture.getValue());

        Mockito.verify(_logger).info(Mockito.contains("Tailer file rotate"));
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
    private Parser<Object> _parser;
    private final SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH");

    private static final long INTERVAL = 50;
    private static final long SLEEP_INTERVAL = INTERVAL + 25;
}
