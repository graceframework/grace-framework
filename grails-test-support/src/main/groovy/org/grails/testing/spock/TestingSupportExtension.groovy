/*
 * Copyright 2016-2022 the original author or authors.
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
package org.grails.testing.spock

import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import groovy.transform.CompileStatic
import org.junit.jupiter.api.BeforeEach
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.MethodKind
import org.spockframework.runtime.model.SpecInfo

import grails.testing.spring.AutowiredTest

import org.grails.testing.GrailsUnitTest

@CompileStatic
class TestingSupportExtension extends AbstractGlobalExtension {

    AutowiredInterceptor autowiredInterceptor = new AutowiredInterceptor()
    CleanupContextInterceptor cleanupContextInterceptor = new CleanupContextInterceptor()

    @Override
    void visitSpec(SpecInfo spec) {
        if (AutowiredTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupInterceptor(autowiredInterceptor)
        }
        if (GrailsUnitTest.isAssignableFrom(spec.reflection)) {
            spec.addCleanupSpecInterceptor(cleanupContextInterceptor)
        }
        for (Method method : spec.getReflection().getDeclaredMethods()) {
            if (method.isAnnotationPresent(BeforeEach)) {
                spec.addSetupMethod(createJUnitFixtureMethod(spec, method, MethodKind.SETUP, BeforeEach))
            }
        }
    }

    private MethodInfo createMethod(SpecInfo specInfo, Method method, MethodKind kind, String name) {
        MethodInfo methodInfo = new MethodInfo()
        methodInfo.setParent(specInfo)
        methodInfo.setName(name)
        methodInfo.setReflection(method)
        methodInfo.setKind(kind)
        methodInfo
    }

    private MethodInfo createJUnitFixtureMethod(SpecInfo specInfo, Method method, MethodKind kind, Class<? extends Annotation> annotation) {
        MethodInfo methodInfo = createMethod(specInfo, method, kind, method.getName())
        methodInfo.setExcluded(isOverriddenJUnitFixtureMethod(specInfo, method, annotation))
        methodInfo
    }

    private boolean isOverriddenJUnitFixtureMethod(SpecInfo specInfo, Method method, Class<? extends Annotation> annotation) {
        if (Modifier.isPrivate(method.getModifiers())) {
            return false
        }

        for (Class<?> currClass = specInfo.class; currClass != specInfo.class.superclass; currClass = currClass.getSuperclass()) {
            for (Method currMethod : currClass.getDeclaredMethods()) {
                if (!currMethod.isAnnotationPresent(annotation)) {
                    continue
                }
                if (!currMethod.getName().equals(method.getName())) {
                    continue
                }
                if (!Arrays.deepEquals(currMethod.getParameterTypes(), method.getParameterTypes())) {
                    continue
                }
                return true
            }
        }

        false
    }

}
