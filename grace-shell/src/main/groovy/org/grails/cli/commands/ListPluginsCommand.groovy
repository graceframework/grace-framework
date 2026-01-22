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
package org.grails.cli.commands

import groovy.transform.CompileStatic

import grails.build.logging.GrailsConsole

/**
 * Lists the available plugins from the Plugin Repository
 *
 * @author Michael Yan
 * @since 2023.3.0
 */
@CompileStatic
class ListPluginsCommand implements Command, GlobalCommand, ProjectCommand {

    public static final String PLUGIN_REPO_URL = 'https://repo1.maven.org/maven2/org/graceframework/plugins'
    private static final List<String> EXCLUDES = [
        'asset-pipeline-core',
        'asset-pipeline-gradle',
        'async',
        'cache',
        'database-migration',
        'dynamic-modules',
        'events',
        'fields',
        'geb',
        'gsp',
        'hibernate',
        'mongodb',
        'scaffolding',
        'views-component',
        'views-gradle',
        'views-json',
        'views-json-templates',
        'views-markup'
    ]

    final String name = 'list-plugins'
    final CommandDescription description = new CommandDescription(name,
            'Lists the available plugins from the Plugin Repository',
            'grace list-plugins')


    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        try {
            console.addStatus 'Available Plugins'
            String text = new URL(PLUGIN_REPO_URL).text
            text.eachMatch(/<a href="([a-z-]+)\/" title="([a-z-]+)\/">.+/) { List<String> it ->
                String plugin = it[1]
                if (!EXCLUDES.contains(plugin)) {
                    console.log "* ${it[1]}"
                }
            }

            console.println()
            console.log "You can find more information on https://central.sonatype.com/namespace/org.graceframework.plugins"
        }
        catch(Throwable e) {
            console.error "Failed to list plugins", e
            return false
        }

        true
    }

}
