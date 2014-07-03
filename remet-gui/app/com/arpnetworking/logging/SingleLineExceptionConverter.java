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
package com.arpnetworking.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

import com.google.common.base.Joiner;

/**
 * <code>Throwable</code> renderer for the LogBack framework which renders a 
 * given instance of <code>Throwable</code> in a single line. This renderer 
 * includes the message, stack trace and cause.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class SingleLineExceptionConverter extends ThrowableHandlingConverter {

    /**
     * {@inheritDoc}
     */
    @Override
    public String convert(final ILoggingEvent event) {
        final IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy instanceof ThrowableProxy) {
            final ThrowableProxy throwableProxy = (ThrowableProxy) proxy;
            return throwableToString(throwableProxy.getThrowable());
        }
        return "";
    }

    private static String throwableToString(final Throwable throwable) {
        if (throwable == null) {
            return "null";
        }
        return new StringBuilder(1024)
                .append("class=\"").append(throwable.getClass().getName())
                .append("\" message=\"").append(throwable.getMessage())
                .append("\" stack_trace=\"").append(Joiner.on(",").join(throwable.getStackTrace()))
                .append("\" caused by ").append(throwableToString(throwable.getCause()))
                .toString();
    }

    /**
     * Public constructor. Required for instantiation by LogBack.
     */
    public SingleLineExceptionConverter() {}
}
