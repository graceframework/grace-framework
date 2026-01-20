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
class UrlMappingsArtefactHandlerSpec extends Specification {

    void "Check TestUrlMappings within 'grails-app/controllers'"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestUrlMappings {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'grails', 'demo', 'TestUrlMappings.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestUrlMappings within 'app/controllers'"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestUrlMappings {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'app', 'controllers', 'org', 'grails', 'demo', 'TestUrlMappings.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestUrlMappings within 'grails-app/boot'"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestUrlMappings {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'boot', 'org', 'grails', 'demo', 'TestUrlMappings.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestApp within 'grails-app/controllers' but without suffix 'UrlMappings'"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestUrlMapping {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'grails', 'demo', 'TestUrlMapping.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestUrlMappings within 'grails-app/boot' and annotated with @Artefact('UrlMappings')"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("UrlMappings")
class TestUrlMappings {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'boot', 'org', 'grails', 'demo', 'TestUrlMappings.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestUrlMappings within 'grails-app/controllers' but not allow abstract"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("UrlMappings")
abstract class TestUrlMappings {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'controllers', 'org', 'grails', 'demo', 'TestUrlMappings.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestUrlMappings with @Artefact('UrlMappings')"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("UrlMappings")
class TestUrlMappings {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestUrlMappings without @Artefact('UrlMappings')"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestUrlMappings {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestUrlMappings with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("UrlMapping")
class TestUrlMappings {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestApp without suffix 'UrlMappings'"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("UrlMappings")
class TestUrlMapping {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestUrlMappings with @Artefact('UrlMappings') but not allow abstract"() {
        given:
        ArtefactHandler handler = new UrlMappingsArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("UrlMappings")
abstract class TestUrlMappings {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
