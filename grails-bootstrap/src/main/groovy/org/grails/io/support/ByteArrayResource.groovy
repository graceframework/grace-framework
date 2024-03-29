/*
 * Copyright 2002-2022 the original author or authors.
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
package org.grails.io.support

import groovy.transform.CompileStatic

/**
 * A Resource impl used represent a Resource as an array of bytes
 */
@CompileStatic
class ByteArrayResource implements Resource {

    byte[] bytes
    String description = 'resource loaded from byte array'

    ByteArrayResource(byte[] bytes) {
        this.bytes = bytes
    }

    ByteArrayResource(byte[] bytes, String desc) {
        this.bytes = bytes
        this.description = desc
    }

    InputStream getInputStream() {
        new ByteArrayInputStream(bytes)
    }

    boolean exists() { true }

    boolean isReadable() { true }

    URL getURL() {
        throw new UnsupportedOperationException('Method getURL not supported')
    }

    URI getURI() {
        throw new UnsupportedOperationException('Method getURI not supported')
    }

    File getFile() {
        throw new UnsupportedOperationException('Method getFile not supported')
    }

    long contentLength() { bytes.length }

    long lastModified() { 0 }

    String getFilename() { description }

    //Fully qualified name to work around Groovy bug
    org.grails.io.support.Resource createRelative(String relativePath) {
        throw new UnsupportedOperationException('Method createRelative not supported')
    }

}
