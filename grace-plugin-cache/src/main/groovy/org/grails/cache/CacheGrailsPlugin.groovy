/*
 * Copyright 2012-2024 the original author or authors.
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

import grails.plugins.Plugin
import grails.util.GrailsUtil

@CompileStatic
class CacheGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def grailsVersion = '2023.0.0 > *'
    def observe = ['controllers', 'services']
    def loadAfter = ['controllers', 'services']
    def title = 'Grace Cache Plugin'
    def description = 'Provides AST transformations for caching method calls'

}
