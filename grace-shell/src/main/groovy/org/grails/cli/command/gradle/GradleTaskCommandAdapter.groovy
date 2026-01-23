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

import grails.util.Described
import grails.util.GrailsNameUtils
import grails.util.Named

import org.grails.cli.commands.CommandDescription
import org.grails.cli.commands.ExecutionContext
import org.grails.cli.commands.ProjectCommand
import org.grails.cli.gradle.GradleInvoker

/**
 * Adapts a {@link Named} command into a Gradle task execution
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GradleTaskCommandAdapter implements ProjectCommand {

    final Named adapted

    GradleTaskCommandAdapter(Named adapted) {
        this.adapted = adapted
    }

    @Override
    CommandDescription getDescription() {
        String description
        if (adapted instanceof Described) {
            description = ((Described) adapted).description
        }
        else {
            description = ''
        }
        new CommandDescription(adapted.name, description)
    }

    @Override
    @CompileDynamic
    boolean handle(ExecutionContext executionContext) {
        GradleInvoker invoker = new GradleInvoker(executionContext)
        String method = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(adapted.name)

        def commandLine = executionContext.commandLine
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
        adapted.name
    }

    @Override
    boolean isVisible() {
        return false
    }

}
