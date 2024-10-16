/*
 * Copyright 2014-2022 the original author or authors.
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
package org.grails.charsequences;

/**
 * Wraps a part of a String and implements the CharSequence interface
 *
 *
 * @author Lari Hotari
 * @since 2.3.10
 *
 */
class SubCharSequence implements CharSequence, CharArrayAccessible {

    private final CharSequence str;

    private final int count;

    private final int start;

    SubCharSequence(CharSequence str, int start, int count) {
        if (start + count > str.length()) {
            throw new StringIndexOutOfBoundsException(start);
        }
        this.str = str;
        this.start = start;
        this.count = count;
    }

    public char charAt(int index) {
        if ((index < 0) || (index + this.start >= this.str.length())) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return this.str.charAt(index + this.start);
    }

    public int length() {
        return this.count;
    }

    public CharSequence subSequence(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > this.count) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException(end - start);
        }
        if (start == 0 && end == this.count) {
            return this;
        }
        return new SubCharSequence(this.str, this.start + start, end - start);
    }

    @Override
    public String toString() {
        return this.str.subSequence(this.start, this.start + this.count).toString();
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if ((srcEnd < 0) || (srcEnd > this.start + this.count)) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
        }
        CharSequences.getChars(this.str, this.start + srcBegin, this.start + srcEnd, dst, dstBegin);
    }

}
