/*
 * Copyright 2025 the original author or authors.
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
package grails.plugin.json.view.api

import groovy.json.JsonException
import groovy.transform.CompileStatic

import grails.plugin.json.util.JsonToken
import org.grails.buffer.FastStringWriter

/**
 * Json Writeable
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
abstract class JsonWritable implements Writable, CharSequence {

    protected boolean inline = false
    protected boolean first = true

    public static final JsonWritable NULL_OUTPUT = new JsonWritable() {

        @Override
        Writer writeTo(Writer out) throws IOException {
            out.write(JsonToken.NULL_VALUE)
            return out
        }

    }

    public static final JsonWritable NOOP_OUTPUT = new JsonWritable() {

        @Override
        Writer writeTo(Writer out) throws IOException {
            return out
        }

    }

    void setInline(boolean inline) {
        this.inline = inline
    }

    void setFirst(boolean first) {
        this.first = first
    }

    @Override
    String toString() {
        FastStringWriter out = new FastStringWriter()
        try {
            writeTo(out)
        } catch (IOException e) {
            throw new JsonException('Error writing JSON writable: ' + e.getMessage(), e)
        }
        return out.toString()
    }

    @Override
    int length() {
        return toString().length()
    }

    @Override
    char charAt(int index) {
        return toString().charAt(index)
    }

    @Override
    CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end)
    }

}
