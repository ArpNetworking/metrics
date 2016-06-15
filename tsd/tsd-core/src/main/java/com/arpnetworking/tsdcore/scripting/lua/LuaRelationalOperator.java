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
package com.arpnetworking.tsdcore.scripting.lua;

/**
 * Lua's relational operators.
 *
 * @see <a href="http://www.lua.org/pil/3.2.html">http://www.lua.org/pil/3.2.html</a>
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public enum LuaRelationalOperator {

    /**
     * Equal to.
     */
    EQUAL_TO("=="),

    /**
     * Not equal to.
     */
    NOT_EQUAL_TO("~="),

    /**
     * Less than.
     */
    LESS_THAN("<"),

    /**
     * Less than or equal to.
     */
    LESS_THAN_OR_EQUAL_TO("<="),

    /**
     * Greater than.
     */
    GREATER_THAN(">"),

    /**
     * Greater than or equal to.
     */
    GREATER_THAN_OR_EQUAL_TO(">=");

    public String getToken() {
        return _token;
    }

    LuaRelationalOperator(final String token) {
        _token = token;
    }

    private final String _token;
}
