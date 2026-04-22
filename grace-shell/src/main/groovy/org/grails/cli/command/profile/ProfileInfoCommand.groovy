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
package org.grails.cli.command.profile

import groovy.transform.CompileStatic

import grails.build.logging.GrailsConsole

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.ArgumentCompletingCommand
import org.grails.cli.command.Command
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.GlobalCommand
import org.grails.cli.command.ProjectCommand
import org.grails.cli.profile.Feature
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProfileRepositoryAware

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT

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
    static final String USAGE = 'grace profile-info'
    static final String EXAMPLES = '''
    # Display information about a given profile
        $ grace profile-info web
'''

    final String name = NAME
    final CommandDescription description = new CommandDescription(name, 'Display information about a given profile', USAGE, EXAMPLES)

    ProfileRepository profileRepository

    ProfileInfoCommand() {
        description.argument(name: 'Profile Name', description: 'The name or coordinates of the profile', required: true)
        description.flag(name: 'only', type: 'boolean', description: "Only show the commands and features of this profile", required: false)
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: "Print the options and usage", required: false)
    }

    @Override
    void setProfileRepository(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        if (commandLine.hasOption(CommandLine.HELP_ARGUMENT) || commandLine.hasOption('h')) {
            printCommandHelp(console)
            return true
        }
        if (profileRepository == null) {
            console.error('No profile repository provided')
            return false
        }

        boolean showAll = !executionContext.commandLine.hasOption('only')
        def profileName = executionContext.commandLine.remainingArgs[0]

        Profile profile = profileRepository.getProfile(profileName)
        if (profile == null) {
            console.error("Profile not found for name [$profileName]")
        }
        else {
            console.log("Profile: ${profile.name}")
            console.log("Version: ${profile.version}")
            console.log("Extends: ${profile.extends*.name.join(', ')}")
            console.log("Description: ${profile.description}")
            console.log('')
            console.log('Provided Commands:')
            console.log('-' * 100)
            console.log("${'Name'.padRight(30)} Description")
            console.log('-' * 100)
            Iterable<Command> commands = getCommands(profile, showAll).sort { Command c -> c.name }

            for (Command cmd in commands) {
                StringBuilder description = new StringBuilder()
                description.append("${cmd.description.name.padRight(30)} ${cmd.description.description}")
                appendMessage(description, cmd.isDeprecated(), '[deprecated]')
                console.log(description.toString())
            }
            console.log('')
            console.log('Provided Features:')
            console.log('-' * 100)
            console.log("${'Name'.padRight(30)} Required  Defaults  Description")
            console.log('-' * 100)
            Iterable<Feature> features = getFeatures(profile, showAll).sort { Feature f -> f.name }
            List<Feature> requiredFeatures = getRequiredFeatures(profile, showAll)
            List<Feature> defaultFeatures = getDefaultFeatures(profile, showAll)
            for (Feature feature in features) {
                boolean isRequired = requiredFeatures.contains(feature)
                boolean isDefault = defaultFeatures.contains(feature)
                console.log(feature.name.padRight(31) +
                        (isRequired ? '  *' : '   ').padRight(10) +
                        (isDefault ? '  +' : '   ').padRight(10) +
                        feature.description)
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

    private static Iterable<Command> getCommands(Profile profile, boolean includeParents) {
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

    private static Iterable<Feature> getFeatures(Profile profile, boolean includeParents) {
        Set<Feature> allFeatures = []
        allFeatures.addAll(profile.internalFeatures)
        if (includeParents) {
            Iterable<Profile> parents = profile.extends
            for (Profile p in parents) {
                allFeatures.addAll(profile.features)
            }
        }
        allFeatures
    }

    private static List<Feature> getRequiredFeatures(Profile profile, boolean includeParents) {
        List<Feature> requiredFeatures = new ArrayList<>()
        requiredFeatures.addAll(profile.requiredFeatures)
        if (includeParents) {
            Iterable<Profile> parents = profile.extends
            for (p in parents) {
                requiredFeatures.addAll(p.requiredFeatures)
            }
        }
        requiredFeatures
    }

    private static List<Feature> getDefaultFeatures(Profile profile, boolean includeParents) {
        List<Feature> defaultFeatures = new ArrayList<>()
        defaultFeatures.addAll(profile.defaultFeatures)
        if (includeParents) {
            Iterable<Profile> parents = profile.extends
            for (p in parents) {
                defaultFeatures.addAll(p.defaultFeatures)
            }
        }
        defaultFeatures
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
