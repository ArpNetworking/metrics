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

package com.arpnetworking.test.junitbenchmarks;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Tests the filtering of HProf results.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class HProfFilterTest {
    @Test
    public void regress() throws IOException {
        // Copy the resource to a real place
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.hprof.txt");
        final Path referenceFile = Paths.get("profile.hprof.filtered.ref.txt");
        final Path referencePath = tmp.resolve(referenceFile);
        final Path filteredFile = Paths.get("profile.hprof.filtered.txt");
        final Path filteredPath = tmp.resolve(filteredFile);
        final Path inputPath = tmp.resolve(inputFile);
        Resources.copy(Resources.getResource(inputFile.toString()), new FileOutputStream(inputPath.toFile()));
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();

        Resources.copy(Resources.getResource(filteredFile.toString()), new FileOutputStream(referencePath.toFile()));
        final List<String> reference = Files.readAllLines(referencePath, Charsets.UTF_8);

        final List<String> lines = Files.readAllLines(filteredPath, Charsets.UTF_8);

        Assert.assertEquals("Expected same number of lines in files", reference.size(), lines.size());
        for (int x = 0; x < reference.size(); ++x) {
            Assert.assertEquals("Line " + (x + 1), reference.get(x), lines.get(x));
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void missingInput() throws IOException {
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.doesnotexist.hprof.txt");
        final Path inputPath = tmp.resolve(inputFile);
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSample() throws IOException {
        // Copy the resource to a real place
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.hprof.invalidSample.txt");
        final Path inputPath = tmp.resolve(inputFile);
        Resources.copy(Resources.getResource(inputFile.toString()), new FileOutputStream(inputPath.toFile()));
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();
    }

    @Test
    public void missingTrace() throws IOException {
        // Copy the resource to a real place
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.hprof.missingTrace.txt");
        final Path referenceFile = Paths.get("profile.hprof.missingTrace.filtered.ref.txt");
        final Path referencePath = tmp.resolve(referenceFile);
        final Path filteredFile = Paths.get("profile.hprof.missingTrace.filtered.txt");
        final Path filteredPath = tmp.resolve(filteredFile);
        final Path inputPath = tmp.resolve(inputFile);
        Resources.copy(Resources.getResource(inputFile.toString()), new FileOutputStream(inputPath.toFile()));
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();

        Resources.copy(Resources.getResource(filteredFile.toString()), new FileOutputStream(referencePath.toFile()));
        final List<String> reference = Files.readAllLines(referencePath, Charsets.UTF_8);

        final List<String> lines = Files.readAllLines(filteredPath, Charsets.UTF_8);

        Assert.assertEquals("Expected same number of lines in files", reference.size(), lines.size());
        for (int x = 0; x < reference.size(); ++x) {
            Assert.assertEquals("Line " + (x + 1), reference.get(x), lines.get(x));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void badTrace() throws IOException {
        // Copy the resource to a real place
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.hprof.badTrace.txt");
        final Path inputPath = tmp.resolve(inputFile);
        Resources.copy(Resources.getResource(inputFile.toString()), new FileOutputStream(inputPath.toFile()));
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();
    }

    @Test
    public void truncatedAfterHeader() throws IOException {
        // Copy the resource to a real place
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.hprof.truncatedAfterHeader.txt");
        final Path filteredFile = Paths.get("profile.hprof.truncatedAfterHeader.filtered.txt");
        final Path filteredPath = tmp.resolve(filteredFile);
        final Path inputPath = tmp.resolve(inputFile);
        Resources.copy(Resources.getResource(inputFile.toString()), new FileOutputStream(inputPath.toFile()));
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();

        final List<String> reference = Files.readAllLines(inputPath, Charsets.UTF_8);

        final List<String> lines = Files.readAllLines(filteredPath, Charsets.UTF_8);

        Assert.assertEquals("Expected same number of lines in files", reference.size(), lines.size());
        for (int x = 0; x < reference.size(); ++x) {
            Assert.assertEquals("Line " + (x + 1), reference.get(x), lines.get(x));
        }
    }

    @Test
    public void truncatedInHeader() throws IOException {
        // Copy the resource to a real place
        final Path tmp = Paths.get("build/tmp");
        final Path inputFile = Paths.get("profile.hprof.truncatedInHeader.txt");
        final Path filteredFile = Paths.get("profile.hprof.truncatedInHeader.filtered.txt");
        final Path filteredPath = tmp.resolve(filteredFile);
        final Path inputPath = tmp.resolve(inputFile);
        Resources.copy(Resources.getResource(inputFile.toString()), new FileOutputStream(inputPath.toFile()));
        final HProfFilter filter = new HProfFilter(inputPath);
        filter.run();

        final List<String> reference = Files.readAllLines(inputPath, Charsets.UTF_8);

        final List<String> lines = Files.readAllLines(filteredPath, Charsets.UTF_8);

        Assert.assertEquals("Expected same number of lines in files", reference.size(), lines.size());
        for (int x = 0; x < reference.size(); ++x) {
            Assert.assertEquals("Line " + (x + 1), reference.get(x), lines.get(x));
        }
    }
}
