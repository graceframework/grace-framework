/*
 * Copyright 2024-2025 the original author or authors.
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
package org.grails.cache;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Cache plugin
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@ConfigurationProperties("grails.cache")
public class CacheProperties {

    public static final String DEFAULT_CACHE_MANAGER = "GrailsConcurrentMapCacheManager";

    /**
     * Enable cache or not.
     */
    private Boolean enabled = true;

    /**
     * Whether to clear caches at startup.
     */
    private Boolean clearAtStartup = false;

    /**
     * Cache manager to use.
     */
    private String cacheManager = DEFAULT_CACHE_MANAGER;

    /**
     * Cache configurations.
     */
    private Map<String, CacheConfig> caches = new HashMap<>();

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getClearAtStartup() {
        return this.clearAtStartup;
    }

    public void setClearAtStartup(Boolean clearAtStartup) {
        this.clearAtStartup = clearAtStartup;
    }

    public String getCacheManager() {
        return this.cacheManager;
    }

    public void setCacheManager(String cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Map<String, CacheConfig> getCaches() {
        return this.caches;
    }

    public void setCaches(Map<String, CacheConfig> caches) {
        this.caches = caches;
    }

    public static class CacheConfig {

        /**
         * Max capacity of the cache.
         */
        private Integer maxCapacity;

        public Integer getMaxCapacity() {
            return this.maxCapacity;
        }

        public void setMaxCapacity(Integer maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

    }

}
