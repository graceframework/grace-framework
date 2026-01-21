/*
 * Copyright 2016-2026 the original author or authors.
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
package grails.cli.commands

import grails.cli.commands.io.FileSystemInteraction
import grails.cli.commands.io.FileSystemInteractionImpl
import grails.cli.commands.template.TemplateRenderer
import grails.cli.commands.template.TemplateRendererImpl
import grails.codegen.model.ModelBuilder

trait GrailsApplicationCommand implements ApplicationCommand, ModelBuilder {

    @Delegate
    TemplateRenderer templateRenderer

    @Delegate
    FileSystemInteraction fileSystemInteraction

    ExecutionContext executionContext

    boolean handle(ExecutionContext executionContext) {
        this.executionContext = executionContext
        this.templateRenderer = new TemplateRendererImpl(executionContext.baseDir)
        this.fileSystemInteraction = new FileSystemInteractionImpl(executionContext.baseDir)
        handle()
    }

    List<String> getArgs() {
        executionContext.commandLine.remainingArgs
    }

    abstract boolean handle()

}
