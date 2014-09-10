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
package com.arpnetworking.utility.test;

import java.lang.reflect.Method;

/**
 * Common test utility helper methods.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public final class TestHelper {

    /**
     * Determine whether a particular method is a setter method.
     *
     * @param method The method to evaluate.
     * @return True if and only if the method is a setter method.
     */
    public static boolean isSetterMethod(final Method method) {
        return method.getName().startsWith(SETTER_METHOD_PREFIX)
                &&
                !method.isVarArgs()
                &&
                method.getParameterTypes().length == 1;
    }

    /**
     * Determine whether a particular method is a getter method.
     *
     * @param method The method to evaluate.
     * @return True if and only if the method is a getter method.
     */
    public static boolean isGetterMethod(final Method method) {
        return method.getName().startsWith(GETTER_METHOD_PREFIX)
                &&
                !Void.class.equals(method.getReturnType())
                &&
                !method.isVarArgs()
                &&
                method.getParameterTypes().length == 0;
    }

    /**
     * Return the corresponding setter method name for a getter method.
     * 
     * @param getterMethod The getter method.
     * @return The name of the corresponding setter method.
     */
    public static String setterMethodNameForGetter(final Method getterMethod) {
        return SETTER_METHOD_PREFIX + getterMethod.getName().substring(GETTER_METHOD_PREFIX.length());
    }

    /**
     * Return the corresponding getter method name for a setter method.
     * 
     * @param setterMethod The setter method.
     * @return The name of the corresponding getter method.
     */
    public static String getterMethodNameForSetter(final Method setterMethod) {
        return GETTER_METHOD_PREFIX + setterMethod.getName().substring(SETTER_METHOD_PREFIX.length());
    }

    private TestHelper() {}

    private static final String GETTER_METHOD_PREFIX = "get";
    private static final String SETTER_METHOD_PREFIX = "set";
}
