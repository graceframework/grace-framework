/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.cache

import groovy.transform.CompileStatic
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * Delegating Spring's {@link CacheManager}.
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
@CompileStatic
class GrailsDelegatingCacheManager implements GrailsCacheManager {

    private static final String[] REMOVE_CACHE_METHODS = ['removeCache', 'destroyCache', 'clearCache']

    private final CacheManager delegate

    GrailsDelegatingCacheManager(CacheManager cacheManager) {
        this.delegate = cacheManager
    }

    @Override
    boolean cacheExists(String name) {
        return this.delegate.getCacheNames().contains(name)
    }

    @Override
    boolean destroyCache(String name) {
        REMOVE_CACHE_METHODS.each { String method ->
            if (!this.delegate.respondsTo(method).isEmpty()) {
                this.delegate.invokeMethod(method, name)
                return true
            }
        }
        return true
    }

    @Override
    Cache getCache(String name) {
        return this.delegate.getCache(name)
    }

    @Override
    Collection<String> getCacheNames() {
        return this.delegate.getCacheNames()
    }

}
