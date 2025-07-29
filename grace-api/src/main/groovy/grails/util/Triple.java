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

public class Triple<A, B, C> {

    final A aValue;

    final B bValue;

    final C cValue;

    public Triple(A aValue, B bValue, C cValue) {
        this.aValue = aValue;
        this.bValue = bValue;
        this.cValue = cValue;
    }

    public A getaValue() {
        return this.aValue;
    }

    public B getbValue() {
        return this.bValue;
    }

    public C getcValue() {
        return this.cValue;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + ((this.aValue == null) ? 0 : this.aValue.hashCode());
        result = 31 * result + ((this.bValue == null) ? 0 : this.bValue.hashCode());
        result = 31 * result + ((this.cValue == null) ? 0 : this.cValue.hashCode());
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
        Triple other = (Triple) obj;
        if (this.aValue == null) {
            if (other.aValue != null) {
                return false;
            }
        }
        else if (!this.aValue.equals(other.aValue)) {
            return false;
        }
        if (this.bValue == null) {
            if (other.bValue != null) {
                return false;
            }
        }
        else if (!this.bValue.equals(other.bValue)) {
            return false;
        }
        if (this.cValue == null) {
            if (other.cValue != null) {
                return false;
            }
        }
        else if (!this.cValue.equals(other.cValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Triple [aValue=" + this.aValue + ", bValue=" + this.bValue + ", cValue=" + this.cValue + "]";
    }

}
