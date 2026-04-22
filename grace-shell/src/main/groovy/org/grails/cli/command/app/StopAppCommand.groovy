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

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand
import org.grails.config.CodeGenConfig

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT
import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Stops the running Grace application
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
class StopAppCommand implements ProjectCommand {

    static final String USAGE = 'grace stop-app'
    static final String EXAMPLES = '''
    # Stops the running Grace application
        $ grace stop-app
'''

    final String name = 'stop-app'
    final CommandDescription description = new CommandDescription(name, 'Stops the running Grace application', USAGE, EXAMPLES, ['stop'])

    StopAppCommand() {
        populateDescription()
    }

    private void populateDescription() {
        description.flag(name: 'host', type: 'integer', description: 'Specifies the host to bind to', banner: 'HOST', required: false)
        description.flag(name: 'port', type: 'integer', description: 'Specifies the port which to start Grace on (defaults to 8080 or 8443 for HTTPS)', banner: 'PORT', required: false)
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

        console.updateStatus 'Shutting down application...'

        Integer port = flag(commandLine, 'port')?.toInteger() ?: config.getProperty('server.port', Integer, 8080)
        String host = flag(commandLine, 'host') ?: config.getProperty('server.address', String, 'localhost')

        try {
            String contextPath = config.getProperty('server.context-path') ?: config.getProperty('server.contextPath') ?: ''
            String managementPath = config.getProperty('management.endpoints.web.base-path') ?: config.getProperty('management.endpoints.web.basePath') ?: '/actuator'

            def url = new URL("http://${host}:${port}${contextPath}${managementPath}/shutdown")
            def connection = url.openConnection()
            connection.setRequestMethod('POST')
            connection.doOutput = true
            connection.connect()
            console.updateStatus connection.content.text
            while (isServerAvailable(host, port)) {
                sleep 100
            }
            console.updateStatus 'Application shutdown.'

            System.setProperty('run-app.running', 'false')

            return true
        }
        catch (java.net.ConnectException e) {
            console.error 'Application not running.', e
            return false
        }
        catch (java.io.FileNotFoundException ignored) {
            console.info '''# Please check that '/shutdown' endpoint enabled in 'app/conf/application.yml'
management:
    endpoint:
        shutdown:
            enabled: true

'''
            return false
        }
        catch (Exception e) {
            console.error 'Application shutdown error: ', e
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