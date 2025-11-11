/*
 * Copyright 2016-2025 the original author or authors.
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
package grails.plugins.mongodb

import groovy.transform.CompileStatic

import grails.plugins.Plugin

/**
 * Plugin that integrates Hibernate into a Grails application
 *
 * @author Graeme Rocher
 * @author Puneet Behl
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class MongodbGrailsPlugin extends Plugin {

    def grailsVersion = '2023.0.0 > *'

    def author = 'Grace Framework'
    def title = 'Grace Data MongoDB'
    def description = 'Provides integration between Grace and MongoDB document datastore through GORM API'
    def documentation = 'https://github.com/graceframework/grace-data-mongodb'

    def observe = ['domainClass']
    def loadAfter = ['domainClass']

    def license = 'APACHE'
    def organization = [name: 'Grace Framework', url: 'https://graceframework.org']
    def issueManagement = [system: 'Github', url: 'https://github.com/graceframework/grace-data-mongodb/issues']
    def scm = [url: 'https://github.com/graceframework/grace-data-mongodb']

}
