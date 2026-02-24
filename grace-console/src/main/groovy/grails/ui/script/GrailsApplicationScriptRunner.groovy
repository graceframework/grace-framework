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
package grails.ui.script

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext

import grails.boot.Grails
import grails.build.logging.GrailsConsole
import grails.config.Config
import grails.core.GrailsApplication
import grails.persistence.support.PersistenceContextInterceptor
import grails.util.BuildSettings

/**
 * Used to run Grails scripts within the context of a Grails application
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class GrailsApplicationScriptRunner implements ApplicationRunner, ApplicationContextAware {

    static GrailsConsole console = GrailsConsole.getInstance()

    ConfigurableApplicationContext applicationContext

    @Override
    void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext
    }

    @Override
    void run(ApplicationArguments args) throws Exception {
        String[] scriptNames = args.getSourceArgs()

        List<File> scripts = []
        scriptNames.each { String scriptName ->
            File script
            if (scriptName.startsWith('/')) {
                scriptName = scriptName.substring(1)
            }
            if (scriptName.endsWith('.groovy')) {
                scriptName = scriptName - '.groovy'
            }
            script = new File(BuildSettings.BASE_DIR, "${scriptName}.groovy")
            if (script.exists()) {
                scripts.add(script)
            }
        }

        if (scripts.isEmpty()) {
            console.error("Specified scripts [${scriptNames.join(',')}] not found")
            this.applicationContext.close()
            return
        }

        Binding binding = new Binding()
        binding.setVariable('ctx', this.applicationContext)

        Config config = this.applicationContext.getBean('grailsApplication', GrailsApplication).config
        String defaultPackageKey = 'grails.codegen.defaultPackage'
        String defaultPackageName = config.getProperty(defaultPackageKey, String)

        GroovyShell sh
        CompilerConfiguration configuration = new CompilerConfiguration()
        if (defaultPackageName) {
            ImportCustomizer importCustomizer = new ImportCustomizer()
            importCustomizer.addStarImports(defaultPackageName)
            configuration.addCompilationCustomizers(importCustomizer)
        }
        sh = new GroovyShell(binding, configuration)

        Collection<PersistenceContextInterceptor> interceptors = this.applicationContext.getBeansOfType(PersistenceContextInterceptor).values()

        try {
            String basePath = BuildSettings.BASE_DIR.canonicalPath
            for (File script in scripts) {
                try {
                    String relativePath = (script.canonicalPath - basePath).substring(1)
                    console.addStatus("Script :$relativePath")

                    for (i in interceptors) {
                        i.init()
                    }

                    sh.evaluate(script)

                    for (i in interceptors) {
                        i.destroy()
                    }
                    console.updateStatus('EXECUTE SUCCESSFUL')
                }
                catch (Throwable e) {
                    console.error("Script execution error: $e.message")
                }
            }
        }
        finally {
            try {
                for (i in interceptors) {
                    i.destroy()
                }
                this.applicationContext?.close()
            }
            catch (Throwable ignored) {
            }
        }
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The last argument is the Application class name. All other args are script names
     */
    static void main(String[] args) {
        if (args.size() > 1) {
            Class applicationClass
            try {
                applicationClass = Thread.currentThread().contextClassLoader.loadClass(args.last())
                Grails grails = new Grails(applicationClass, GrailsApplicationScriptRunner.class)
                grails.run(args.init() as String[])
            }
            catch (Throwable ignored) {
                console.error('Application class not found')
                System.exit(0)
            }
        }
        else {
            console.error('Missing application class name and script name arguments')
            System.exit(0)
        }
    }

}
