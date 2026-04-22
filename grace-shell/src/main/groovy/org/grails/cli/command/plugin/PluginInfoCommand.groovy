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
package org.grails.cli.command.plugin

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.Command
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.GlobalCommand
import org.grails.cli.command.ProjectCommand

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT

/**
 * Get the information about the given plugin
 *
 * @author Michael Yan
 * @since 2023.3.0
 */
class PluginInfoCommand implements Command, GlobalCommand, ProjectCommand {

    public static final String PLUGIN_REPO_URL = 'https://repo1.maven.org/maven2/org/graceframework/plugins'
    public static final String NAME = 'plugin-info'
    static final String USAGE = 'grace plugin-info [PLUGIN NAME]'
    static final String EXAMPLES = '''
    # Display information about a given plugin
        $ grace plugin-info admin
'''

    final String name = NAME
    final CommandDescription description = new CommandDescription(name,
            'Display information about the given plugin',
            USAGE, EXAMPLES
    )

    PluginInfoCommand() {
        description.argument(name: 'Plugin Name', description: 'The name of the plugin', required: true)
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: "Print the options and usage", required: false)
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        if (commandLine.hasOption(CommandLine.HELP_ARGUMENT) || commandLine.hasOption('h')) {
            printCommandHelp(console)
            return true
        }

        String pluginName = commandLine.remainingArgs[0]

        if (!pluginName) {
            console.error 'Missing the name of the plugin!'
            return false
        }

        try {
            console.addStatus "Plugin Name: ${pluginName}"
            GPathResult mavenMetadata = new XmlSlurper().parseText(new URL("${PLUGIN_REPO_URL}/${pluginName}/maven-metadata.xml").text)
            String latestVersion = mavenMetadata.versioning.release.text()
            if (!latestVersion) {
                latestVersion = mavenMetadata.versioning.latest.text()
            }
            console.addStatus "Latest Version: ${latestVersion}"

            List allVersions = mavenMetadata.versioning.versions.version*.text()
            console.addStatus "All Versions: ${allVersions?.reverse()?.join(', ')}"

            GPathResult pluginInfo
            if (latestVersion.endsWith('-SNAPSHOT')) {
                GPathResult versionMetadata = new XmlSlurper().parseText(
                        new URL("${PLUGIN_REPO_URL}/${pluginName}/${latestVersion}/maven-metadata.xml").text)
                String snapshotVersion = versionMetadata.version.text()
                pluginInfo = new XmlSlurper().parseText(
                        new URL("${PLUGIN_REPO_URL}/${pluginName}/${latestVersion}/${pluginName}-${snapshotVersion}-plugin.xml").text)
            }
            else {
                pluginInfo = new XmlSlurper().parseText(
                        new URL("${PLUGIN_REPO_URL}/${pluginName}/${latestVersion}/${pluginName}-${latestVersion}-plugin.xml").text)
            }

            if (pluginInfo) {
                console.addStatus "Title: ${pluginInfo.title.text()}"
                String desc = pluginInfo.description.text()
                if (desc) {
                    console.log('')
                    console.log(desc)
                    console.log('')
                }

                console.log "* License: ${pluginInfo.license.text()}"

                if (pluginInfo.documentation) {
                    console.log "* Documentation: ${pluginInfo.documentation.text()}"
                }
                if (pluginInfo.issueManagement) {
                    console.log "* Issue Tracker: ${pluginInfo.issueManagement.@url.text()}"
                }
                if (pluginInfo.scm) {
                    console.log "* Source: ${pluginInfo.scm.@url.text()}"
                }

                console.log """* Definition:

dependencies {
    implementation "org.graceframework.plugins:${pluginName}:${latestVersion}"    
}

"""
            }
        }
        catch(Throwable e) {
            console.error "Failed to display plugin info: ${e.message}", e
            return false
        }

        true
    }

    void printCommandHelp(GrailsConsole console) {
        console.out.println('Usage:')
        console.out.println('  ' + description.usage)
        console.out.println()

        if (!description.getFlags().isEmpty()) {
            console.out.println('Options:')
        }
        description.getFlags().each {f ->
            def flag = new StringBuilder()
            flag << '  ' << (f.aliases ? "$f.aliases, " : '    ')
            if (f.type == 'boolean') {
                flag << "[--${f.name}]".padRight(30)
            }
            else {
                flag << "[--${f.name}=${f.banner}]".padRight(30)
            }
            flag << '# ' + f.description
            console.out.println(flag)
        }
        console.out.println()

        console.out.println('Description:')
        console.out.println('    ' + description.description)
        console.out.println()

        console.out.println('Examples:')
        console.out.println(description.examples)
    }

}
