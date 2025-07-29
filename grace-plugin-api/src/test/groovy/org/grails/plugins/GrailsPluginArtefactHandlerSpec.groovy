/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.plugins

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.core.ArtefactHandler

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class GrailsPluginArtefactHandlerSpec extends Specification {

    void "Check FooGrailsPlugin within 'grails-app/plugins'"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class FooGrailsPlugin {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/plugins/org/grails/demo/FooGrailsPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check FooGrailsPlugin within 'app/plugins'"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class FooGrailsPlugin {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/app/plugins/org/grails/demo/FooGrailsPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check FooGrailsPlugin within 'grails-app/abc'"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class FooGrailsPlugin {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/abc/org/grails/demo/FooGrailsPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestPlugin within 'grails-app/plugins' but without suffix 'GrailsPlugin'"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestPlugin {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/plugins/org/grails/demo/TestPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check FooGrailsPlugin within 'grails-app/abc' and annotated with @Artefact('GrailsPlugin')"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("GrailsPlugin")
class FooGrailsPlugin {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/abc/org/grails/demo/FooGrailsPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check FooGrailsPlugin within 'grails-app/plugins' but not allow abstract"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("GrailsPlugin")
abstract class FooGrailsPlugin {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/plugins/org/grails/demo/FooGrailsPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check FooGrailsPlugin with @Artefact('GrailsPlugin')"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("GrailsPlugin")
class FooGrailsPlugin {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check FooGrailsPlugin without @Artefact('GrailsPlugin')"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class FooGrailsPlugin {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check FooGrailsPlugin with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Plugin")
class FooGrailsPlugin {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestPlugin without 'GrailsPlugin' suffix"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("GrailsPlugin")
class TestPlugin {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check FooGrailsPlugin with @Artefact('GrailsPlugin') but not allow abstract"() {
        given:
        ArtefactHandler handler = new GrailsPluginArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("GrailsPlugin")
abstract class FooGrailsPlugin {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
