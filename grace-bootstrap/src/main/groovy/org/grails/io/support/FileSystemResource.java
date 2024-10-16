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
package org.grails.io.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

/**
 * Based on Spring FileSystemResource implementation.
 *
 * @author Juergen Hoeller
 * @author Graeme Rocher
 *
 * @since 28.12.2003
 * @see java.io.File
 */
public class FileSystemResource implements Resource {

    private final File file;

    private final String path;

    /**
     * Create a new FileSystemResource from a File handle.
     *
     * @param file a File handle
     */
    public FileSystemResource(File file) {
        assertNotNull(file, "File must not be null");
        this.file = file;
        this.path = GrailsResourceUtils.cleanPath(file.getPath());
    }

    /**
     * Create a new FileSystemResource from a file path.
     *
     * @param path a file path
     */
    public FileSystemResource(String path) {
        assertNotNull(path, "Path must not be null");
        this.file = new File(path);
        this.path = GrailsResourceUtils.cleanPath(path);
    }

    /**
     * Return the file path for this resource.
     */
    public final String getPath() {
        return this.path;
    }

    /**
     * This implementation returns whether the underlying file exists.
     * @see java.io.File#exists()
     */
    public boolean exists() {
        return this.file.exists();
    }

    /**
     * This implementation checks whether the underlying file is marked as readable
     * (and corresponds to an actual file with content, not to a directory).
     * @see java.io.File#canRead()
     * @see java.io.File#isDirectory()
     */
    public boolean isReadable() {
        return this.file.canRead() && !this.file.isDirectory();
    }

    /**
     * This implementation opens a FileInputStream for the underlying file.
     * @see java.io.FileInputStream
     */
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(this.file.toPath());
    }

    /**
     * This implementation returns a URL for the underlying file.
     * @see java.io.File#toURI()
     */
    public URL getURL() throws IOException {
        return this.file.toURI().toURL();
    }

    /**
     * This implementation returns a URI for the underlying file.
     * @see java.io.File#toURI()
     */
    public URI getURI() throws IOException {
        return this.file.toURI();
    }

    /**
     * This implementation returns the underlying File reference.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * This implementation returns the underlying File's length.
     */
    public long contentLength() throws IOException {
        return this.file.length();
    }

    public long lastModified() throws IOException {
        return this.file.lastModified();
    }

    /**
     * This implementation returns the name of the file.
     * @see java.io.File#getName()
     */
    public String getFilename() {
        return this.file.getName();
    }

    /**
     * This implementation returns a description that includes the absolute
     * path of the file.
     * @see java.io.File#getAbsolutePath()
     */
    public String getDescription() {
        return "file [" + this.file.getAbsolutePath() + "]";
    }

    /**
     * This implementation creates a FileSystemResource, applying the given path
     * relative to the path of the underlying file of this resource descriptor.
     */
    public Resource createRelative(String relativePath) {
        String pathToUse = GrailsResourceUtils.applyRelativePath(this.path, relativePath);
        return new FileSystemResource(pathToUse);
    }

    // implementation of WritableResource

    /**
     * This implementation checks whether the underlying file is marked as writable
     * (and corresponds to an actual file with content, not to a directory).
     * @see java.io.File#canWrite()
     * @see java.io.File#isDirectory()
     */
    public boolean isWritable() {
        return this.file.canWrite() && !this.file.isDirectory();
    }

    /**
     * This implementation opens a FileOutputStream for the underlying file.
     * @see java.io.FileOutputStream
     */
    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(this.file.toPath());
    }

    /**
     * This implementation compares the underlying File references.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj == this ||
                (obj instanceof FileSystemResource && this.path.equals(((FileSystemResource) obj).path)));
    }

    /**
     * This implementation returns the hash code of the underlying File reference.
     */
    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    protected void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public String toString() {
        return this.file.toString();
    }

}
