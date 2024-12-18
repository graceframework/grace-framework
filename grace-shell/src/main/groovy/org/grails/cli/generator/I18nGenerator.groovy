/*
 * Copyright 2022-2024 the original author or authors.
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
package org.grails.cli.generator

import groovy.transform.CompileStatic

import grails.cli.generator.AbstractGenerator
import grails.util.GrailsNameUtils

/**
 * @author Michael Yan
 * @since 2023.2.0
 */
@CompileStatic
class I18nGenerator extends AbstractGenerator {

    @Override
    boolean generate() {
        String[] args = commandLine.remainingArgs.toArray(new String[0])
        if (args.size() < 2) {
            return
        }

        String className = args[1].capitalize()
        String propertyName = className.uncapitalize()
        Map<String, String> classAttributes = new LinkedHashMap<>()
        String[] attributes = (args.size() >= 3 ? args[2..-1] : []) as String[]
        attributes.each { String item ->
            String[] attr = (item.contains(':') ? item.split(':') : [item, 'String']) as String[]
            classAttributes[attr[0]] = attr[1]
        }

        StringBuilder text = new StringBuilder()
        text.append('\n# ' + className + '\n')
        classAttributes.each { attr ->
            String label = propertyName + '.' + attr.key + '.label'
            String value = GrailsNameUtils.getNaturalName(attr.key)

            text.append(label + '=' + value + '\n')
        }

        prependToFile('app/i18n/messages.properties', text.toString())

        true
    }

}
