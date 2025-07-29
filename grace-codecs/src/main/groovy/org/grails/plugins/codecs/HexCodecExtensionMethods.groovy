/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.plugins.codecs

import java.nio.charset.StandardCharsets

import org.codehaus.groovy.runtime.NullObject

class HexCodecExtensionMethods {

    static final String HEXDIGITS = '0123456789abcdef'

    // Expects an array/list of numbers
    static Object encodeAsHex(Object theTarget) {
        if (theTarget == null || theTarget instanceof NullObject) {
            return null
        }

        StringBuilder result = new StringBuilder()
        if (theTarget instanceof String) {
            theTarget = theTarget.getBytes(StandardCharsets.UTF_8)
        }
        theTarget.each {
            result << HEXDIGITS[(it & 0xF0) >> 4]
            result << HEXDIGITS[it & 0x0F]
        }
        result.toString()
    }

    static Object decodeHex(Object theTarget) {
        if (!theTarget) {
            return null
        }

        List output = []

        String str = theTarget.toString().toLowerCase()
        if (str.size() % 2) {
            throw new UnsupportedOperationException('Decode of hex strings requires strings of even length')
        }

        int currentByte
        str.eachWithIndex { String val, int idx ->
            if (idx % 2) {
                output << (currentByte | HEXDIGITS.indexOf(val))
                currentByte = 0
            }
            else {
                currentByte = HEXDIGITS.indexOf(val) << 4
            }
        }

        byte[] result = new byte[output.size()]
        output.eachWithIndex { v, int i -> result[i] = v }
        result
    }

}
