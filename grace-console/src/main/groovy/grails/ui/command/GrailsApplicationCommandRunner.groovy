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
package grails.ui.command

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ConfigurableApplicationContext

import grails.config.Settings
import grails.dev.commands.ApplicationCommand
import grails.dev.commands.ApplicationContextCommandRegistry
import grails.dev.commands.ExecutionContext
import grails.ui.support.DevelopmentGrails

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsApplicationCommandRunner extends DevelopmentGrails {

    static GrailsConsole console = GrailsConsole.getInstance()

    String commandName

    protected GrailsApplicationCommandRunner(String commandName, Class<?>... sources) {
        super(sources)
        this.commandName = commandName
    }

    @Override
    ConfigurableApplicationContext run(String... args) {
        ConfigurableApplicationContext ctx = null
        ApplicationCommand command = ApplicationContextCommandRegistry.findCommand(commandName)
        if (command) {
            Object skipBootstrap = command.hasProperty('skipBootstrap')?.getProperty(command)
            if (skipBootstrap instanceof Boolean && !System.getProperty(Settings.SETTING_SKIP_BOOTSTRAP)) {
                System.setProperty(Settings.SETTING_SKIP_BOOTSTRAP, skipBootstrap.toString())
            }

            try {
                ctx = super.run(args)
            }
            catch (Throwable e) {
                console.error("Context failed to load: $e.message")
            }

            try {
                console.addStatus("Command :$command.name")
                CommandLine commandLine = new CommandLineParser().parse(args)
                ctx.autowireCapableBeanFactory.autowireBeanProperties(command, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)
                command.applicationContext = ctx
                boolean result = command.handle(new ExecutionContext(commandLine))
                result ? console.addStatus('EXECUTE SUCCESSFUL') : console.error('EXECUTE FAILED', '')
            }
            catch (Throwable e) {
                console.error("Command execution error: $e.message")
            }
            finally {
                try {
                    ctx?.close()
                }
                catch (Throwable ignored) {
                }
            }
        }
        else {
            console.error("Command not found for name: $commandName")
        }
        ctx
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Command name, the last argument is the Application class name
     */
    static void main(String[] args) {
        if (args.size() > 1) {
            Class applicationClass
            try {
                applicationClass = Thread.currentThread().contextClassLoader.loadClass(args.last())
            }
            catch (Throwable e) {
                console.error('Application class not found')
                System.exit(0)
            }

            GrailsApplicationCommandRunner runner = new GrailsApplicationCommandRunner(args[0], applicationClass)
            runner.run(args.init() as String[])
        }
        else {
            console.error('Missing application class name and script name arguments')
        }
    }

}
