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
package org.grails.cli.command.stats

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT
import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Prints statistics about the project
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
class StatsCommand implements ProjectCommand {

    static final String USAGE = 'grace stats'
    static final String EXAMPLES = '''
    # Prints statistics about the project
        $ grace stats
'''

    final String name = 'stats'
    final CommandDescription description = new CommandDescription(name, 'Prints statistics about the project', USAGE, EXAMPLES)

    final def EMPTY = /^\s*$/
    final def SLASH_SLASH = /^\s*\/\/.*/
    final def SLASH_STAR_STAR_SLASH = /^(.*)\/\*(.*)\*\/(.*)$/

    // TODO - handle slash_star comments inside strings
    def DEFAULT_LOC_MATCHER = { File file ->
        int loc = 0
        int comment = 0
        file.eachLine { line ->
            if (line ==~ EMPTY) return
            if (line ==~ SLASH_SLASH) return
            def m = line =~ SLASH_STAR_STAR_SLASH
            if (m.count && m[0][1] ==~ EMPTY && m[0][3] ==~ EMPTY) return
            int open = line.indexOf('/*')
            int close = line.indexOf('*/')
            if (open != -1 && (close-open) <= 1) comment++
            else if (close != -1 && comment) comment--
            if (!comment) loc++
        }
        loc
    }

    // maps file path to
    def pathToInfo = [
            [name: 'Controllers',        path: '^app.controllers',             filetype: ['Controller.groovy']],
            [name: 'URL Mappings',       path: '^app.controllers',             filetype: ['UrlMappings.groovy']],
            [name: 'Interceptors',       path: '^app.controllers',             filetype: ['Interceptor.groovy']],
            [name: 'Domain Classes',     path: '^app.domain',                  filetype: ['.groovy']],
            [name: 'Jobs',               path: '^app.jobs',                    filetype: ['.groovy']],
            [name: 'Services',           path: '^app.services',                filetype: ['Service.groovy']],
            [name: 'Tag Libraries',      path: '^app.taglibs',                 filetype: ['TagLib.groovy']],
            [name: 'Helpers',            path: '^src.main.groovy',             filetype: ['.groovy', '.java']],
            [name: 'Unit Tests',         path: '^src.test.groovy',             filetype: ['.groovy', '.java']],
            [name: 'Integration Tests',  path: '^src.integration-test.groovy', filetype: ['.groovy', '.java']],
            [name: 'Scripts',            path: '^src.main.scripts',            filetype: ['.groovy']],
    ]

    StatsCommand() {
        populateDescription()
    }

    private void populateDescription() {
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: "Print the options and usage", required: false)
        description.flag(name: STACKTRACE_ARGUMENT, type: 'boolean', description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, type: 'boolean', description: 'Show verbose output', required: false)
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
            def baseDirFile = executionContext.baseDir
            def baseDirPathLength = baseDirFile.path.size() + 1
            baseDirFile.eachFileRecurse { file ->
                def match = pathToInfo.find { info ->
                    file.path.substring(baseDirPathLength) =~ info.path &&
                            info.filetype.any{ s -> file.path.endsWith(s) }
                }
                if (match && file.isFile()) {
                    match.filecount = match.filecount ? match.filecount+1 : 1
                    // strip whitespace
                    int loc = match.locmatcher ? match.locmatcher(file) : DEFAULT_LOC_MATCHER(file)
                    match.loc = match.loc ? match.loc + loc : loc
                }
            }

            int totalFiles = 0
            int totalLOC = 0

            def sw = new StringWriter()
            def output = new PrintWriter(sw)

            output.println '''
+--------------------------------+------------+------------+
| Name                           |   Files    |     LOC    |
+--------------------------------+------------+------------+'''

            pathToInfo.each { info ->
                if (info.filecount) {
                    output.println '| ' +
                            info.name.padRight(30, ' ') + ' | ' +
                            info.filecount.toString().padLeft(10, ' ') + ' | ' +
                            info.loc.toString().padLeft(10,' ') + ' | '
                    totalFiles += info.filecount
                    totalLOC += info.loc
                }
            }

            output.println '+--------------------------------+------------+------------+'
            output.println '| Totals                         | ' + totalFiles.toString().padLeft(10, ' ') + ' | ' + totalLOC.toString().padLeft(10, ' ') + ' | '
            output.println '+--------------------------------+------------+------------+\n'

            console.addStatus("About your application's environment")
            console.out.println(sw.toString())

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
