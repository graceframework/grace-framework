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
package org.grails.core.artefact.gsp

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification

import grails.core.ArtefactHandler

/**
 * Test Specification for GroovyPageArtefactHandler
 *
 * @author Michael Yan
 * @since 2024.2.0
 */
class GroovyPageArtefactHandlerSpec extends Specification {

    void "Check index.gsp within 'app/views'"() {
        given:
        ArtefactHandler handler = new GroovyPageArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
import org.grails.gsp.GroovyPage;

class app_views_index_gsp extends GroovyPage {
    public String getGroovyPageFileName() {
        return "/app/views/index.gsp";
    }

    public Object run() {
        return null;
    }
}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grace', 'grace-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grace', 'grace-demo-project', 'app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grace', 'grace-demo-project', 'app', 'views', 'index.gsp'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        handler.isArtefact(classNode)
    }

    void "Check index.jsp within 'app/views'"() {
        given:
        ArtefactHandler handler = new GroovyPageArtefactHandler()
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class<?> clazz = gcl.parseClass('''
class app_views_index_jsp {

}
''')

        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', ['', 'Users', 'grace', 'grace-demo-project'].join(File.separator))
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', ['', 'Users', 'grace', 'grace-demo-project', 'app'].join(File.separator))
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> ['', 'Users', 'grace', 'grace-demo-project', 'app', 'views', 'index.jsp'].join(File.separator)

        ClassNode classNode = new ClassNode(clazz)
        classNode.setModule(moduleNode)

        expect:
        !handler.isArtefact(classNode)
    }

}
