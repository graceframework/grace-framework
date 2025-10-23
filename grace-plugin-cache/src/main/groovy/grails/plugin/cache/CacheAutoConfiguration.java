/*
 * Copyright 2024 the original author or authors.
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
package grails.plugin.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.grails.plugin.cache.GrailsCacheManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Cache.
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@ConditionalOnProperty(name = "grails.cache.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    private final CacheProperties properties;

    public CacheAutoConfiguration(CacheProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomCacheKeyGenerator customCacheKeyGenerator() {
        return new CustomCacheKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrailsCacheManager grailsCacheManager() {
        String cacheManagerClassName = this.properties.getCacheManager();
        if (cacheManagerClassName.equals(CacheProperties.DEFAULT_CACHE_MANAGER)) {
            GrailsConcurrentLinkedMapCacheManager cacheManager = new GrailsConcurrentLinkedMapCacheManager();
            cacheManager.setConfiguration(this.properties);
            return cacheManager;
        }
        else {
            GrailsConcurrentMapCacheManager cacheManager = new GrailsConcurrentMapCacheManager();
            cacheManager.setConfiguration(this.properties);
            return cacheManager;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public GrailsCacheAdminService grailsCacheAdminService() {
        return new GrailsCacheAdminService();
    }

}
