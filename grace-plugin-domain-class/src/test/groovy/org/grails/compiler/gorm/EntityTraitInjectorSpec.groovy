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
package org.grails.compiler.gorm

import spock.lang.Specification

import grails.gorm.Entity
import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.compiler.injection.TraitInjectionUtils

/**
 * Test {@link EntityTraitInjector}
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
class EntityTraitInjectorSpec extends Specification {

    void "Test Domain class was injected by 'EntityTraitInjector'"() {
        given:
        TraitInjectionUtils.@traitInjectors = [new EntityTraitInjector()]
        GrailsAwareClassLoader gcl = new GrailsAwareClassLoader()
        gcl.metaDataMap = [
                'GRAILS_APP_DIR': ['', 'Users', 'grace', 'grace-demo-project', 'app'].join(File.separator),
                'PROJECT_DIR': ['', 'Users', 'grace', 'grace-demo-project'].join(File.separator),
                'PROJECT_TYPE': 'WEB_APP'
        ]

        when:
        def clazz = gcl.parseClass('''
class Person {
}
''', ['', 'Users', 'grace', 'grace-demo-project', 'app', 'domain', 'org', 'demo', 'Person.groovy'].join(File.separator))

        def person = clazz.newInstance()

        then:
        person instanceof Entity

        cleanup:
        TraitInjectionUtils.@traitInjectors = null
    }

}
