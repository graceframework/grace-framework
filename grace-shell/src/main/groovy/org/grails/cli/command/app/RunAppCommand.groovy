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
package org.grails.cli.command.app

import org.gradle.tooling.BuildCancelledException

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.GrailsCli
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand
import org.grails.cli.gradle.GradleInvoker
import org.grails.config.CodeGenConfig

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT
import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Runs a Grace application
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
class RunAppCommand implements ProjectCommand {

    static final String USAGE = 'grace run-app'
    static final String EXAMPLES = '''
    # Runs a Grace application
        $ grace run-app
'''

    final String name = 'run-app'
    final CommandDescription description = new CommandDescription(name, 'Runs a Grace application', USAGE, EXAMPLES)

    RunAppCommand () {
        populateDescription()
    }

    private void populateDescription() {
        description.flag(name: 'host', type: 'integer', description: 'Specifies the host to bind to', banner: 'HOST', required: false)
        description.flag(name: 'port', type: 'integer', description: 'Specifies the port which to start Grace on (defaults to 8080 or 8443 for HTTPS)', banner: 'PORT', required: false)
        description.flag(name: 'debug-jvm', type: 'boolean', description: 'Starts the JVM in debug mode allowing attachment of a remote debugger', required: false)
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: 'Print the options and usage', required: false)
        description.flag(name: STACKTRACE_ARGUMENT, type: 'boolean', description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, type: 'boolean', description: 'Show verbose output', required: false)
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        CodeGenConfig config = executionContext.config
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        if (commandLine.hasOption(CommandLine.HELP_ARGUMENT) || commandLine.hasOption('h')) {
            printCommandHelp(console)
            return true
        }

        try {
            if (!commandLine.isEnvironmentSet()) {
                System.setProperty('grails.env', 'dev')
            }
            else {
                System.setProperty('grails.env', commandLine.environment)
            }
            def gradle = new GradleInvoker(executionContext)

            List<String> arguments = []
            boolean verbose = flag(commandLine, 'verbose')
            if (!(verbose || console.verbose)) {
                arguments << '-q' << '--console=plain'
            }
            arguments.addAll(commandLine.remainingArgs)
            arguments << "-Dgrails.run.active=true"
            commandLine.systemProperties.each { key, value ->
                arguments << "-D${key}=$value".toString()
            }

            Integer port = flag(commandLine, 'port')?.toInteger() ?: config.getProperty('server.port', Integer)
            String host = flag(commandLine, 'host') ?: config.getProperty('server.address', String)

            if (port) {
                arguments << "-Dgrails.server.port=$port"
            }
            if (host) {
                arguments << "-Dgrails.server.address=$host"
            }

            console.updateStatus 'Running application...'

            if (!GrailsCli.isInteractiveModeActive()) {
                if (flag(commandLine, 'debug-jvm')) {
                    gradle.'bootRun --debug-jvm'(*arguments)
                }
                else {
                    gradle.'bootRun'(*arguments)
                }
            }
            else {
                System.setProperty('org.gradle.console', 'plain')

                def future
                if (flag(commandLine, 'debug-jvm')) {
                    future = gradle.async."bootRun --debug-jvm"(*arguments)
                }
                else {
                    future = gradle.async."bootRun"(*arguments)
                }

                while (!isServerAvailable(host ?: 'localhost', port ?: 8080)) {
                    console.indicateProgress()
                    if (future.done) {
                        // the server exited for some reason, so break
                        if (future.get() instanceof Throwable) {
                            break
                        }
                    }
                    sleep(100)
                }

                System.setProperty('run-app.running', 'true')

                if (!Boolean.getBoolean('run-app.shutdown.hook.registered')) {
                    System.setProperty('run-app.shutdown.hook.registered', 'true')
                    addShutdownHook {
                        if (Boolean.getBoolean('run-app.running')) {
                            try {
                                stopApp()
                            }
                            catch (ignore) {
                            }
                        }
                    }
                }

                sleep 500
            }

            return true
        }
        catch (BuildCancelledException ignore) {
            console.updateStatus('Application stopped')
            return true
        }
        catch (Throwable e) {
            console.error 'Failed to start server', e
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

    boolean isServerAvailable(String host, Integer port) {
        try {
            new Socket(host, port)
            return true
        }
        catch (ignored) {
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