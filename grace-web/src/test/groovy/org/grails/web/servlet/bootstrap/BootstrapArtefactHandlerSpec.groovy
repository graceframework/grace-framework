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
package org.grails.web.servlet.bootstrap

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.core.ArtefactHandler
import org.grails.web.servlet.boostrap.BootstrapArtefactHandler

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class BootstrapArtefactHandlerSpec extends Specification {

    void "Check TestBootstrap within 'grails-app/init'"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestBootstrap {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/init/org/grails/demo/TestBootstrap.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestBootstrap within 'app/init'"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestBootstrap {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grace/grace-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grace/grace-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grace/grace-demo-project/app/init/org/grace/demo/TestBootstrap.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestBootstrap within 'grails-app/boot'"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestBootstrap {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestBootstrap.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestBootstrap within 'app/boot'"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestBootstrap {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grace/grace-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grace/grace-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grace/grace-demo-project/app/boot/org/grace/demo/TestBootstrap.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestBoot within 'grails-app/init' but without suffix 'Bootstrap'"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestBoot {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/init/org/grails/demo/TestBoot.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestBootstrap within 'grails-app/boot' and annotated with @Artefact('Bootstrap')"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Bootstrap")
class TestBootstrap {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestBootstrap.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestBootstrap within 'grails-app/boot' but now allow abstract"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Bootstrap")
abstract class TestBootstrap {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestBootstrap.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestBootstrap with @Artefact('Bootstrap')"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Bootstrap")
class TestBootstrap {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestBootstrap without @Artefact('Bootstrap')"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestBootstrap {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestBootstrap with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("App")
class TestBootstrap {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestBoot without suffix 'Bootstrap'"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Bootstrap")
class TestBoot {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestBootstrap with @Artefact('Bootstrap') but not allow abstract"() {
        given:
        ArtefactHandler handler = new BootstrapArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Bootstrap")
abstract class TestBootstrap {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
