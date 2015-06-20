/*
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

class Quantity {
    value: number;
    numeratorUnits: string[];
    denominatorUnits: string[];

    constructor(value: number, numeratorUnits: string[], denominatorUnits: string[]) {
        this.value = value;
        this.numeratorUnits = numeratorUnits;
        this.denominatorUnits = denominatorUnits;
    }

    toString(): string {
        var asString = this.value.toString();
        if (this.numeratorUnits.length > 0 || this.denominatorUnits.length > 0) {
            var numeratorParenthesis = this.numeratorUnits.length > 1 && this.denominatorUnits.length > 0;
            var denominatorParenthesis = this.denominatorUnits.length > 1;
            asString.concat(" ");
            if (this.numeratorUnits.length > 0) {
                if (numeratorParenthesis) {
                    asString.concat("(");
                }
                for (var i = 0; i < this.numeratorUnits.length; i++) {
                    asString.concat(this.numeratorUnits[i]);
                    if (i < this.numeratorUnits.length - 1) {
                        asString.concat("*");
                    }
                }
                if (numeratorParenthesis) {
                    asString.concat(")");
                }
            } else {
                asString.concat("1");
            }
            if (this.denominatorUnits.length > 0) {
                asString.concat("/");
                if (denominatorParenthesis) {
                    asString.concat("(");
                }
                for (var i = 0; i < this.denominatorUnits.length; i++) {
                    asString.concat(this.denominatorUnits[i]);
                    if (i < this.denominatorUnits.length - 1) {
                        asString.concat("*");
                    }
                }
                if (denominatorParenthesis) {
                    asString.concat(")");
                }
            }
        }
        return asString;
    }
}

export = Quantity;
