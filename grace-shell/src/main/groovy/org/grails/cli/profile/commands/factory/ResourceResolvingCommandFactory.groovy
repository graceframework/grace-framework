/*
 * Copyright 2014-2026 the original author or authors.
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
package org.grails.cli.profile.commands.factory

import java.util.regex.Pattern

import groovy.transform.CompileStatic
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

import grails.util.BuildSettings

import org.grails.cli.command.Command
import org.grails.cli.command.factory.ClasspathCommandResourceResolver
import org.grails.cli.command.factory.CommandFactory
import org.grails.cli.command.factory.CommandResourceResolver
import org.grails.cli.profile.Profile

/**
 * A abstract {@link CommandFactory} that reads from the file system
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
abstract class ResourceResolvingCommandFactory<T> implements ProfileCommandFactory {

    Profile profile
    boolean inherited

    @Override
    Collection<Command> findCommands() {
        Collection<Resource> resources = findCommandResources(this.profile, this.inherited)
        Collection<Command> commands = []
        for (Resource resource in resources) {
            String commandName = evaluateFileName(resource.filename)
            T data = readCommandFile(resource)

            Command command = createCommand(this.profile, commandName, resource, data)
            if (command) {
                commands << command
            }
        }
        commands
    }

    protected String evaluateFileName(String fileName) {
        fileName - Pattern.compile(/\.(${getMatchingFileExtensions().join('|')})$/)
    }

    protected Collection<Resource> findCommandResources(Profile profile, boolean inherited) {
        Collection<Resource> allResources = []
        for (CommandResourceResolver resolver in getCommandResolvers(profile, inherited)) {
            allResources.addAll resolver.findCommandResources()
        }
        allResources
    }

    protected Collection<CommandResourceResolver> getCommandResolvers(Profile profile, boolean inherited) {
        Collection<CommandResourceResolver> commandResolvers = []

        FileSystemCommandResourceResolver profileCommandsResolver = new FileSystemCommandResourceResolver(matchingFileExtensions)
        profileCommandsResolver.profile = profile

        if (inherited) {
            commandResolvers.add(profileCommandsResolver)
            return commandResolvers
        }

        FileSystemCommandResourceResolver localCommandsResolver1 = new FileSystemCommandResourceResolver(matchingFileExtensions) {

            @Override
            protected Resource getCommandsDirectory() {
                new FileSystemResource("${BuildSettings.BASE_DIR}/src/main/scripts/")
            }

        }
        localCommandsResolver1.profile = profile

        FileSystemCommandResourceResolver localCommandsResolver2 = new FileSystemCommandResourceResolver(matchingFileExtensions) {

            @Override
            protected Resource getCommandsDirectory() {
                new FileSystemResource("${BuildSettings.BASE_DIR}/commands/")
            }

        }
        localCommandsResolver2.profile = profile

        commandResolvers.add(profileCommandsResolver)
        commandResolvers.add(localCommandsResolver1)
        commandResolvers.add(localCommandsResolver2)
        commandResolvers.add(new ClasspathCommandResourceResolver(matchingFileExtensions))
        commandResolvers
    }

    protected abstract T readCommandFile(Resource resource)

    protected abstract Command createCommand(Profile profile, String commandName, Resource resource, T data)

    protected abstract Collection<String> getMatchingFileExtensions()

}
