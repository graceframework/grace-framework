/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.web.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import grails.web.mapping.UrlCreator;
import grails.web.mapping.UrlMapping;

/**
 * Implements caching layer for UrlCreator
 *
 * The "weight" of the cache is the estimated number of characters all cache entries will consume in memory.
 * The estimate is not accurate. It's just used as a hard limit for limiting the cache size.
 *
 * You can tune the maximum weight of the cache by setting "grails.urlcreator.cache.maxsize" in Config.groovy.
 * The default value is 160000 .
 *
 * @author Lari Hotari
 * @since 1.3.5
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UrlCreatorCache {

    private final Cache<ReverseMappingKey, CachingUrlCreator> cacheMap;

    private enum CachingUrlCreatorWeigher implements Weigher<ReverseMappingKey, CachingUrlCreator> {
        INSTANCE;

        @Override
        public int weigh(ReverseMappingKey key, CachingUrlCreator value) {
            return value.weight() + 1;
        }
    }

    public UrlCreatorCache(int maxSize) {
        this.cacheMap = Caffeine.newBuilder()
                .maximumWeight(maxSize).weigher(CachingUrlCreatorWeigher.INSTANCE).build();
    }

    public void clear() {
        this.cacheMap.invalidateAll();
    }

    public ReverseMappingKey createKey(String controller, String action, String namespace, String pluginName, String httpMethod, Map params) {
        return new ReverseMappingKey(controller, action, namespace, pluginName, httpMethod, params);
    }

    public UrlCreator lookup(ReverseMappingKey key) {
        return this.cacheMap.getIfPresent(key);
    }

    public UrlCreator putAndDecorate(ReverseMappingKey key, UrlCreator delegate) {
        CachingUrlCreator cachingUrlCreator = new CachingUrlCreator(delegate, key.weight() * 2);
        CachingUrlCreator prevCachingUrlCreator = this.cacheMap.asMap()
                .putIfAbsent(key, cachingUrlCreator);
        if (prevCachingUrlCreator != null) {
            return prevCachingUrlCreator;
        }
        return cachingUrlCreator;
    }

    private class CachingUrlCreator implements UrlCreator {

        private UrlCreator delegate;

        private ConcurrentHashMap<UrlCreatorKey, String> cache = new ConcurrentHashMap<>();

        private final int weight;

        CachingUrlCreator(UrlCreator delegate, int weight) {
            this.delegate = delegate;
            this.weight = weight;
        }

        public int weight() {
            return this.weight;
        }

        public String createRelativeURL(String controller, String action, Map parameterValues,
                String encoding, String fragment) {
            return createRelativeURL(controller, action, null, null, parameterValues, encoding, fragment);
        }

        @Override
        public String createRelativeURL(String controller, String action,
                String pluginName, Map parameterValues, String encoding) {
            return createRelativeURL(controller, action, null, pluginName, parameterValues, encoding);
        }

        public String createRelativeURL(String controller, String action, String namespace, String pluginName, Map parameterValues,
                String encoding, String fragment) {
            UrlCreatorKey key = new UrlCreatorKey(controller, action, namespace, pluginName, null,
                    parameterValues, encoding, fragment, 0);
            String url = this.cache.get(key);
            if (url == null) {
                url = this.delegate.createRelativeURL(controller, action, namespace, pluginName, parameterValues, encoding, fragment);
                this.cache.put(key, url);
            }
            return url;
        }

        public String createRelativeURL(String controller, String action, Map parameterValues, String encoding) {
            return createRelativeURL(controller, action, null, null, parameterValues, encoding);
        }

        public String createRelativeURL(String controller, String action, String namespace, String pluginName, Map parameterValues, String encoding) {
            UrlCreatorKey key = new UrlCreatorKey(controller, action, namespace, pluginName, null,
                    parameterValues, encoding, null, 0);
            String url = this.cache.get(key);
            if (url == null) {
                url = this.delegate.createRelativeURL(controller, action, namespace, pluginName, parameterValues, encoding);
                this.cache.put(key, url);
            }
            return url;
        }

        public String createURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
            return createURL(controller, action, null, null, parameterValues, encoding, fragment);
        }

        public String createURL(String controller, String action, String pluginName, Map parameterValues, String encoding) {
            return createURL(controller, action, null, pluginName, parameterValues, encoding);
        }

        public String createURL(String controller, String action, String namespace,
                String pluginName, Map parameterValues, String encoding, String fragment) {
            UrlCreatorKey key = new UrlCreatorKey(controller, action, namespace, pluginName, null,
                    parameterValues, encoding, fragment, 1);
            String url = this.cache.get(key);
            if (url == null) {
                url = this.delegate.createURL(controller, action, namespace, pluginName, parameterValues, encoding, fragment);
                this.cache.put(key, url);
            }
            return url;
        }

        public String createURL(String controller, String action, Map parameterValues, String encoding) {
            return createURL(controller, action, null, null, parameterValues, encoding);
        }

        public String createURL(String controller, String action, String namespace,
                String pluginName, Map parameterValues, String encoding) {
            UrlCreatorKey key = new UrlCreatorKey(controller, action, namespace, pluginName, null,
                    parameterValues, encoding, null, 1);
            String url = this.cache.get(key);
            if (url == null) {
                url = this.delegate.createURL(controller, action, namespace, pluginName, parameterValues, encoding);
                this.cache.put(key, url);
            }
            return url;
        }

        // don't cache these methods at all

        public String createURL(Map parameterValues, String encoding, String fragment) {
            return this.delegate.createURL(parameterValues, encoding, fragment);
        }

        public String createURL(Map parameterValues, String encoding) {
            return this.delegate.createURL(parameterValues, encoding);
        }

    }

    public static class ReverseMappingKey {

        protected final String controller;

        protected final String action;

        protected final String namespace;

        protected final String pluginName;

        protected final String httpMethod;

        protected final String[] paramKeys;

        protected final String[] paramValues;

        public ReverseMappingKey(String controller, String action, String namespace,
                String pluginName, String httpMethod, Map<Object, Object> params) {

            this.controller = controller;
            this.action = action;
            this.namespace = namespace;
            this.pluginName = pluginName;
            if (httpMethod != null && !UrlMapping.ANY_HTTP_METHOD.equalsIgnoreCase(httpMethod)) {
                this.httpMethod = httpMethod;
            }
            else {
                this.httpMethod = null;
            }

            if (params != null) {
                this.paramKeys = new String[params.size()];
                this.paramValues = new String[params.size()];
                int i = 0;
                for (Map.Entry entry : params.entrySet()) {
                    this.paramKeys[i] = String.valueOf(entry.getKey());
                    String value = null;
                    if (entry.getValue() instanceof CharSequence) {
                        value = String.valueOf(entry.getValue());
                    }
                    else if (entry.getValue() instanceof Collection) {
                        value = DefaultGroovyMethods.join((Iterable) entry.getValue(), ",");
                    }
                    else if (entry.getValue() instanceof Object[]) {
                        value = DefaultGroovyMethods.join((Object[]) entry.getValue(), ",");
                    }
                    else {
                        value = String.valueOf(entry.getValue());
                    }
                    this.paramValues[i] = value;
                    i++;
                }
            }
            else {
                this.paramKeys = new String[0];
                this.paramValues = new String[0];
            }
        }

        public int weight() {
            int weight = 0;
            weight += (this.controller != null) ? this.controller.length() : 0;
            weight += (this.action != null) ? this.action.length() : 0;
            weight += (this.namespace != null) ? this.namespace.length() : 0;
            weight += (this.pluginName != null) ? this.pluginName.length() : 0;
            for (String paramKey : this.paramKeys) {
                weight += (paramKey != null) ? paramKey.length() : 0;
            }
            for (String paramValue : this.paramValues) {
                weight += (paramValue != null) ? paramValue.length() : 0;
            }
            return weight;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.action == null) ? 0 : this.action.hashCode());
            result = prime * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
            result = prime * result + ((this.pluginName == null) ? 0 : this.pluginName.hashCode());
            result = prime * result + ((this.controller == null) ? 0 : this.controller.hashCode());
            result = prime * result + Arrays.hashCode(this.paramKeys);
            result = prime * result + Arrays.hashCode(this.paramValues);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ReverseMappingKey other = (ReverseMappingKey) obj;
            if (this.action == null) {
                if (other.action != null) {
                    return false;
                }
            }
            else if (!this.action.equals(other.action)) {
                return false;
            }
            if (this.controller == null) {
                if (other.controller != null) {
                    return false;
                }
            }
            else if (!this.controller.equals(other.controller)) {
                return false;
            }
            if (this.namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            }
            else if (!this.namespace.equals(other.namespace)) {
                return false;
            }
            if (this.pluginName == null) {
                if (other.pluginName != null) {
                    return false;
                }
            }
            else if (!this.pluginName.equals(other.pluginName)) {
                return false;
            }
            if (this.httpMethod == null) {
                if (other.httpMethod != null) {
                    return false;
                }
            }
            else if (!this.httpMethod.equals(other.httpMethod)) {
                return false;
            }

            if (!Arrays.equals(this.paramKeys, other.paramKeys)) {
                return false;
            }
            if (!Arrays.equals(this.paramValues, other.paramValues)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "UrlCreatorCache.ReverseMappingKey [action=" + this.action + ", controller=" + this.controller +
                    ", namespace=" + this.namespace + ", plugin=" + this.pluginName +
                    ", paramKeys=" + Arrays.toString(this.paramKeys) + ", paramValues=" +
                    Arrays.toString(this.paramValues) + "]";
        }

    }

    private static class UrlCreatorKey extends ReverseMappingKey {

        protected final String encoding;

        protected final String fragment;

        protected final int urlType;

        UrlCreatorKey(String controller, String action, String namespace,
                String pluginName, String httpMethod, Map<Object, Object> params, String encoding,
                String fragment, int urlType) {
            super(controller, action, namespace, pluginName, httpMethod, params);
            this.encoding = (encoding != null) ? encoding.toLowerCase() : null;
            this.fragment = fragment;
            this.urlType = urlType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((this.encoding == null) ? 0 : this.encoding.hashCode());
            result = prime * result + ((this.fragment == null) ? 0 : this.fragment.hashCode());
            result = prime * result + this.urlType;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            UrlCreatorKey other = (UrlCreatorKey) obj;
            if (this.encoding == null) {
                if (other.encoding != null) {
                    return false;
                }
            }
            else if (!this.encoding.equals(other.encoding)) {
                return false;
            }
            if (this.fragment == null) {
                if (other.fragment != null) {
                    return false;
                }
            }
            else if (!this.fragment.equals(other.fragment)) {
                return false;
            }
            if (this.urlType != other.urlType) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "UrlCreatorCache.UrlCreatorKey [encoding=" + this.encoding + ", fragment=" + this.fragment +
                    ", urlType=" + this.urlType + ", action=" + this.action + ", controller=" + this.controller +
                    ", namespace=" + this.namespace + ", plugin=" + this.pluginName +
                    ", paramKeys=" + Arrays.toString(this.paramKeys) + ", paramValues=" + Arrays.toString(this.paramValues) + "]";
        }

    }

}
