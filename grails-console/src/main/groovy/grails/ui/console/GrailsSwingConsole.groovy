/*
 * Copyright 2014-2022 the original author or authors.
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
package grails.ui.console

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationContextFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ResourceLoader
import org.springframework.util.ClassUtils

import grails.boot.Grails
import grails.core.GrailsApplication
import grails.persistence.support.PersistenceContextInterceptor
import grails.ui.console.support.GroovyConsoleApplicationContext
import grails.ui.console.support.GroovyConsoleWebApplicationContext

/**
 * The Grails console runs Grails embedded within a Swing console instead of within a container like Tomcat
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsSwingConsole extends Grails {

    static {
        System.setProperty('java.awt.headless', 'false')
    }

    GrailsSwingConsole(Class<?>... sources) {
        super(sources)
        configureApplicationContextClass()
    }

    GrailsSwingConsole(ResourceLoader resourceLoader, Class<?>... sources) {
        super(resourceLoader, sources)
        configureApplicationContextClass()
    }

    void configureApplicationContextClass() {
        if (ClassUtils.isPresent('javax.servlet.ServletContext', Thread.currentThread().contextClassLoader)) {
            setApplicationContextFactory(ApplicationContextFactory.ofContextClass(GroovyConsoleWebApplicationContext))
        }
        else {
            setApplicationContextFactory(ApplicationContextFactory.ofContextClass(GroovyConsoleApplicationContext))
        }
    }

    @Override
    protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {
        startConsole(context)
    }

    protected void startConsole(ConfigurableApplicationContext context) {
        GrailsApplication grailsApplication = context.getBean(GrailsApplication)
        Set<String> packageNames = (getAllSources() as Set<Class>)*.package.name as Set<String>
        String[] imports = packageNames.toArray(new String[0])

        Binding binding = new Binding()
        binding.setVariable('app', this)
        binding.setVariable('ctx', context)
        binding.setVariable(GrailsApplication.APPLICATION_ID, grailsApplication)

        CompilerConfiguration baseConfig = new CompilerConfiguration()
        baseConfig.addCompilationCustomizers(new ImportCustomizer().addStarImports(imports))

        ConfigurableApplicationContext self = context
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
        groovy.console.ui.Console groovyConsole = new groovy.console.ui.Console(classLoader, binding, baseConfig) {

            @Override
            boolean exit(EventObject evt) {
                boolean exit = super.exit(evt)
                self.close()
                System.exit(0)
                exit
            }

        }

        def interceptors = context.getBeansOfType(PersistenceContextInterceptor).values()
        groovyConsole.beforeExecution = {
            for (i in interceptors) {
                i.init()
            }
        }

        groovyConsole.afterExecution = {
            for (i in interceptors) {
                i.destroy()
            }
        }
        groovyConsole.run()
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
        new GrailsSwingConsole(sources).run(args)
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Application class name
     */
    static void main(String[] args) {
        if (args) {
            def applicationClass = Thread.currentThread().contextClassLoader.loadClass(args[0])
            new GrailsSwingConsole(applicationClass).run(args)
        }
        else {
            System.err.println('Missing application class name argument')
        }
    }

}
