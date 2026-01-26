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
package grails.ui.command

import groovy.transform.CompileStatic
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext

import grails.boot.Grails
import grails.build.logging.GrailsConsole
import grails.cli.command.ApplicationCommand
import grails.cli.command.ApplicationContextCommandRegistry
import grails.cli.command.ExecutionContext
import grails.config.Settings
import grails.persistence.support.PersistenceContextInterceptor

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsApplicationCommandRunner implements ApplicationRunner, ApplicationContextAware {

    static GrailsConsole console = GrailsConsole.getInstance()

    ConfigurableApplicationContext applicationContext

    @Override
    void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext
    }

    @Override
    void run(ApplicationArguments args) throws Exception {
        String commandName = args.getSourceArgs()[0]
        ApplicationCommand command = ApplicationContextCommandRegistry.findCommand(commandName)

        if (command) {
            Object skipBootstrap = command.hasProperty('skipBootstrap')?.getProperty(command)
            if (skipBootstrap instanceof Boolean && !System.getProperty(Settings.SETTING_SKIP_BOOTSTRAP)) {
                System.setProperty(Settings.SETTING_SKIP_BOOTSTRAP, skipBootstrap.toString())
            }

            PersistenceContextInterceptor interceptor = null
            String[] beanNames = this.applicationContext.getBeanNamesForType(PersistenceContextInterceptor.class)

            if (beanNames.length > 0) {
                interceptor = (PersistenceContextInterceptor) this.applicationContext.getBean(beanNames[0])
            }

            if (interceptor != null) {
                interceptor.init()
            }

            try {
                console.addStatus("Command :$command.name")
                CommandLine commandLine = new CommandLineParser().parse(args.getSourceArgs())
                ExecutionContext executionContext = new ExecutionContext(commandLine)
                this.applicationContext.autowireCapableBeanFactory.autowireBeanProperties(command, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)
                command.applicationContext = this.applicationContext
                command.executionContext = executionContext

                boolean result = command.handle(executionContext)
                result ? console.addStatus('EXECUTE SUCCESSFUL') : console.error('EXECUTE FAILED', '')

                if (interceptor != null) {
                    interceptor.flush()
                }
            }
            catch (Throwable e) {
                console.error("Command execution error: $e.message")
            }
            finally {
                if (interceptor != null) {
                    interceptor.destroy()
                }
            }
        }
        else {
            console.error("Command not found for name: $commandName")
        }
        this.applicationContext.close()
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Command name, the last argument is the Application class name
     */
    static void main(String[] args) {
        if (args.size() > 1) {
            Class<?> applicationClass
            try {
                applicationClass = Thread.currentThread().contextClassLoader.loadClass(args.last())
                Grails grails = new Grails(applicationClass, GrailsApplicationCommandRunner.class)
                grails.run(args.init() as String[])
            }
            catch (Throwable ignore) {
                console.error('Application class not found')
                System.exit(0)
            }
        }
        else {
            console.error('Missing application class name and script name arguments')
        }
    }

}
