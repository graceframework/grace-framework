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
package org.grails.compiler.injection

import java.security.CodeSource

import groovy.xml.XmlSlurper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification
import spock.lang.TempDir

import grails.plugins.metadata.GrailsPlugin

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
class GlobalGrailsPluginTransformationSpec extends Specification {

    @TempDir
    File targetDir

    def createClassLoader() {
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.setDisabledGlobalASTTransformations(['org.grails.compiler.injection.GlobalGrailsClassInjectorTransformation'] as Set<String>)
        configuration.setTargetDirectory(targetDir)

        def gcl = new TestPluginGroovyClassLoader(getClass().getClassLoader(), configuration)
        return gcl
    }

    void "Test 'META-INF/grails-plugin.xml' was generate"() {
        given:
        def gcl = createClassLoader()

        when:
        def pluginClass = gcl.parseClass('''
class FooGrailsPlugin extends grails.plugins.Plugin {
    def grailsVersion = "2022.1.2 > *"
    def version = "1.0.0"
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Grails Foo Plugin"
    def author = "Michael Yan"
    def authorEmail = "rain@rainboyan.com"
    def description = 'Grails Foo plugin'
}
''', '/Users/grails/grails-demo-project/grails-app/src/main/groovy/org/demo/FooGrailsPlugin.groovy')

        Class postClass = gcl.parseClass('''
class Post {
  String title
}
''', '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy')

        GrailsPlugin grailsPluginAnno = postClass.getAnnotation(GrailsPlugin)
        def pluginXmlFile = new File(targetDir, 'META-INF/grails-plugin.xml')

        then:
        pluginXmlFile.exists()

        when:
        def plugin = new XmlSlurper().parse(pluginXmlFile)

        then:
        plugin.@name == 'foo'
        plugin.@version == '1.0.0'
        plugin.type == 'FooGrailsPlugin'
        plugin.grailsVersion == '2022.1.2 > *'
        plugin.version == '1.0.0'
        plugin.pluginExcludes == '[grails-app/views/error.gsp]'
        plugin.title == 'Grails Foo Plugin'
        plugin.author == 'Michael Yan'
        plugin.authorEmail == 'rain@rainboyan.com'
        plugin.description == 'Grails Foo plugin'

        and:
        grailsPluginAnno
        grailsPluginAnno.name() == 'grailsDemoProject'
        grailsPluginAnno.version() == '1.0.0'
    }

}


class TestPluginGroovyClassLoader extends GroovyClassLoader {
    CompilationUnit compilationUnit

    TestPluginGroovyClassLoader() {
    }

    TestPluginGroovyClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config)
    }

    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit compilationUnit = super.createCompilationUnit(config, source)
        compilationUnit.addFirstPhaseOperation(new CompilationUnit.IPrimaryClassNodeOperation() {

            @Override
            void call(SourceUnit sourceUnit, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                sourceUnit.getAST().putNodeMetaData('PROJECT_NAME', 'grails-demo-project')
                sourceUnit.getAST().putNodeMetaData('PROJECT_VERSION', '1.0.0')
                sourceUnit.getAST().putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
                sourceUnit.getAST().putNodeMetaData('PROJECT_TYPE', 'PLUGIN')
                sourceUnit.getAST().putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
            }

        }, Phases.CANONICALIZATION)

        this.compilationUnit = compilationUnit
    }

    ClassNode getClassNode(String name) {
        this.compilationUnit.getClassNode(name)
    }

}
