/*
 * Copyright 2011-2022 the original author or authors.
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
package org.grails.cli.interactive.completers

import java.util.regex.Pattern

import jline.console.completer.Completer

/**
 * JLine Completor that accepts a string if it matches a given regular
 * expression pattern.
 *
 * @author Peter Ledbrook
 * @since 2.0
 */
class RegexCompletor implements Completer {

    Pattern pattern

    RegexCompletor(String pattern) {
        this(Pattern.compile(pattern))
    }

    RegexCompletor(Pattern pattern) {
        this.pattern = pattern
    }

    /**
     * <p>Check whether the whole buffer matches the configured pattern.
     * If it does, the buffer is added to the <tt>candidates</tt> list
     * (which indicates acceptance of the buffer string) and returns 0,
     * i.e. the start of the buffer. This mimics the behaviour of SimpleCompletor.
     * </p>
     * <p>If the buffer doesn't match the configured pattern, this returns
     * -1 and the <tt>candidates</tt> list is left empty.</p>
     */
    int complete(String buffer, int cursor, List candidates) {
        if (buffer ==~ pattern) {
            candidates << buffer
            return 0
        }

        -1
    }

}
