/*
 * Copyright 2016-2023 the original author or authors.
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
package org.grails.core.support.internal.tools;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.springframework.core.io.FileSystemResource;

import grails.io.IOUtils;
import grails.util.BuildSettings;

/**
 * A classloader that only finds resources and classes that are in the same jar as the given class
 *
 * For internal use only
 *
 * @author Graeme Rocher
 * @since 3.1.13
 */
public class ClassRelativeClassLoader extends URLClassLoader {

    public ClassRelativeClassLoader(Class targetClass) {
        super(createClassLoaderUrls(targetClass), ClassLoader.getSystemClassLoader());
    }

    private static URL[] createClassLoaderUrls(Class targetClass) {
        URL root = IOUtils.findRootResource(targetClass);
        if (BuildSettings.RESOURCES_DIR != null && BuildSettings.RESOURCES_DIR.exists()) {
            try {
                return new URL[] { root, new FileSystemResource(BuildSettings.RESOURCES_DIR.getCanonicalFile()).getURL() };
            }
            catch (IOException e) {
                return new URL[] { root };
            }
        }
        else {
            return new URL[] { root };
        }
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if ("".equals(name)) {
            URL[] urls = getURLs();
            int l = urls.length;
            return new Enumeration<>() {
                int i = 0;

                @Override
                public boolean hasMoreElements() {
                    return this.i < l;
                }

                @Override
                public URL nextElement() {
                    return urls[this.i++];
                }
            };
        }
        else {
            return findResources(name);
        }
    }

}
