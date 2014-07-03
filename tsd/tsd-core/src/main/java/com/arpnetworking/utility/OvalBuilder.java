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

import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;
import net.sf.oval.exception.ConstraintsViolatedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

        } catch (final NoSuchMethodException |
                SecurityException |
                InstantiationException |
                IllegalAccessException |
                IllegalArgumentException e) {
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

    private final Class<? extends T> _targetClass;

    private static final Validator VALIDATOR = new Validator();
    private static final String UNABLE_TO_CONSTRUCT_TARGET_CLASS = "Unable to construct target class; target_class=%s";
}
