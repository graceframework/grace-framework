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
package grails.ui.console

import java.util.prefs.Preferences

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.springframework.boot.ApplicationContextFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ResourceLoader
import org.springframework.util.ClassUtils

import grails.binding.GroovyShellBindingCustomizer
import grails.boot.Grails
import grails.core.GrailsApplication
import grails.persistence.support.PersistenceContextInterceptor
import grails.ui.console.support.GroovyConsoleApplicationContext
import grails.ui.console.support.GroovyConsoleWebApplicationContext
import grails.util.GrailsVersion

/**
 * The Grails console runs Grails embedded within a Swing console instead of within a container like Tomcat
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsConsole extends Grails {

    static {
        System.setProperty('java.awt.headless', 'false')
    }

    GrailsConsole(Class<?>... sources) {
        super(sources)
        configureApplicationContextClass()
    }

    GrailsConsole(ResourceLoader resourceLoader, Class<?>... sources) {
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
    ConfigurableApplicationContext run(String... args) {
        ConfigurableApplicationContext applicationContext = super.run(args)

        startConsole(applicationContext)

        applicationContext
    }

    @CompileDynamic
    protected void startConsole(ConfigurableApplicationContext context) {
        GrailsApplication grailsApplication = context.getBean(GrailsApplication)
        Collection<GroovyShellBindingCustomizer> bindingCustomizers = context.getBeansOfType(GroovyShellBindingCustomizer).values()
        Set<String> packageNames = (getAllSources() as Set<Class>)*.package.name as Set<String>
        String[] imports = packageNames.toArray(new String[0])

        Binding binding = new Binding()
        binding.setVariable('app', this)
        binding.setVariable('ctx', context)
        binding.setVariable(GrailsApplication.APPLICATION_ID, grailsApplication)
        bindingCustomizers?.each { customizer -> customizer.customize(binding) }

        CompilerConfiguration baseConfig = new CompilerConfiguration()
        baseConfig.addCompilationCustomizers(new ImportCustomizer().addStarImports(imports))

        ConfigurableApplicationContext self = context
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
        groovy.console.ui.Console groovyConsole = new groovy.console.ui.Console(classLoader, binding, baseConfig) {

            static final TITLE = 'GrailsConsole'
            static prefs = Preferences.userNodeForPackage(GrailsConsole)

            @Override
            def askToInterruptScript() {
                if (!super.scriptRunning) {
                    return true
                }
                def rc = javax.swing.JOptionPane.showConfirmDialog(super.frame,
                        "Script executing. Press 'OK' to attempt to interrupt it before exiting.",
                        TITLE, javax.swing.JOptionPane.OK_CANCEL_OPTION)
                if (rc == javax.swing.JOptionPane.OK_OPTION) {
                    super.doInterrupt()
                    return true
                }
                false
            }

            @Override
            boolean askToSaveFile() {
                if (!super.dirty) {
                    return true
                }
                switch (javax.swing.JOptionPane.showConfirmDialog(super.frame,
                        'Save changes' + (super.scriptFile != null ? " to ${super.scriptFile.name}" : '') + '?',
                        TITLE, javax.swing.JOptionPane.YES_NO_CANCEL_OPTION)) {
                    case javax.swing.JOptionPane.YES_OPTION:
                        return fileSave()
                    case javax.swing.JOptionPane.NO_OPTION:
                        return true
                    default:
                        return false
                }
            }

            @Override
            boolean exit(EventObject evt) {
                boolean exit = super.exit(evt)
                self.close()
                System.exit(0)
                exit
            }

            @Override
            void updateTitle() {
                if (super.frame.title) {
                    String title = TITLE
                    if (super.indy) {
                        title += ' (Indy)'
                    }
                    if (super.scriptFile != null) {
                        super.frame.title = super.scriptFile.name + (super.dirty ? ' * ' : '') + ' - ' + title
                    }
                    else {
                        super.frame.title = title
                    }
                }
            }

            @Override
            void showAbout(EventObject evt = null) {
                def grailsVersion = GrailsVersion.current().getVersion()
                def javaVersion = String.format('%s (%s %s)', System.getProperty('java.version'),
                        System.getProperty('java.vm.vendor'), System.getProperty('java.vm.version'))
                def osVersion = String.format('%s %s %s', System.getProperty('os.name'), System.getProperty('os.version'), System.getProperty('os.arch'))
                def groovyVersion = GroovySystem.getVersion()
                def pane = super.swing.optionPane()
                pane.setMessage('Welcome to the Grails Console for evaluating Groovy scripts' +
                        "\nGrails: $grailsVersion" +
                        "\nGroovy: $groovyVersion" +
                        "\nJVM: $javaVersion" +
                        "\nOS: $osVersion")
                def dialog = pane.createDialog(super.frame, 'About ' + TITLE)
                dialog.setVisible(true)
            }

        }

        Collection<PersistenceContextInterceptor> interceptors = context.getBeansOfType(PersistenceContextInterceptor).values()
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
        new GrailsConsole(sources).run(args)
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Application class name
     */
    static void main(String[] args) {
        if (args) {
            Class<?> applicationClass = Thread.currentThread().contextClassLoader.loadClass(args[0])
            new GrailsConsole(applicationClass).run(args)
        }
        else {
            System.err.println('Missing application class name argument')
        }
    }

}
