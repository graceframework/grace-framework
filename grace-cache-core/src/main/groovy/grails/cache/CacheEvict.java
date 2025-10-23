/*
 * Copyright 2012-2025 the original author or authors.
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
package grails.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import org.grails.datastore.gorm.transform.GormASTTransformationClass;

/**
 * Indicates that a method (or all methods on a class) trigger(s)
 * a cache invalidate operation.
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 * @since 2024.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@GroovyASTTransformationClass("org.grails.datastore.gorm.transform.OrderedGormTransformation")
@GormASTTransformationClass("org.grails.cache.compiler.CacheEvictTransformation")
public @interface CacheEvict {

    /**
     * Qualifier value for the specified cached operation.
     * <p>May be used to determine the target cache (or caches), matching the qualifier
     * value.
     */
    String[] value();

    /**
     * A closure for computing the key dynamically.
     * <p>Default is null, meaning all method parameters are considered as a key.
     */
    Class[] key() default {};

    /**
     * A closure used for conditioning the method caching.
     * <p>Default is null, meaning the method is always cached.
     */
    Class[] condition() default {};

    /**
     * Weather or not all the entries inside the cache(s) are removed or not. By
     * default, only the value under the associated key is removed.
     * <p>Note that specifying setting this parameter to true and specifying a
     * CacheKey is not allowed.
     */
    boolean allEntries() default false;
}
