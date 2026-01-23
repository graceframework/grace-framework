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
package org.grails.cli.command.plugin

import groovy.transform.CompileStatic

import grails.build.logging.GrailsConsole

import org.grails.cli.command.app.CreateAppCommand
import org.grails.cli.profile.Profile

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT
import static org.grails.build.parsing.CommandLine.QUIET_ARGUMENT
import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * A command for creating a plugin
 *
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class CreatePluginCommand extends CreateAppCommand {

    public static final String NAME = 'create-plugin'
    static final String USAGE = 'grace create-plugin [NAME] [options]'
    static final String EXAMPLES = '''
    # Creates a plugin
        $ grace create-plugin com.example.myplugin
'''

    CreatePluginCommand() {
        populateDescription()
        description.flags.clear()
        description.flag(name: PACKAGE_NAME_FLAG, aliases: '-p', type: 'string', description: 'The Package name, for example \'com.example\'', banner: 'PACKAGE NAME', required: false)
        description.flag(name: PROFILE_FLAG, type: 'string', description: 'The profile to use: plugin, web-plugin, starter, default: web-plugin', banner: 'PROFILE', required: false)
        description.flag(name: FEATURES_FLAG, type: 'string', description: 'The features provided by the profile to use\nYou can use profile-info [PROFILE] to show all the features of the profile', banner: 'FEATURES', required: false)
        description.flag(name: TEMPLATE_FLAG, aliases: '-m', type: 'string', description: 'Path to some application template (can be a filesystem path or URL)\nFor example: https://github.com/grace-templates/helloworld.git', banner: 'TEMPLATE', required: false)
        description.flag(name: GRACE_VERSION_FLAG, type: 'string', description: 'Specific Grace Version', banner: 'GRACE VERSION', required: false)
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: 'Show the help message and quit', required: false)
        description.flag(name: STACKTRACE_ARGUMENT, type: 'boolean', description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, type: 'boolean', description: 'Show verbose output', required: false)
        description.flag(name: QUIET_ARGUMENT, aliases: '-q', type: 'boolean', description: 'Suppress status output', required: false)
        description.flag(name: FORCE_FLAG, aliases: '-f', type: 'boolean', description: 'Force overwrite of existing files', required: false)
        description.flag(name: INPLACE_FLAG, type: 'boolean', description: 'Used to create a plugin in the current directory')
    }

    @Override
    protected void populateDescription() {
        description.argument(name: 'Plugin Name', description: 'The name of the plugin to create.', required: false)
        description.description = 'Creates a plugin'
        description.usage = USAGE
        description.examples = EXAMPLES
    }

    @Override
    String getName() { NAME }

    @Override
    protected String getDefaultProfile() { 'web-plugin' }

    @Override
    protected boolean validateProfile(Profile profileInstance, String profileName, GrailsConsole console) {
        if (profileInstance == null) {
            console.error("Profile not found for name [$profileName]")
            return false
        }

        def pluginProfile = profileInstance.extends.find { Profile parent -> parent.name == 'plugin' }
        if (profileName != 'plugin' && pluginProfile == null) {
            console.error("No valid plugin profile found for name [$profileName]")
            return false
        }

        true
    }

}
