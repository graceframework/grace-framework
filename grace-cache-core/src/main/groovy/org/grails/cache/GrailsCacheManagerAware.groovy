/*
 * Copyright 2017-2026 the original author or authors.
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

import grails.cache.GrailsCacheKeyGenerator
import grails.cache.SimpleCacheKeyGenerator

/**
 * A trait for classes that are cache aware
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2024.0.0
 */
@CompileStatic
trait GrailsCacheManagerAware {

    private GrailsCacheKeyGenerator grailsCacheKeyGenerator = new SimpleCacheKeyGenerator()

    private GrailsCacheManager grailsCacheManager

    void setGrailsCacheManager(GrailsCacheManager grailsCacheManager) {
        this.grailsCacheManager = grailsCacheManager
    }

    /**
     * @return The Grails cache manager or null if it isn't present
     */
    GrailsCacheManager getGrailsCacheManager() {
        return this.grailsCacheManager
    }

    void setGrailsCacheKeyGenerator(GrailsCacheKeyGenerator grailsCacheKeyGenerator) {
        this.grailsCacheKeyGenerator = grailsCacheKeyGenerator
    }

    /**
     * @return The custom key generator, or null if it isn't present
     */
    GrailsCacheKeyGenerator getGrailsCacheKeyGenerator() {
        return this.grailsCacheKeyGenerator
    }

}
