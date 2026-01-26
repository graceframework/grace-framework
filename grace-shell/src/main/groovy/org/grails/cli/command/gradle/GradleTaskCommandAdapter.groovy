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
package org.grails.cli.command.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import grails.cli.command.ApplicationCommand
import grails.util.GrailsNameUtils

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand
import org.grails.cli.gradle.GradleInvoker

/**
 * Adapts a {@link ApplicationCommand} command into a Gradle task execution
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GradleTaskCommandAdapter implements ProjectCommand {

    final ApplicationCommand applicationCommand

    GradleTaskCommandAdapter(ApplicationCommand command) {
        this.applicationCommand = command
    }

    @Override
    CommandDescription getDescription() {
        new CommandDescription(this.applicationCommand.name, this.applicationCommand.description)
    }

    @Override
    @CompileDynamic
    boolean handle(ExecutionContext executionContext) {
        GradleInvoker invoker = new GradleInvoker(executionContext)
        String method = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(this.applicationCommand.name)

        CommandLine commandLine = executionContext.commandLine
        if (commandLine.remainingArgs || commandLine.undeclaredOptions) {
            invoker."${method}"("-Pargs=${commandLine.remainingArgsWithOptionsString}")
        }
        else {
            invoker."${method}"()
        }

        true
    }

    @Override
    String getName() {
        this.applicationCommand.name
    }

    @Override
    boolean isVisible() {
        return false
    }

}
