/*
 * Copyright 2014-2024 the original author or authors.
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

import groovy.transform.CompileStatic

import org.grails.cli.GrailsCli
import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileCommand
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.ProjectCommand
import org.grails.cli.profile.commands.factory.CommandFactory
import org.grails.config.CodeGenConfig

/**
 * Registry of available commands
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class CommandRegistry {

    private static final Map<String, Command> REGISTERED_COMMANDS = [:]
    private static final List<CommandFactory> REGISTERED_COMMAND_FACTORIES = []

    static {
        Iterator<Command> commands = ServiceLoader.load(Command).iterator()

        while (commands.hasNext()) {
            Command command = commands.next()
            REGISTERED_COMMANDS[command.fullName] = command
        }

        Iterator<CommandFactory> commandFactories = ServiceLoader.load(CommandFactory).iterator()
        while (commandFactories.hasNext()) {
            CommandFactory commandFactory = commandFactories.next()

            REGISTERED_COMMAND_FACTORIES << commandFactory
        }
    }

    /**
     * Returns a command for the given name and repository
     *
     * @param name The command name
     * @param repository The {@link ProfileRepository} instance
     * @return A command or null of non exists
     */
    static Command getCommand(String name, ProfileRepository repository) {
        Command command = REGISTERED_COMMANDS[name]
        if (command instanceof ProfileRepositoryAware) {
            command.profileRepository = repository
        }
        command
    }

    static Collection<Command> findCommands(ProfileRepository repository) {
        REGISTERED_COMMANDS.values().collect { Command cmd ->
            if (cmd instanceof ProfileRepositoryAware) {
                ((ProfileRepositoryAware) cmd).profileRepository = repository
            }
            cmd
        }
    }

    static Collection<Command> findCommands(Profile profile, boolean inherited = false) {
        Collection<Command> commands = []

        for (CommandFactory cf in REGISTERED_COMMAND_FACTORIES) {
            Collection<Command> factoryCommands = cf.findCommands(profile, inherited)
            Closure<?> condition = { Command c -> c.name == 'events' }
            Collection<Command> eventCommands = factoryCommands.findAll(condition)
            for (ec in eventCommands) {
                ec.handle(new GrailsCli.ExecutionContextImpl(new CodeGenConfig(profile.configuration)))
            }
            factoryCommands.removeAll(condition)
            commands.addAll factoryCommands
        }

        commands.addAll(REGISTERED_COMMANDS.values()
                .findAll { Command c ->
                    (c instanceof ProjectCommand) ||
                            (c instanceof ProfileCommand) &&
                            ((ProfileCommand) c).profile == profile
                }
        )
        commands
    }

}
