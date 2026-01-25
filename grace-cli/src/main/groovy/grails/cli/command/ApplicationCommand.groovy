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
package grails.cli.command

import org.springframework.context.ConfigurableApplicationContext

import grails.cli.core.io.FileSystemInteraction
import grails.cli.core.io.FileSystemInteractionImpl
import grails.cli.core.template.TemplateRenderer
import grails.cli.core.template.TemplateRendererImpl
import grails.codegen.model.ModelBuilder

/**
 * Represents a command that is run against the {@link org.springframework.context.ApplicationContext}
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
trait ApplicationCommand implements Command, ModelBuilder {

    private ConfigurableApplicationContext applicationContext
    private ExecutionContext executionContext

    @Delegate
    TemplateRenderer templateRenderer

    @Delegate
    FileSystemInteraction fileSystemInteraction

    /**
     * Sets the application context of the command
     *
     * @param applicationContext The application context
     */
    void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    ConfigurableApplicationContext getApplicationContext() {
        this.applicationContext
    }

    /**
     * Sets the execute context of the command
     *
     * @param executionContext The execute context
     */
    void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext
        this.templateRenderer = new TemplateRendererImpl(executionContext.baseDir)
        this.fileSystemInteraction = new FileSystemInteractionImpl(executionContext.baseDir)
    }

    ExecutionContext getExecutionContext() {
        this.executionContext
    }

    List<String> getArgs() {
        getExecutionContext().commandLine.remainingArgs
    }

    /**
     * Handles the command
     *
     * @param executionContext The execution context
     * @return True if the command was successful
     */
    @Override
    boolean handle(ExecutionContext executionContext) {
        true
    }

}
