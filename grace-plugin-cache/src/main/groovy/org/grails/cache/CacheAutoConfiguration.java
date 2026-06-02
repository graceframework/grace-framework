/*
 * Copyright 2024-2026 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import grails.cache.CustomCacheKeyGenerator;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Cache.
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
@AutoConfiguration(after = org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class)
@ConditionalOnProperty(name = "grails.cache.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    private final CacheProperties cacheProperties;

    public CacheAutoConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomCacheKeyGenerator customCacheKeyGenerator() {
        return new CustomCacheKeyGenerator();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "grailsCacheManager")
    public GrailsCacheManager grailsCacheManager(@Qualifier("cacheManager") ObjectProvider<CacheManager> cacheManagerObjectProvider) {
        CacheManager cacheManager = cacheManagerObjectProvider.getIfAvailable(() -> {
            GrailsConcurrentMapCacheManager concurrentMapCacheManager = new GrailsConcurrentMapCacheManager();
            concurrentMapCacheManager.setConfiguration(this.cacheProperties);
            return concurrentMapCacheManager;
        });
        return new GrailsDelegatingCacheManager(cacheManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public GrailsCacheAdminService grailsCacheAdminService(@Qualifier("grailsCacheManager") GrailsCacheManager grailsCacheManager,
            CustomCacheKeyGenerator customCacheKeyGenerator) {
        GrailsCacheAdminService grailsCacheAdminService = new GrailsCacheAdminService();
        grailsCacheAdminService.setGrailsCacheManager(grailsCacheManager);
        grailsCacheAdminService.setCustomCacheKeyGenerator(customCacheKeyGenerator);
        return grailsCacheAdminService;
    }

}
