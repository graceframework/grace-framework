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
class ApplicationArtefactHandlerSpec extends Specification {

    void "Check TestApplication within 'grails-app/init'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/init/org/grails/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestApplication within 'app/init'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grace/grace-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grace/grace-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grace/grace-demo-project/app/init/org/grace/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestApplication within 'grails-app/boot'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestApplication within 'app/boot'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grace/grace-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grace/grace-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grace/grace-demo-project/app/boot/org/grace/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestApp within 'grails-app/boot' but without suffix 'Application'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApp {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestApp.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestApplication within 'grails-app/boot' and annotated with @Artefact('Application')"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestApplication within 'src/main'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grace/grace-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grace/grace-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grace/grace-demo-project/src/main/groovy/org/grace/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestApplication within 'grails-app/boot' but now allow abstract"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
abstract class TestApplication {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/boot/org/grails/demo/TestApplication.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestApplication with @Artefact('Application')"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class TestApplication {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestApplication without @Artefact('Application')"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestApplication {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestApplication with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("App")
class TestApplication {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestApp without suffix 'Application'"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
class TestApp {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestApplication with @Artefact('Application') but not allow abstract"() {
        given:
        ArtefactHandler handler = new ApplicationArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Application")
abstract class TestApplication {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
