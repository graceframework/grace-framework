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
package org.grails.cli.command.about

import groovy.transform.CompileStatic

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT

/**
 * About command gives information about version numbers for Grace, Groovy, Gradle, Spring Boot,
 * the application's name, version, folder, and the current environment name.
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
@CompileStatic
class AboutCommand implements ProjectCommand {

    static final String USAGE = 'grace about'
    static final String EXAMPLES = '''
    # List versions of the Grace application and the environment
        $ grace about
'''

    final String name = 'about'
    final CommandDescription description = new CommandDescription(name, 'List versions of Grace application and the environment',
            USAGE, EXAMPLES, ['a'])

    AboutCommand() {
        populateDescription()
    }

    private void populateDescription() {
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

        try {
            console.addStatus("About your application's environment")
            console.println()
            console.println('Name:               ' + ApplicationInfo.appName)
            console.println('Version:            ' + ApplicationInfo.appVersion)
            console.println('Application root:   ' + ApplicationInfo.appRoot)
            console.println('Environment:        ' + ApplicationInfo.appEnvironment)
            console.println()
            console.println('Grace:              ' + ApplicationInfo.grailsVersion)
            console.println('Groovy:             ' + ApplicationInfo.groovyVersion)
            console.println('Gradle:             ' + ApplicationInfo.gradleVersion)
            console.println('Spring Boot:        ' + ApplicationInfo.springBootVersion)
            console.println('JVM:                ' + ApplicationInfo.javaVersion)
            console.println('OS:                 ' + ApplicationInfo.osVersion)
            console.println()
            return true
        }
        catch (Exception ignored) {
            return false
        }
    }

    void printCommandHelp(GrailsConsole console) {
        console.out.println('Usage:')
        console.out.println('  ' + USAGE)
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
