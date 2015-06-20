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
package com.arpnetworking.utility;

import com.arpnetworking.logback.annotations.LogValue;
import com.arpnetworking.steno.LogValueMapFactory;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;
import net.sf.oval.exception.ConstraintsViolatedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * This abstract class for builders that define data constraints using Oval
 * annotations.
 *
 * @param <T> The type of object created by the builder.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public abstract class OvalBuilder<T> implements Builder<T> {

    /**
     * Static factory initializes the source type's builder with state from
     * the source instance. The builder implementation and its default
     * constructor must be accessible by OvalBuilder.
     *
     * @param <T> The type of object created by the builder.
     * @param <B> The type of builder to return.
     * @param source The source of initial state.
     * @return Instance of builder {@code <B>} populated from source.
     */
    @SuppressWarnings("unchecked")
    public static <T, B extends Builder<? super T>> B clone(final T source) {
        B builder = null;
        try {
            final Class<B> builderClass = (Class<B>) Class.forName(
                    source.getClass().getName() + "$Builder",
                    true, // initialize
                    source.getClass().getClassLoader());
            final Constructor<B> builderConstructor = builderClass.getDeclaredConstructor();
            if (!builderConstructor.isAccessible()) {
                builderConstructor.setAccessible(true);
            }
            builder = builderConstructor.newInstance();
        } catch (final InvocationTargetException | NoSuchMethodException | InstantiationException
                | IllegalAccessException | ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
        return clone(source, builder);
    }

    /**
     * Static factory initializes the specified builder with state from the
     * source instance.
     *
     * @param <T> The type of object created by the builder.
     * @param <B> The type of builder to return.
     * @param source The source of initial state.
     * @param target The target builder instance.
     * @return Target populated from source.
     */
    public static <T, B extends Builder<? super T>> B clone(final T source, final B target) {
        for (final Method targetMethod : target.getClass().getMethods()) {
            if (isSetterMethod(targetMethod)) {
                final Optional<Method> getterMethod = getGetterForSetter(targetMethod, source.getClass());
                if (getterMethod.isPresent()) {
                    try {
                        if (!getterMethod.get().isAccessible()) {
                            getterMethod.get().setAccessible(true);
                        }
                        Object value = getterMethod.get().invoke(source);
                        if (value instanceof Optional) {
                            value = ((Optional) value).orNull();
                        }
                        targetMethod.invoke(target, value);
                    } catch (final IllegalAccessException | InvocationTargetException e) {
                        throw Throwables.propagate(e);
                    }
                }
            }
        }
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T build() {
        final List<ConstraintViolation> violations = VALIDATOR.validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintsViolatedException(violations);
        }
        return construct();
    }

    /**
     * Generate a Steno log compatible representation.
     *
     * @return Steno log compatible representation.
     */
    @LogValue
    public Object toLogValue() {
        return LogValueMapFactory.of(
                "id", Integer.toHexString(System.identityHashCode(this)),
                "class", this.getClass(),
                "TargetClass", _targetClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLogValue().toString();
    }

    /**
     * Protected method to construct the target class reflectively from the
     * specified type by passing its constructor an instance of this builder.
     *
     * @return Instance of target class created from this builder.
     */
    protected T construct() {
        try {
            final Constructor<? extends T> constructor = _targetClass.getDeclaredConstructor(this.getClass());
            constructor.setAccessible(true);
            return constructor.newInstance(this);

        } catch (final NoSuchMethodException
                | SecurityException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException e) {
            throw new UnsupportedOperationException(
                    String.format(UNABLE_TO_CONSTRUCT_TARGET_CLASS, _targetClass),
                    e);
        } catch (final InvocationTargetException e) {
            // If the constructor of the class threw an exception, unwrap it and
            // rethrow it. If the constructor throws anything other than a
            // RuntimeException we wrap it.
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new UnsupportedOperationException(
                    String.format(UNABLE_TO_CONSTRUCT_TARGET_CLASS, _targetClass),
                    cause);
        }
    }

    /**
     * Protected constructor for subclasses.
     *
     * @param targetClass The concrete type to be created by this builder.
     */
    protected OvalBuilder(final Class<? extends T> targetClass) {
        _targetClass = targetClass;
    }

    private static Optional<Method> getGetterForSetter(final Method setter, final Class<?> clazz) {
        // Attempt to find "getFoo" and then "isFoo"; the parameter type is not
        // definitively indicative of get vs is because an Optional wrapped
        // boolean can be exposed as get instead of is.
        try {
            final String getterName = GETTER_GET_METHOD_PREFIX + setter.getName().substring(SETTER_METHOD_PREFIX.length());
            return Optional.of(clazz.getDeclaredMethod(getterName));
        } catch (final NoSuchMethodException e1) {
            try {
                final String getterName = GETTER_IS_METHOD_PREFIX + setter.getName().substring(SETTER_METHOD_PREFIX.length());
                return Optional.of(clazz.getDeclaredMethod(getterName));
            } catch (final NoSuchMethodException e2) {
                return Optional.absent();
            }
        }
    }

    private static boolean isGetterMethod(final Method method) {
        return (method.getName().startsWith(GETTER_GET_METHOD_PREFIX)
                || method.getName().startsWith(GETTER_IS_METHOD_PREFIX))
                &&
                !Void.class.equals(method.getReturnType())
                &&
                !method.isVarArgs()
                &&
                method.getParameterTypes().length == 0;
    }

    private static boolean isSetterMethod(final Method method) {
        return method.getName().startsWith(SETTER_METHOD_PREFIX)
                &&
                Builder.class.isAssignableFrom(method.getReturnType())
                &&
                !method.isVarArgs()
                &&
                method.getParameterTypes().length == 1;
    }

    private final Class<? extends T> _targetClass;

    private static final Validator VALIDATOR = new Validator();
    private static final String GETTER_IS_METHOD_PREFIX = "is";
    private static final String GETTER_GET_METHOD_PREFIX = "get";
    private static final String SETTER_METHOD_PREFIX = "set";
    private static final String UNABLE_TO_CONSTRUCT_TARGET_CLASS = "Unable to construct target class; target_class=%s";
}
