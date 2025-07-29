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
class DomainClassArtefactHandlerSpec extends Specification {

    void "Check TestEntity annotated with Grails '@Entity'"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
}
''')

        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestEntity annotated with GORM '@Entity'"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.gorm.annotation.Entity
class TestEntity {
}
''')

        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestEntity annotated with JPA '@Entity'"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@jakarta.persistence.Entity
class TestEntity {
}
''')

        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestEntity within 'grails-app/domain'"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestEntity {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/domain/org/grails/demo/TestEntity.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestEntity within 'app/domain'"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestEntity {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/app/domain/org/grails/demo/TestEntity.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestEntity within 'grails-app/models'"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestEntity {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/models/org/grails/demo/TestEntity.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

    void "Check TestEntity within 'grails-app/models' and annotated with @Artefact('Domain')"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class TestEntity {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/rest/org/grails/demo/TestEntity.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestEntity within 'grails-app/domain' and allow abstract"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
abstract class TestEntity {
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/domain/org/grails/demo/TestEntity.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check TestEntity with @Artefact('Domain')"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class TestEntity {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check TestEntity without @Artefact('Domain')"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class TestEntity {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestEntity with @Artefact but wrong type"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("DomainClass")
class TestEntity {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        !handler.isArtefact(classNode)
        !handler.isArtefact(clazz)
    }

    void "Check TestEntity with @Artefact('Domain') and allow abstract"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
abstract class TestEntity {
}
''')
        ClassNode classNode = new ClassNode(clazz)

        expect:
        handler.isArtefact(classNode)
        handler.isArtefact(clazz)
    }

    void "Check FooGrailsPlugin within 'src/groovy' is not a Domain Class"() {
        given:
        ArtefactHandler handler = new DomainClassArtefactHandler()
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
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/src/main/groovy/org/grails/demo/FooGrailsPlugin.groovy'

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

}
