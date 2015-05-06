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
package com.arpnetworking.configuration.triggers;

import com.arpnetworking.configuration.Trigger;
import com.arpnetworking.utility.OvalBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import net.sf.oval.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * <code>Trigger</code> implementation based on a file's modified date and
 * its hash. If the file is created or removed the evaluation will return
 * true.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class FileTrigger implements Trigger {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluateAndReset() {
        final boolean exists = _file.exists();
        if (_exists ^ exists) {
            LOGGER.debug(String.format(
                    "File created or removed; file=%s previousExists=%s newExists=%s",
                    _file,
                    _exists,
                    exists));

            _exists = exists;
            _lastModified = _file.lastModified();
            _hash = createHash(_file);

            return true;
        } else if (exists) {
            final long lastModified = _file.lastModified();
            if (lastModified > _lastModified) {
                _lastModified = lastModified;
                final byte[] hash = createHash(_file);
                if (!Arrays.equals(hash, _hash)) {
                    LOGGER.debug(String.format(
                            "File modified and changes found; file=%s",
                            _file));

                    _hash = hash;
                    return true;
                } else {
                    LOGGER.debug(String.format(
                            "File modified but no changes found; file=%s",
                            _file));
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FileTrigger.class)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("File", _file)
                .toString();
    }

    private byte[] createHash(final File file) {
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            final byte[] bytesBuffer = new byte[1024];
            int bytesRead = -1;
            _md5.reset();
            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                _md5.update(bytesBuffer, 0, bytesRead);
            }
            return _md5.digest();
        } catch (final IOException ex) {
            return null;
        }
    }

    private FileTrigger(final Builder builder) {
        // The file trigger should always return true on the first evaluation
        // while on subsequent evaluations true should only be returned if the
        // file was created, removed or changed since the previous evaluation.
        // To accomplish this the file is initially considered to exist with
        // a modified time of -1 and a null hash.
        _file = builder._file;
        _exists = true;
        _lastModified = -1;
        _hash = null;

        try {
            _md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }
    }

    private final File _file;
    private final MessageDigest _md5;

    private boolean _exists;
    private long _lastModified;
    private byte[] _hash;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTrigger.class);

    /**
     * Builder for <code>FileTrigger</code>.
     */
    public static final class Builder extends OvalBuilder<FileTrigger> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(FileTrigger.class);
        }

        /**
         * Set the <code>File</code> to monitor. Cannot be null.
         *
         * @param value The <code>File</code> to monitor.
         * @return This <code>Builder</code> instance.
         */
        public Builder setFile(final File value) {
            _file = value;
            return this;
        }

        @NotNull
        private File _file;
    }
}
