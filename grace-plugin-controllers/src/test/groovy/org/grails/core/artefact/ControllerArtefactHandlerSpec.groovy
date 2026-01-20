/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.core.artefact

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.core.ArtefactHandler

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class ControllerArtefactHandlerSpec extends Specification {

    void "Check TestController annotated with Grails '@Controller'"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.web.Controller
class TestController {
}
''')

        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestController annotated with Spring '@Controller'"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@org.springframework.stereotype.Controller
class TestController {
}
''')

        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestController within 'grails-app/controllers'"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestController {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'grails', 'demo', 'TestController.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestController within 'app/controllers'"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestController {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'app', 'controllers', 'org', 'grails', 'demo', 'TestController.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestController within 'grails-app/rest'"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestController {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'rest', 'org', 'grails', 'demo', 'TestController.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestControl within 'grails-app/controllers' but without suffix 'Controller'"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestControl {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'grails', 'demo', 'TestControl.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestController within 'grails-app/rest' and annotated with @Artefact('Controller')"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class TestController {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'rest', 'org', 'grails', 'demo', 'TestController.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestController within 'grails-app/controllers' but not allow abstract"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
abstract class TestController {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'grails', 'demo', 'TestController.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestController with @Artefact('Controller')"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class TestController {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestController without @Artefact('Controller')"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestController {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestController with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Control")
class TestController {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestControl without Controller suffix"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class TestControl {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestController with @Artefact('Controller') but not allow abstract"() {
        given:
        ArtefactHandler handler = new ControllerArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
abstract class TestController {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
