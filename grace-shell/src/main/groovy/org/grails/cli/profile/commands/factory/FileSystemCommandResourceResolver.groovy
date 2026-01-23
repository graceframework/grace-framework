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

import groovy.transform.CompileStatic
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

import org.grails.cli.command.factory.CommandResourceResolver
import org.grails.cli.profile.Profile
import org.grails.io.support.StaticResourceLoader

/**
 * A {@link CommandResourceResolver} that resolves from the file system
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class FileSystemCommandResourceResolver implements ProfileCommandResourceResolver {

    protected final Collection<String> matchingFileExtensions
    protected Profile profile

    FileSystemCommandResourceResolver(Collection<String> matchingFileExtensions) {
        this.matchingFileExtensions = matchingFileExtensions
    }

    @Override
    Collection<Resource> findCommandResources() {
        Resource commandsDir = getCommandsDirectory()
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(new StaticResourceLoader(commandsDir))
        if (commandsDir.exists()) {
            Collection<Resource> commandFiles = []
            for (ext in this.matchingFileExtensions) {
                commandFiles.addAll resolver.getResources("*.$ext")
            }
            commandFiles = commandFiles.sort(false) { Resource file -> file.filename }
            return commandFiles
        }
        []
    }

    @Override
    Collection<String> getMatchingFileExtensions() {
        return this.matchingFileExtensions
    }

    protected Resource getCommandsDirectory() {
        this.profile.profileDir.createRelative('commands/')
    }

    @Override
    void setProfile(Profile profile) {
        this.profile = profile
    }

}
