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
package grails.ui.shell

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.apache.groovy.groovysh.Groovysh
import org.apache.groovy.groovysh.InteractiveShellRunner
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.tools.shell.IO
import org.springframework.boot.ApplicationContextFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ResourceLoader
import org.springframework.util.ClassUtils

import grails.binding.GroovyShellBindingCustomizer
import grails.boot.Grails
import grails.core.GrailsApplication
import grails.ui.shell.support.GroovyshApplicationContext
import grails.ui.shell.support.GroovyshWebApplicationContext

/**
 * A Shell
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@InheritConstructors
class GrailsShell extends Grails {

    static final String[] BANNER = [
            '  _____                         ______       ____',
            ' / ___/______  ___ _  ____ __  / __/ /  ___ / / /',
            '/ (_ / __/ _ \\/ _ \\ |/ / // / _\\ \\/ _ \\/ -_) / /',
            '\\___/_/  \\___/\\___/___/\\_, / /___/_//_/\\__/_/_/',
            '                      /___/'
    ]

    GrailsShell(Class<?>... sources) {
        super(sources)
        configureApplicationContextClass()
    }

    GrailsShell(ResourceLoader resourceLoader, Class<?>... sources) {
        super(resourceLoader, sources)
        configureApplicationContextClass()
    }

    private void configureApplicationContextClass() {
        if (ClassUtils.isPresent('jakarta.servlet.ServletContext', Thread.currentThread().contextClassLoader)) {
            setApplicationContextFactory(ApplicationContextFactory.ofContextClass(GroovyshWebApplicationContext))
        }
        else {
            setApplicationContextFactory(ApplicationContextFactory.ofContextClass(GroovyshApplicationContext))
        }
    }

    @Override
    ConfigurableApplicationContext run(String... args) {
        ConfigurableApplicationContext applicationContext = super.run(args)

        startConsole(applicationContext)

        applicationContext
    }

    protected void startConsole(ConfigurableApplicationContext context) {
        GrailsApplication grailsApplication = context.getBean(GrailsApplication)
        Collection<GroovyShellBindingCustomizer> bindingCustomizers = context.getBeansOfType(GroovyShellBindingCustomizer).values()
        Set<String> packageNames = (getAllSources() as Set<Class>)*.package.name as Set<String>

        Binding binding = new Binding()
        binding.setVariable('app', this)
        binding.setVariable('ctx', context)
        binding.setVariable(GrailsApplication.APPLICATION_ID, grailsApplication)
        bindingCustomizers?.each { customizer -> customizer.customize(binding) }

        Groovysh groovysh = new Groovysh(binding, new IO()) {
            CompilerConfiguration configuration = CompilerConfiguration.DEFAULT

            @Override
            void displayWelcomeBanner(InteractiveShellRunner runner) {
                io.out.println()

                for (String line : BANNER) {
                    io.out.println(String.format('@|green  %s|@', line))
                }

                io.out.println('-' * (95 - 1))
                io.out.println(String.format('Groovy: %s, JVM: %s',
                        GroovySystem.version,
                        System.properties['java.version']))
                io.out.println("Type '@|bold :help|@' or '@|bold :h|@' for help.")
                io.out.println('-' * (95 - 1))
            }

        }
        groovysh.historyFull = true
        groovysh.imports.addAll(packageNames.collect({ it + '.*' }).toList())
        groovysh.run('')
    }

    /**
     * Static helper that can be used to run a {@link Grails} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    static ConfigurableApplicationContext run(Class<?> source, String... args) {
        run([source] as Class[], args)
    }

    /**
     * Static helper that can be used to run a {@link Grails} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    static ConfigurableApplicationContext run(Class<?>[] sources, String[] args) {
        new GrailsShell(sources).run(args)
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Application class name
     */
    static void main(String[] args) {
        if (args) {
            Class<?> applicationClass = Thread.currentThread().contextClassLoader.loadClass(args[0])
            GrailsShell.run(applicationClass, args)
        }
        else {
            System.err.println('Missing application class name argument')
        }
    }

}
