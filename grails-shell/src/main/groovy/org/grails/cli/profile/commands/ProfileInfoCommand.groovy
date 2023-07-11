/*
 * Copyright 2015-2023 the original author or authors.
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
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware
import org.grails.cli.profile.ProjectContext
import org.grails.cli.profile.ProjectContextAware

/**
 * A command to find out information about the given profile
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class ProfileInfoCommand extends ArgumentCompletingCommand implements ProjectContextAware, ProfileRepositoryAware {

    public static final String NAME = 'profile-info'

    final String name = NAME
    final CommandDescription description = new CommandDescription(name, 'Display information about a given profile')

    ProjectContext projectContext
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

        def profileName = executionContext.commandLine.remainingArgs[0]

        Profile profile = profileRepository.getProfile(profileName)
        if (profile == null) {
            console.error("Profile not found for name [$profileName]")
        }
        else {
            console.log('-' * 100)
            console.log("Profile: ${profile.name} v${profile.version}")
            console.log('-' * 100)
            console.log(profile.description)
            console.log('')
            console.log('Provided Commands:')
            console.log('-' * 18)
            Iterable<Command> commands = profile.getCommands(projectContext).sort { Command c -> c.name }.toUnique { Command c -> c.name }

            for (cmd in commands) {
                CommandDescription description = cmd.description
                console.log("* ${description.name.padRight(30)} ${description.description}")
            }
            console.log('')
            console.log('Provided Features:')
            console.log('-' * 18)
            Iterable<Feature> features = profile.features.sort { Feature f -> f.name }

            for (feature in features) {
                console.log("* ${feature.name.padRight(30)} ${feature.description}")
            }
        }

        true
    }

}
