/**
 * Copyright 2015 Groupon.com
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
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

/**
 * <code>Trigger</code> implementation based on a url's last modified date and
 * ETag. Either can trigger a reload; the last modified if is later than the
 * previous value or the ETag if it differs from the previous value. If
 * the url is unavailable it is not considered changed to prevent flickering
 * caused by connectivity or server issues.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class UrlTrigger implements Trigger {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluateAndReset() {
        HttpGet request = null;
        try {
            request = new HttpGet(_url.toURI());
            if (_previousETag != null) {
                request.addHeader(HttpHeaders.IF_NONE_MATCH, _previousETag);
            }
            if (_previousLastModified != null) {
                request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(_previousLastModified));
            }
            final HttpResponse response = CLIENT.execute(request);
            if (response.getStatusLine().getStatusCode() == 304) {
                LOGGER.debug(String.format(
                        "Url unmodified; url=%s status=%d",
                        _url,
                        response.getStatusLine().getStatusCode()));
                return false;
            }
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                LOGGER.warn(String.format(
                        "Url unretrievable; url=%s status=%d",
                        _url,
                        response.getStatusLine().getStatusCode()));
                return false;
            }
            return isLastModifiedChanged(response) || isEtagChanged(response);
        } catch (final URISyntaxException | IOException e) {
            LOGGER.warn(String.format("Unable to evaluate url trigger; url=%s", _url), e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }

        return false;
    }

    private boolean isEtagChanged(final HttpResponse response) {
        final Header newEtagHeader = response.getFirstHeader(HttpHeaders.ETAG);
        if (newEtagHeader != null) {
            final String newETag = newEtagHeader.getValue();
            if (_previousETag == null || !newETag.equals(_previousETag)) {
                LOGGER.debug(String.format(
                        "Url modified; url=%s status=%d newETag=%s previousETag=%s",
                        _url,
                        response.getStatusLine().getStatusCode(),
                        newETag,
                        _previousETag));
                _previousETag = newETag;
                return true;
            }
        }
        return false;
    }

    private boolean isLastModifiedChanged(final HttpResponse response) {
        final Header newLastModifiedHeader = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
        if (newLastModifiedHeader != null) {
            final Date newLastModified;
            try {
                newLastModified = DateUtils.parseDate(newLastModifiedHeader.getValue());
            } catch (final DateParseException e) {
                throw Throwables.propagate(e);
            }
            if (_previousLastModified == null || newLastModified.after(_previousLastModified)) {
                LOGGER.debug(String.format(
                        "Url modified; url=%s status=%d newLastModified=%s previousLastModified=%s",
                        _url,
                        response.getStatusLine().getStatusCode(),
                        newLastModified,
                        _previousLastModified));
                _previousLastModified = newLastModified;
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(UrlTrigger.class)
                .add("id", Integer.toHexString(System.identityHashCode(this)))
                .add("Url", _url)
                .toString();
    }

    private UrlTrigger(final Builder builder) {
        // The url trigger should always return true on the first successful
        // evaluation while on subsequent evaluations true should only be
        // returned if the content at the url was changed since the previous
        // evaluation. To accomplish this a modified time of -1 and a null hash
        // is used.
        _url = builder._url;
        _previousLastModified = null;
        _previousETag = null;
    }

    private final URL _url;

    private Date _previousLastModified;
    private String _previousETag;

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlTrigger.class);
    private static final ClientConnectionManager CONNECTION_MANAGER = new PoolingClientConnectionManager();
    private static final HttpClient CLIENT = new DefaultHttpClient(CONNECTION_MANAGER);
    private static final int CONNECTION_TIMEOUT_IN_MILLISECONDS = 3000;

    static {
        final HttpParams params = CLIENT.getParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT_IN_MILLISECONDS);
    }

    /**
     * Builder for <code>UrlTrigger</code>.
     */
    public static final class Builder extends OvalBuilder<UrlTrigger> {

        /**
         * Public constructor.
         */
        public Builder() {
            super(UrlTrigger.class);
        }

        /**
         * Set the source <code>URL</code>.
         *
         * @param value The source <code>URL</code>.
         * @return This <code>Builder</code> instance.
         */
        public Builder setUrl(final URL value) {
            _url = value;
            return this;
        }

        @NotNull
        private URL _url;
    }
}
