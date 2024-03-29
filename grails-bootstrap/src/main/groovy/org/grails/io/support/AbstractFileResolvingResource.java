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
package org.grails.io.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * Abstract base class for resources which resolve URLs into File references,
 * such as <code>org.springframework.core.io.UrlResource</code> or <code>org.springframework.core.io.ClassPathResource</code>.
 *
 * <p>Detects the "file" protocol as well as the JBoss "vfs" protocol in URLs,
 * resolving file system references accordingly.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractFileResolvingResource implements Resource {

    /**
     * This implementation returns a File reference for the underlying class path
     * resource, provided that it refers to a file in the file system.
     */
    public File getFile() throws IOException {
        URL url = getURL();
        return GrailsResourceUtils.getFile(url, getDescription());
    }

    /**
     * This implementation determines the underlying File
     * (or jar file, in case of a resource in a jar/zip).
     */
    protected File getFileForLastModifiedCheck() throws IOException {
        URL url = getURL();
        if (GrailsResourceUtils.isJarURL(url)) {
            URL actualUrl = GrailsResourceUtils.extractJarFileURL(url);
            return GrailsResourceUtils.getFile(actualUrl, "Jar URL");
        }
        return getFile();
    }

    /**
     * This implementation returns a File reference for the underlying class path
     * resource, provided that it refers to a file in the file system.
     */
    protected File getFile(URI uri) throws IOException {
        return GrailsResourceUtils.getFile(uri, getDescription());
    }

    /**
     * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the
     * given connection, preferring <code>false</code> but leaving the
     * flag at <code>true</code> for JNLP based resources.
     * @param con the URLConnection to set the flag on
     */
    private static void useCachesIfNecessary(URLConnection con) {
        con.setUseCaches(con.getClass().getName().startsWith("JNLP"));
    }

    public boolean exists() {
        try {
            URL url = getURL();
            if (GrailsResourceUtils.isFileURL(url)) {
                // Proceed with file system resolution...
                return getFile().exists();
            }

            // Try a URL connection content-length header...
            URLConnection con = url.openConnection();
            useCachesIfNecessary(con);
            HttpURLConnection httpCon =
                    (con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
            if (httpCon != null) {
                httpCon.setRequestMethod("HEAD");
                int code = httpCon.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    return true;
                }
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                }
            }
            if (con.getContentLength() >= 0) {
                return true;
            }
            if (httpCon != null) {
                // no HTTP OK status, and no content-length header: give up
                httpCon.disconnect();
                return false;
            }

            // Fall back to stream existence: can we open the stream?
            InputStream is = getInputStream();
            is.close();
            return true;
        }
        catch (IOException ex) {
            return false;
        }
    }

    public boolean isReadable() {
        try {
            URL url = getURL();
            if (GrailsResourceUtils.isFileURL(url)) {
                // Proceed with file system resolution...
                File file = getFile();
                return (file.canRead() && !file.isDirectory());
            }
            return true;
        }
        catch (IOException ex) {
            return false;
        }
    }

    public long contentLength() throws IOException {
        URL url = getURL();
        if (GrailsResourceUtils.isFileURL(url)) {
            // Proceed with file system resolution...
            return getFile().length();
        }
        // Try a URL connection content-length header...
        URLConnection con = url.openConnection();
        useCachesIfNecessary(con);
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).setRequestMethod("HEAD");
        }
        return con.getContentLength();
    }

    public long lastModified() throws IOException {
        URL url = getURL();
        if (GrailsResourceUtils.isFileURL(url) || GrailsResourceUtils.isJarURL(url)) {
            // Proceed with file system resolution...
            return getFile().lastModified();
        }
        // Try a URL connection last-modified header...
        URLConnection con = url.openConnection();
        useCachesIfNecessary(con);
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).setRequestMethod("HEAD");
        }
        return con.getLastModified();
    }

}
