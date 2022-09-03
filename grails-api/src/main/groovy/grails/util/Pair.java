/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util;

public class Pair<A, B> {

    final A aValue;

    final B bValue;

    public Pair(A aValue, B bValue) {
        this.aValue = aValue;
        this.bValue = bValue;
    }

    public A getaValue() {
        return this.aValue;
    }

    public B getbValue() {
        return this.bValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.aValue == null) ? 0 : this.aValue.hashCode());
        result = prime * result + ((this.bValue == null) ? 0 : this.bValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Pair other = (Pair) obj;
        if (this.aValue == null) {
            if (other.aValue != null) {
                return false;
            }
        }
        else if (!this.aValue.equals(other.aValue)) {
            return false;
        }
        if (this.bValue == null) {
            return other.bValue == null;
        }
        else {
            return this.bValue.equals(other.bValue);
        }
    }

    @Override
    public String toString() {
        return "TupleKey [aValue=" + this.aValue + ", bValue=" + this.bValue + "]";
    }

}
