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
package grails.ui.script

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.springframework.context.ConfigurableApplicationContext

import grails.build.logging.GrailsConsole
import grails.config.Config
import grails.core.GrailsApplication
import grails.persistence.support.PersistenceContextInterceptor
import grails.ui.support.DevelopmentGrails
import grails.util.BuildSettings

/**
 * Used to run Grails scripts within the context of a Grails application
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsApplicationScriptRunner extends DevelopmentGrails {

    static GrailsConsole console = GrailsConsole.getInstance()

    List<File> scripts

    private GrailsApplicationScriptRunner(List<File> scripts, Class<?>... sources) {
        super(sources)
        this.scripts = scripts
    }

    @Override
    ConfigurableApplicationContext run(String... args) {
        ConfigurableApplicationContext ctx = super.run(args)

        Binding binding = new Binding()
        binding.setVariable('ctx', ctx)

        Config config = ctx.getBean('grailsApplication', GrailsApplication).config
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

        Collection<PersistenceContextInterceptor> interceptors = ctx.getBeansOfType(PersistenceContextInterceptor).values()

        try {
            for (File script in scripts) {
                try {
                    console.addStatus("Script :$script.name")
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
                ctx?.close()
            }
            catch (Throwable ignored) {
            }
        }

        ctx
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
            }
            catch (Throwable ignored) {
                console.error('Application class not found')
                System.exit(0)
            }
            String[] scriptNames = args.init() as String[]
            List<File> scripts = []
            scriptNames.each { String scriptName ->
                File script = new File(BuildSettings.GRAILS_APP_DIR, "scripts/${scriptName}.groovy")
                if (script.exists()) {
                    scripts.add(script)
                }
                else {
                    console.error("Specified script [${scriptName}] not found")
                    System.exit(0)
                }
            }

            new GrailsApplicationScriptRunner(scripts, applicationClass).run(args)
        }
        else {
            console.error('Missing application class name and script name arguments')
            System.exit(0)
        }
    }

}
