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
package com.arpnetworking.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Immutable decorator for <code>ObjectMapper</code>.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class ImmutableObjectMapper {

    /**
     * Decorate an <code>ObjectMapper</code> instance so that it is immutable.
     *
     * <i>Warning:</i> Anyone with a reference to the original
     * <code>ObjectMapper</code> instance may modify it and thus appear to
     * modify the decorated instance as well.
     *
     * @param objectMapper The <code>ObjectMapper</code> instance to decorate.
     * @return Immutable <code>ObjectMapper</code> instance.
     */
    public static ObjectMapper of(final ObjectMapper objectMapper) {
        final Callback[] callbacks = new Callback[2];
        callbacks[0] = new SafeMethodCallback(objectMapper);
        callbacks[1] = UNSAFE_METHOD_CALLBACK;

        final Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(ObjectMapper.class);
        enhancer.setCallbackFilter(CALLBACK_FILTER);
        enhancer.setCallbacks(callbacks);

        return (ObjectMapper) enhancer.create();
    }

    private ImmutableObjectMapper() {}

    private static final CallbackFilter CALLBACK_FILTER = new ImmutableObjectMapperCallbackFilter();
    private static final Callback UNSAFE_METHOD_CALLBACK = new UnsafeMethodCallback();

    private static final class ImmutableObjectMapperCallbackFilter implements CallbackFilter {

        public ImmutableObjectMapperCallbackFilter() {}

        private static final Set<String> SAFE_METHOD_NAMES = Sets.newHashSet();

        // CHECKSTYLE.OFF: ExecutableStatementCount - Data definition
        static {
            // Read Methods
            SAFE_METHOD_NAMES.add("reader");
            SAFE_METHOD_NAMES.add("readerForUpdating");
            SAFE_METHOD_NAMES.add("readTree");
            SAFE_METHOD_NAMES.add("readValue");
            SAFE_METHOD_NAMES.add("readValues");
            SAFE_METHOD_NAMES.add("readerWithView");
            // Write Methods
            SAFE_METHOD_NAMES.add("writer");
            SAFE_METHOD_NAMES.add("writerFor");
            SAFE_METHOD_NAMES.add("writeTree");
            SAFE_METHOD_NAMES.add("writeValue");
            SAFE_METHOD_NAMES.add("writeValueAsBytes");
            SAFE_METHOD_NAMES.add("writeValueAsString");
            SAFE_METHOD_NAMES.add("writerWithDefaultPrettyPrinter");
            SAFE_METHOD_NAMES.add("writerWithType");
            SAFE_METHOD_NAMES.add("writerWithView");
            // Inspection & Miscellaneous
            SAFE_METHOD_NAMES.add("acceptJsonFormatVisitor");
            SAFE_METHOD_NAMES.add("canDeserialize");
            SAFE_METHOD_NAMES.add("canSerialize");
            SAFE_METHOD_NAMES.add("constructType");
            SAFE_METHOD_NAMES.add("convertValue");
            SAFE_METHOD_NAMES.add("copy");
            SAFE_METHOD_NAMES.add("createArrayNode");
            SAFE_METHOD_NAMES.add("createObjectNode");
            SAFE_METHOD_NAMES.add("defaultClassIntrospector");
            SAFE_METHOD_NAMES.add("findMixInClassFor");
            SAFE_METHOD_NAMES.add("findModules");
            SAFE_METHOD_NAMES.add("getDateFormat");
            SAFE_METHOD_NAMES.add("getDeserializationConfig");
            SAFE_METHOD_NAMES.add("getDeserializationContext");
            SAFE_METHOD_NAMES.add("generateJsonSchema");
            SAFE_METHOD_NAMES.add("getNodeFactory");
            SAFE_METHOD_NAMES.add("getPropertyNamingStrategy");
            SAFE_METHOD_NAMES.add("getSerializationConfig");
            SAFE_METHOD_NAMES.add("getSerializerFactory");
            SAFE_METHOD_NAMES.add("getTypeFactory");
            SAFE_METHOD_NAMES.add("isEnabled");
            SAFE_METHOD_NAMES.add("mixInCount");
            SAFE_METHOD_NAMES.add("treeAsTokens");
            SAFE_METHOD_NAMES.add("treeToValue");
            SAFE_METHOD_NAMES.add("valueToTree");
            SAFE_METHOD_NAMES.add("version");

            // The following are unsafe in addition to obvious mutators:
            // * getFactory
            // * getJsonFactory
            // * getSerializerProvider
            // * getSubtypeResolver
            // * getVisibilityChecker
        }
        // CHECKSTYLE.ON: ExecutableStatementCount

        @Override
        public int accept(final Method method) {
            // All non-public methods are considered safe
            if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                return 0;
            }
            // All methods declared on root Object class are considered safe
            if (method.getDeclaringClass().equals(Object.class)) {
                return 0;
            }
            // All methods explicitly listed are considered safe
            return SAFE_METHOD_NAMES.contains(method.getName()) ? 0 : 1;
        }
    }

    private static final class SafeMethodCallback implements MethodInterceptor {

        public SafeMethodCallback(final ObjectMapper wrapper) {
            _wrapper = wrapper;
        }

        // CHECKSTYLE.OFF: IllegalThrows - Required by external interface
        @Override
        public Object intercept(
                final Object obj,
                final Method method,
                final Object[] args,
                final MethodProxy proxy) throws Throwable {
            // CHECKSTYLE.ON: IllegalThrows
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(_wrapper, args);
        }

        private final ObjectMapper _wrapper;
    }

    private static final class UnsafeMethodCallback implements MethodInterceptor {

        // CHECKSTYLE.OFF: IllegalThrows - Required by external interface
        @Override
        public Object intercept(
                final Object obj,
                final Method method,
                final Object[] args,
                final MethodProxy proxy) throws Throwable {
            // CHECKSTYLE.ON: IllegalThrows
            throw new UnsupportedOperationException(String.format(
                    "Cannot mutate immutable ObjectMapper; method=%s",
                    method.getName()));
        }
    }
}
