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
package org.grails.core.artefact.gsp

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.core.ArtefactHandler

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class TagLibArtefactHandlerSpec extends Specification {

    void "Check TestTagLib annotated with Grails '@TagLib'"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.gsp.TagLib
class TestTagLib {
}
''')

        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestTagLib within 'grails-app/taglib'"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestTagLib {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/taglib/org/grails/demo/TestTagLib.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestTagLib within 'app/taglib'"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestTagLib {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/app/taglib/org/grails/demo/TestTagLib.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestTagLib within 'grails-app/tags'"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestTagLib {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/tags/org/grails/demo/TestTagLib.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestTag within 'grails-app/taglib' but without suffix 'TagLib'"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestTag {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/taglib/org/grails/demo/TestTag.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestTagLib within 'grails-app/tags' and annotated with @Artefact('TagLib')"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
class TestTagLib {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/tags/org/grails/demo/TestTagLib.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestTagLib within 'grails-app/taglib' but not allow abstract"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
abstract class TestTagLib {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/taglib/org/grails/demo/TestTagLib.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestTagLib with @Artefact('TagLib')"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
class TestTagLib {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestTagLib without @Artefact('TagLib')"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestTagLib {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestTagLib with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLibrary")
class TestTagLib {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestTag without 'TagLib' suffix"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
class TestTag {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestTagLib with @Artefact('TagLib') but not allow abstract"() {
        given:
        ArtefactHandler handler = new TagLibArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
abstract class TestTagLib {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

}
