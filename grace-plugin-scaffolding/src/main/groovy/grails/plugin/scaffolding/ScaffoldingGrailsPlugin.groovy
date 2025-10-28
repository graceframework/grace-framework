/*
 * Copyright 2015-2025 the original author or authors.
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
package grails.plugin.scaffolding

import groovy.transform.CompileStatic

import grails.plugins.Plugin
import grails.util.GrailsUtil

/**
 * @author Graeme Rocher
 * @since 2024.0.0
 */
@CompileStatic
class ScaffoldingGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def grailsVersion = '2023.0.0 > *'
    def title = 'Scaffolding Plugin'
    def description = 'Plugin that generates scaffolded controllers and views for a Grace application.'

}
