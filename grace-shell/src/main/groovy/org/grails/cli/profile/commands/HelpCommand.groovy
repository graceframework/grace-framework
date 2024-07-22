/*
 * Copyright 2015-2024 the original author or authors.
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
package org.grails.cli.profile.commands

import jline.console.completer.Completer

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.grails.cli.profile.Command
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileCommand
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.ProjectCommand
import org.grails.cli.profile.ProjectContext
import org.grails.cli.profile.ProjectContextAware

/**
 * {@code 'help'} command.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
class HelpCommand implements ProfileCommand, Completer, ProjectContextAware, ProfileRepositoryAware {

    public static final String NAME = 'help'

    CommandDescription description = new CommandDescription(NAME,
            'Prints help information for a specific command',
            'help [COMMAND NAME]')

    Profile profile
    ProfileRepository profileRepository
    ProjectContext projectContext

    private CommandLineParser cliParser = new CommandLineParser()

    HelpCommand() {
        this.description.flag(name: 'all', description: 'Show all commands', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        Collection<Command> allCommands
        String remainingArgs = commandLine.getRemainingArgsString()
        if (remainingArgs?.trim()) {
            allCommands = findCommands(true)
            CommandLine remainingArgsCommand = cliParser.parseString(remainingArgs)
            String helpCommandName = remainingArgsCommand.getCommandName()
            for (Command command : allCommands) {
                if (command.fullName == helpCommandName) {
                    console.addStatus("Command: $command.fullName")
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
                        console.addStatus('Flags:')
                        for (arg in command.description.flags) {
                            console.println "* ${arg.name} - ${arg.description ?: ''}"
                        }
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
            console.println "${command.fullName.padRight(40)}${command.description.description}"
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
                !(cmd instanceof ProjectCommand)
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

}
