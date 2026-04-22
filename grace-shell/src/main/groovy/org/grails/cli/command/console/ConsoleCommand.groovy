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
package org.grails.cli.command.console

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.GrailsCli
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand
import org.grails.cli.gradle.GradleInvoker

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Runs the Grace interactive console
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
class ConsoleCommand implements ProjectCommand {

    static final String USAGE = 'grace console'
    static final String EXAMPLES = '''
    # Runs the Grace interactive console
        $ grace console
'''

    final String name = 'console'
    final CommandDescription description = new CommandDescription(name, 'Runs the Grace interactive console', USAGE, EXAMPLES)

    ConsoleCommand() {
        populateDescription()
    }

    private void populateDescription() {
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: "Print the options and usage", required: false)
        description.flag(name: VERBOSE_ARGUMENT, type: 'boolean', description: 'Show verbose output', required: false)
        description.flag(name: 'debug-jvm', type: 'boolean', description: 'Starts the JVM in debug mode allowing attachment of a remote debugger', required: false)
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
            boolean isIm = GrailsCli.isInteractiveModeActive()
            def gradle = new GradleInvoker(executionContext)
            if (isIm) {
                System.setProperty('org.gradle.console', 'plain')
                gradle = gradle.async
            }

            List<String> arguments = []
            boolean verbose = flag(commandLine, 'verbose')
            if (!(verbose || console.verbose)) {
                arguments << '--quiet'
            }
            arguments.addAll(commandLine.remainingArgs)

            console.updateStatus 'Running console...'

            boolean debugJvm = flag(commandLine, 'debug-jvm')
            if (debugJvm) {
                gradle.'console --debug-jvm'(*arguments)
            }
            else {
                gradle.console(*arguments)
            }

            return true
        }
        catch (Exception ignored) {
            return false
        }
    }

    def flag(CommandLine commandLine, String name) {
        if (commandLine.hasOption(name)) {
            return commandLine.optionValue(name)
        }

        def value = commandLine?.undeclaredOptions?.get(name)
        value ?: null
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
