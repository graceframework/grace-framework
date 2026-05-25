/*
 * Copyright 2015-2026 the original author or authors.
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
package org.grails.plugins.databasemigration

import groovy.transform.CompileStatic
import liquibase.parser.ChangeLogParser
import liquibase.parser.ChangeLogParserFactory

import grails.plugins.Plugin
import grails.util.GrailsUtil

import org.grails.plugins.databasemigration.liquibase.GroovyChangeLogParser

@CompileStatic
class DatabaseMigrationGrailsPlugin extends Plugin {

    def version = GrailsUtil.getGrailsVersion()
    def grailsVersion = '2024.0.0 > *'
    def title = 'Grace Database Migration Plugin'

    @Override
    Closure doWithSpring() {
        configureLiquibase()
        return { -> }
    }

    private void configureLiquibase() {
        def groovyChangeLogParser = ChangeLogParserFactory.instance.parsers.find { ChangeLogParser changeLogParser ->
            changeLogParser instanceof GroovyChangeLogParser } as GroovyChangeLogParser
        groovyChangeLogParser.applicationContext = applicationContext
        groovyChangeLogParser.config = config
    }

    static String getDataSourceName(String dataSourceName) {
        isDefaultDataSource(dataSourceName) ? dataSourceName : "dataSource_$dataSourceName"
    }

    static Boolean isDefaultDataSource(String dataSourceName) {
        !dataSourceName || 'dataSource' == dataSourceName
    }

}
