/*
 * Copyright 2012-2022 the original author or authors.
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
package org.grails.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.grails.io.support.Resource;

/**
 * Bridges Grails and Spring Resource APIs
 *
 * @author Graeme Rocher
 * @since 2.2
 */
public class SpringResource implements Resource {

    org.springframework.core.io.Resource springResource;

    public SpringResource(org.springframework.core.io.Resource springResource) {
        this.springResource = springResource;
    }

    public InputStream getInputStream() throws IOException {
        return this.springResource.getInputStream();
    }

    public boolean exists() {
        return this.springResource.exists();
    }

    public boolean isReadable() {
        return this.springResource.isReadable();
    }

    public URL getURL() throws IOException {
        return this.springResource.getURL();
    }

    public URI getURI() throws IOException {
        return this.springResource.getURI();
    }

    public File getFile() throws IOException {
        return this.springResource.getFile();
    }

    public long contentLength() throws IOException {
        return this.springResource.contentLength();
    }

    public long lastModified() throws IOException {
        return this.springResource.lastModified();
    }

    public String getFilename() {
        return this.springResource.getFilename();
    }

    public String getDescription() {
        return this.springResource.getDescription();
    }

    public Resource createRelative(String relativePath) {
        try {
            return new SpringResource(this.springResource.createRelative(relativePath));
        }
        catch (IOException e) {
            return null;
        }
    }

}
