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
package org.grails.plugins.databasemigration.liquibase

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import liquibase.changelog.ChangeLogParameters
import liquibase.exception.ChangeLogParseException
import liquibase.parser.core.ParsedNode
import liquibase.parser.core.xml.AbstractChangeLogParser
import liquibase.resource.Resource
import liquibase.resource.ResourceAccessor
import liquibase.util.FileUtil
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.context.ApplicationContext

import grails.config.ConfigMap

import static org.grails.plugins.databasemigration.PluginConstants.DATA_SOURCE_NAME_KEY

@CompileStatic
class GroovyChangeLogParser extends AbstractChangeLogParser {

    final int priority = PRIORITY_DEFAULT

    ApplicationContext applicationContext

    ConfigMap config

    @Override
    @CompileDynamic
    protected ParsedNode parseToNode(String physicalChangeLogLocation, ChangeLogParameters changeLogParameters,
            ResourceAccessor resourceAccessor) throws ChangeLogParseException {
        try {
            Resource resource = resourceAccessor.get(physicalChangeLogLocation)
            if (resource.exists()) {
                CompilerConfiguration compilerConfiguration = new CompilerConfiguration(CompilerConfiguration.DEFAULT)
                if (compilerConfiguration.metaClass.respondsTo(compilerConfiguration, 'setDisabledGlobalASTTransformations')) {
                    Set disabled = compilerConfiguration.disabledGlobalASTTransformations ?: []
                    disabled << 'org.grails.datastore.gorm.query.transform.GlobalDetachedCriteriaASTTransformation'
                    compilerConfiguration.disabledGlobalASTTransformations = disabled
                }

                def changeLogProperties = config.getProperty('changelogProperties', Map) ?: [:]

                String changeLogText = resource.openInputStream().text
                GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, compilerConfiguration, false)
                Script script = new GroovyShell(classLoader, new Binding(changeLogProperties), compilerConfiguration).parse(changeLogText as String)
                script.run()

                setChangeLogProperties(changeLogProperties, changeLogParameters)

                Closure databaseChangeLogBlock = script.getProperty('databaseChangeLog') as Closure

                DatabaseChangeLogBuilder builder = new DatabaseChangeLogBuilder()
                builder.dataSourceName = changeLogParameters.getValue(DATA_SOURCE_NAME_KEY, null)
                builder.applicationContext = applicationContext
                builder.databaseChangeLog(databaseChangeLogBlock) as ParsedNode
            }
            else {
                throw new ChangeLogParseException(FileUtil.getFileNotFoundMessage(physicalChangeLogLocation))
            }
        }
        catch (Exception e) {
            throw new ChangeLogParseException(e)
        }
    }

    @Override
    boolean supports(String changeLogFile, ResourceAccessor resourceAccessor) {
        changeLogFile.endsWith('.groovy')
    }

    @CompileDynamic
    protected void setChangeLogProperties(Map changeLogProperties, ChangeLogParameters changeLogParameters) {
        changeLogProperties.each { name, value ->
            String contexts = null
            String labels = null
            String databases = null
            if (value instanceof Map) {
                contexts = value.contexts
                labels = value.labels
                databases = value.databases
                value = value.value
            }
            changeLogParameters.set(name as String, value as String, contexts as String, labels, databases, true, null)
        }
    }

}
