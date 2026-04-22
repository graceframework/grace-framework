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
package org.grails.cli.command.help

import jline.console.completer.Completer

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.grails.cli.command.Command
import org.grails.cli.command.CommandArgument
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.CommandRegistry
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.GlobalCommand
import org.grails.cli.command.ProjectCommand
import org.grails.cli.command.ProjectContext
import org.grails.cli.command.ProjectContextAware
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileCommand
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware

/**
 * {@code 'help'} command.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
class HelpCommand implements ProfileCommand, Completer, ProjectContextAware, ProfileRepositoryAware {

    public static final String NAME = 'help'
    static final String USAGE = 'grace help [COMMAND NAME]'
    static final String EXAMPLES = '''
    # Show all the commands
        $ grace help -all

    # Prints help information for create-app command
        $ grace help create-app
'''

    CommandDescription description = new CommandDescription(NAME,
            'Prints help information for a specific command',
            USAGE, EXAMPLES)

    Profile profile
    ProfileRepository profileRepository
    ProjectContext projectContext

    private CommandLineParser cliParser = new CommandLineParser()

    HelpCommand() {
        this.description.flag(name: 'all', type: 'boolean', description: 'Show all commands, default: false', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        if (commandLine.hasOption(CommandLine.HELP_ARGUMENT) || commandLine.hasOption('h')) {
            printCommandHelp(console)
            return true
        }

        Collection<Command> allCommands
        String remainingArgs = commandLine.getRemainingArgsString()
        if (remainingArgs?.trim()) {
            allCommands = findCommands(true)
            CommandLine remainingArgsCommand = cliParser.parseString(remainingArgs)
            String helpCommandName = remainingArgsCommand.getCommandName()
            for (Command command : allCommands) {
                if (command.fullName == helpCommandName) {
                    StringBuilder name = new StringBuilder()
                    name.append(command.fullName)
                    appendMessage(name, command.isDeprecated(), '[deprecated]')
                    console.addStatus("Command: $name")
                    console.addStatus('Description:')
                    console.println "${command.description.description ?: ''}"
                    if (command.description.usage) {
                        console.println()
                        console.addStatus('Usage:')
                        console.println "${command.description.usage}"
                    }
                    if (command.description.arguments) {
                        console.println()
                        console.addStatus('Arguments:')
                        for (arg in command.description.arguments) {
                            console.println "* ${arg.name} - ${arg.description ?: ''} (${arg.required ? 'REQUIRED' : 'OPTIONAL'})"
                        }
                    }
                    if (command.description.flags) {
                        console.println()
                        console.addStatus('Options:')
                        command.description.flags.each { CommandArgument f ->
                            def flag = new StringBuilder()
                            flag << '  ' << (f.aliases ? "$f.aliases, " : '    ')
                            if (f.type == 'boolean') {
                                flag << "[--${f.name}]".padRight(40)
                            } else {
                                flag << "[--${f.name}=${f.banner}]".padRight(40)
                            }
                            if (f.description.contains('\n')) {
                                f.description.split('\n').eachWithIndex { String d, int index ->
                                    index == 0 ? flag << '# ' + d + '\n' : flag << ('# '.padLeft(48) + d)
                                }
                            }
                            else {
                                flag << '# ' + f.description
                            }

                            console.println(flag)
                        }
                    }

                    if (command.description.examples) {
                        console.println()
                        console.addStatus('Examples:')
                        console.println(command.description.examples)
                    }

                    return true
                }
            }
            console.error "Help for command $helpCommandName not found"
            return false
        }

        boolean showAll = commandLine.hasOption('all')
        allCommands = findCommands(showAll)
        console.log '''
Usage (optionals marked with *):'
grace [environment]* [target] [arguments]*'

'''
        console.addStatus('Examples:')
        console.log('$ grace create-app blog')
        console.log('$ grace dev run-app')
        console.log ''
        console.addStatus('Available Commands (type grace help \'command-name\' for more info):')
        console.addStatus("${'Command Name'.padRight(37)} Command Description")
        console.println('-' * 100)
        for (Command command : allCommands) {
            StringBuilder description = new StringBuilder()
            description.append("${command.fullName.padRight(40)}${command.description.description}")
            appendMessage(description, command.isDeprecated(), '[deprecated]')
            console.println description
        }
        console.println()
        console.addStatus('Detailed usage with help [command]')
        true
    }

    @Override
    int complete(String buffer, int cursor, List<CharSequence> candidates) {
        List<Command> allCommands = findCommands(false)

        for (Command cmd in allCommands) {
            if (buffer) {
                if (cmd.fullName.startsWith(buffer)) {
                    candidates << cmd.name.substring(buffer.size())
                }
            }
            else {
                candidates << cmd.fullName
            }
        }
        cursor
    }

    private Collection<Command> findCommands(boolean showAll) {
        Iterable<Command> commands
        if (profile) {
            commands = profile.getCommands(projectContext)
        }
        else {
            commands = CommandRegistry.findCommands(profileRepository).findAll { Command cmd ->
                projectContext == null ? (cmd instanceof GlobalCommand || cmd !instanceof ProjectCommand) : (cmd instanceof ProjectCommand)
            }
        }
        if (showAll) {
            return commands.findAll()
                    .unique { Command cmd -> cmd.fullName }
                    .sort(false) { Command cmd -> cmd.fullName }
        }
        else {
            return commands.findAll { Command command -> command.visible }
                    .unique { Command cmd -> cmd.fullName }
                    .sort(false) { Command cmd -> cmd.fullName }
        }
    }

    private void appendMessage(StringBuilder result, boolean append, String message) {
        if (append) {
            if (result.length() > 0) {
                result.append(' ')
            }
            result.append(message)
        }
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
