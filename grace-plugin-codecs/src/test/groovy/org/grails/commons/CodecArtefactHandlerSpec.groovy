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
package org.grails.commons

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.core.ArtefactHandler

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class CodecArtefactHandlerSpec extends Specification {

    void "Check TestCodec within 'grails-app/utils'"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestCodec {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'utils', 'org', 'grails', 'demo', 'TestCodec.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestCodec within 'app/utils'"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestCodec {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'app', 'utils', 'org', 'grails', 'demo', 'TestCodec.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestCodec within 'grails-app/abc'"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestCodec {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'abc', 'org', 'grails', 'demo', 'TestCodec.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestCode within 'grails-app/utils' but without suffix 'Codec'"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestCode {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'utils', 'org', 'grails', 'demo', 'TestCode.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestCodec within 'grails-app/abc' and annotated with @Artefact('Codec')"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Codec")
class TestCodec {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'abc', 'org', 'grails', 'demo', 'TestCodec.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestCodec within 'grails-app/init' but now allow abstract"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Codec")
abstract class TestCodec {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grails', 'grails-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grails', 'grails-demo-project', 'grails-app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grails', 'grails-demo-project', 'grails-app', 'utils', 'org', 'grails', 'demo', 'TestCodec.groovy'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestCodec with @Artefact('Codec')"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Codec")
class TestCodec {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestCodec without @Artefact('Codec')"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestCodec {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestCodec with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("App")
class TestCodec {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestApp without suffix 'Codec'"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Codec")
class TestApp {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestCodec with @Artefact('Codec') but not allow abstract"() {
        given:
        ArtefactHandler handler = new CodecArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Codec")
abstract class TestCodec {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
