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
package org.grails.web.pages;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import groovy.lang.GroovyObject;
import org.springframework.util.Assert;

/**
 * Provides services for resolving URIs.
 * Caches lookups in an internal ConcurrentMap cache.
 *
 * @author Lari Hotari , Sagire Software Oy
 */
public class DefaultGroovyPagesUriService extends GroovyPagesUriSupport {

    private final ConcurrentMap<TupleStringKey, String> templateURICache = new ConcurrentHashMap<>();

    private final ConcurrentMap<TupleStringKey, String> deployedViewURICache = new ConcurrentHashMap<>();

    private final ConcurrentMap<ControllerObjectKey, String> controllerNameCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<TupleStringKey, String> noSuffixViewURICache = new ConcurrentHashMap<>();

    /* (non-Javadoc)
     * @see grails.web.pages.GroovyPagesUriService#getTemplateURI(java.lang.String, java.lang.String)
     */
    @Override
    public String getTemplateURI(String controllerName, String templateName) {
        TupleStringKey key = new TupleStringKey(controllerName, templateName);
        String uri = this.templateURICache.get(key);
        if (uri == null) {
            uri = super.getTemplateURI(controllerName, templateName);
            String prevuri = this.templateURICache.putIfAbsent(key, uri);
            if (prevuri != null) {
                return prevuri;
            }
        }
        return uri;
    }

    @Override
    public String getDeployedViewURI(String controllerName, String viewName) {
        TupleStringKey key = new TupleStringKey(controllerName, viewName);
        String uri = this.deployedViewURICache.get(key);
        if (uri == null) {
            uri = super.getDeployedViewURI(controllerName, viewName);
            String prevuri = this.deployedViewURICache.putIfAbsent(key, uri);
            if (prevuri != null) {
                return prevuri;
            }
        }
        return uri;
    }

    @Override
    public String getLogicalControllerName(GroovyObject controller) {
        ControllerObjectKey key = new ControllerObjectKey(controller);
        String name = this.controllerNameCache.get(key);
        if (name == null) {
            name = super.getLogicalControllerName(controller);
            String prevname = name != null ? this.controllerNameCache.putIfAbsent(key, name) : null;
            if (prevname != null) {
                return prevname;
            }
        }
        return name;
    }

    @Override
    public String getNoSuffixViewURI(GroovyObject controller, String viewName) {
        Assert.notNull(controller, "Argument [controller] cannot be null");
        return getNoSuffixViewURI(getLogicalControllerName(controller), viewName);
    }

    @Override
    public String getNoSuffixViewURI(String controllerName, String viewName) {
        TupleStringKey key = new TupleStringKey(controllerName, viewName);
        String uri = this.noSuffixViewURICache.get(key);
        if (uri == null) {
            uri = super.getNoSuffixViewURI(controllerName, viewName);
            String prevuri = this.noSuffixViewURICache.putIfAbsent(key, uri);
            if (prevuri != null) {
                return prevuri;
            }
        }
        return uri;
    }

    @Override
    public String getTemplateURI(GroovyObject controller, String templateName) {
        return getTemplateURI(getLogicalControllerName(controller), templateName);
    }

    @Override
    public void clear() {
        this.templateURICache.clear();
        this.deployedViewURICache.clear();
        this.controllerNameCache.clear();
        this.noSuffixViewURICache.clear();
    }

    private static class TupleStringKey {

        String keyPart1;

        String keyPart2;

        TupleStringKey(String keyPart1, String keyPart2) {
            this.keyPart1 = keyPart1;
            this.keyPart2 = keyPart2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TupleStringKey that = (TupleStringKey) o;

            if (!Objects.equals(this.keyPart1, that.keyPart1)) {
                return false;
            }
            return Objects.equals(this.keyPart2, that.keyPart2);
        }

        @Override
        public int hashCode() {
            int result = this.keyPart1 != null ? this.keyPart1.hashCode() : 0;
            result = 31 * result + (this.keyPart2 != null ? this.keyPart2.hashCode() : 0);
            return result;
        }

    }

    private static class ControllerObjectKey {

        private final long controllerHashCode;

        private final String controllerClassName;

        ControllerObjectKey(GroovyObject controller) {
            this.controllerHashCode = controller.getClass().hashCode();
            this.controllerClassName = controller.getClass().getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ControllerObjectKey that = (ControllerObjectKey) o;

            if (this.controllerHashCode != that.controllerHashCode) {
                return false;
            }
            return this.controllerClassName.equals(that.controllerClassName);
        }

        @Override
        public int hashCode() {
            int result = (int) (this.controllerHashCode ^ (this.controllerHashCode >>> 32));
            result = 31 * result + this.controllerClassName.hashCode();
            return result;
        }

    }

}
