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
package org.grails.cli.command.gradle

import org.gradle.tooling.BuildLauncher

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.Environment

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand
import org.grails.cli.gradle.GradleInvoker
import org.grails.cli.gradle.GradleUtil

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT

/**
 * Creates a JAR or WAR archive for production deployment
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
class AssembleCommand implements ProjectCommand {

    static final String USAGE = 'grace assemble'
    static final String EXAMPLES = '''
    # Creates a JAR or WAR archive for production deployment
        $ grace assemble
'''

    final String name = 'assemble'
    final CommandDescription description = new CommandDescription(name, 'Creates a JAR or WAR archive for production deployment',
            USAGE, EXAMPLES, ['war', 'package'])

    AssembleCommand() {
        populateDescription()
    }

    private void populateDescription() {
        description.flag(name: 'clean', type: 'boolean', description: "Execute 'clean' prior to creating WAR", required: false)
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
            if (commandLine.hasOption('clean')) {
                console.addStatus 'Cleanup ...'
                GradleUtil.runBuildWithConsoleOutput(executionContext) { BuildLauncher buildLauncher ->
                    buildLauncher.withArguments('clean')
                }
            }

            // configure environment to production if it is not specified
            if (!commandLine.isEnvironmentSet()) {
                System.setProperty('grails.env', 'production')
            }
            else {
                System.setProperty('grails.env', commandLine.environment)
            }

            Environment.reset()

            def arguments = []
            commandLine.systemProperties.each { key, value ->
                arguments << "-D${key}=$value".toString()
            }

            console.addStatus 'Assembles the outputs ...'

            def gradle = new GradleInvoker(executionContext)
            gradle.'assemble'(*arguments)

            String buildPath = new File(BuildSettings.TARGET_DIR, '/libs').canonicalPath
            console.addStatus "Built application to ${buildPath} using environment: ${Environment.current.name}"

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
