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
package org.grails.testing.gorm.spock

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

import grails.testing.gorm.DataTest

@CompileStatic
class GormTestingSupportExtension implements IGlobalExtension {

    DataTestSetupSpecInterceptor dataTestSetupSpecInterceptor = new DataTestSetupSpecInterceptor()
    DataTestSetupInterceptor dataTestSetupInterceptor = new DataTestSetupInterceptor()
    DataTestCleanupInterceptor dataTestCleanupInterceptor = new DataTestCleanupInterceptor()
    DataTestCleanupSpecInterceptor dataTestCleanupSpecInterceptor = new DataTestCleanupSpecInterceptor()

    @Override
    void visitSpec(SpecInfo spec) {
        if (DataTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupSpecInterceptor(dataTestSetupSpecInterceptor)
            spec.addSetupInterceptor(dataTestSetupInterceptor)
            spec.addCleanupInterceptor(dataTestCleanupInterceptor)
            spec.addCleanupSpecInterceptor(dataTestCleanupSpecInterceptor)
        }
    }

}
