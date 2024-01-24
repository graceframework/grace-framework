/*
 * Copyright 2014-2023 the original author or authors.
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
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware

/**
 * Lists the available {@link org.grails.cli.profile.Profile} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ListProfilesCommand implements Command, ProfileRepositoryAware {

    final String name = 'list-profiles'
    final CommandDescription description = new CommandDescription(name, 'Lists the available profiles', 'grace list-profiles')

    ProfileRepository profileRepository

    @Override
    boolean handle(ExecutionContext executionContext) {
        List<Profile> allProfiles = profileRepository.allProfiles.sort { Profile p -> p.name}
        GrailsConsole console = executionContext.console
        console.log('-' * 100)
        console.log('Available Profiles')
        console.log('-' * 100)
        for (Profile p in allProfiles) {
            console.log("* ${p.name.padRight(30)} ${p.version.padRight(20)}  ${p.description}")
        }

        true
    }

}
