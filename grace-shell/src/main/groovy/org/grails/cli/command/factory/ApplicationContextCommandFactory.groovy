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
package org.grails.cli.command.factory

import grails.build.logging.GrailsConsole
import grails.cli.command.ApplicationCommand

import org.grails.cli.command.Command
import org.grails.cli.command.gradle.GradleTaskCommandAdapter
import org.grails.io.support.GrailsFactoriesLoader

/**
 * Automatically populates ApplicationContext command instances and adapts the interface to the shell
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
class ApplicationContextCommandFactory implements CommandFactory {

    @Override
    Collection<Command> findCommands() {
        try {
            ClassLoader classLoader = Thread.currentThread().contextClassLoader
            List<ApplicationCommand> registeredCommands = GrailsFactoriesLoader.loadFactories(ApplicationCommand, classLoader)

            return registeredCommands.collect { ApplicationCommand command -> new GradleTaskCommandAdapter(command) }
        }
        catch (Throwable e) {
            GrailsConsole.instance.error("Error occurred loading commands: $e.message", e)
            return []
        }
    }

}
