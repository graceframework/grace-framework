/*
 * Copyright 2015-2025 the original author or authors.
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

import grails.build.logging.GrailsConsole

import org.grails.cli.profile.Command
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Feature
import org.grails.cli.profile.GlobalCommand
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.ProjectCommand

/**
 * A command to find out information about the given profile
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
class ProfileInfoCommand extends ArgumentCompletingCommand implements GlobalCommand, ProjectCommand, ProfileRepositoryAware {

    public static final String NAME = 'profile-info'

    final String name = NAME
    final CommandDescription description = new CommandDescription(name, 'Display information about a given profile')

    ProfileRepository profileRepository

    ProfileInfoCommand() {
        description.argument(name: 'Profile Name', description: 'The name or coordinates of the profile', required: true)
    }

    void setProfileRepository(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        if (profileRepository == null) {
            console.error('No profile repository provided')
            return false
        }

        boolean showAll = executionContext.commandLine.hasOption('all')
        def profileName = executionContext.commandLine.remainingArgs[0]

        Profile profile = profileRepository.getProfile(profileName)
        if (profile == null) {
            console.error("Profile not found for name [$profileName]")
        }
        else {
            console.log("Profile: ${profile.name}")
            console.log("Version: ${profile.version}")
            console.log('-' * 80)
            console.log(profile.description)
            console.log('')
            console.log('Provided Commands:')
            console.log('-' * 80)
            Iterable<Command> commands = getCommands(profile, showAll).sort { Command c -> c.name }

            for (cmd in commands) {
                StringBuilder description = new StringBuilder()
                description.append("* ${cmd.description.name.padRight(30)} ${cmd.description.description}")
                appendMessage(description, cmd.isDeprecated(), '[deprecated]')
                console.log(description.toString())
            }
            console.log('')
            console.log('Provided Features:')
            console.log('-' * 80)
            Iterable<Feature> features = getFeatures(profile, showAll).sort { Feature f -> f.name }

            for (feature in features) {
                console.log("* ${feature.name.padRight(30)} ${feature.description}")
            }
        }

        true
    }

    private void appendMessage(StringBuilder result, boolean append, String message) {
        if (append) {
            if (result.length() > 0) {
                result.append(' ')
            }
            result.append(message)
        }
    }

    Iterable<Command> getCommands(Profile profile, boolean includeParents) {
        Set<Command> allCommands = []
        allCommands.addAll(profile.internalCommands)
        if (includeParents) {
            Iterable<Profile> parents = profile.extends
            for (Profile p in parents) {
                allCommands.addAll(p.internalCommands)
            }
        }
        allCommands
    }

    Iterable<Feature> getFeatures(Profile profile, boolean includeParents) {
        Set<Feature> allFeatures = []
        allFeatures.addAll(profile.features)
        if (includeParents) {
            Iterable<Profile> parents = profile.extends
            for (Profile p in parents) {
                allFeatures.addAll(profile.features)
            }
        }
        allFeatures
    }

}
