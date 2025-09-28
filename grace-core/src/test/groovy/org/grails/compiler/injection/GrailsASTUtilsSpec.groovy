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
package org.grails.compiler.injection

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.TempDir

import grails.artefact.Artefact
import grails.persistence.Entity
import org.grails.core.artefact.ControllerArtefactHandler

/**
 * @author Jeff Scott Brown
 * @author Michael Yan
 * @since 3.1
 */
class GrailsASTUtilsSpec extends Specification {
    @TempDir
    File tmpDir

    @Issue('grails/grails-core#10079')
    void 'test domain class detection when the current source unit is associated with a controller'() {
        setup:
        File projectDir = new File(tmpDir, "projectDir")

        // create /projectDir/grails-app/domain/ under java.io.tmpdir
        File grailsAppDir = new File(projectDir, 'grails-app')
        File domainDir = new File(grailsAppDir, 'domain')

        String packagePath = Something.package.name.replace('.' as char, File.separatorChar)

        // create the source file that would contain the source for the
        // relevant domain class...
        File domainPackageDir = new File(domainDir, packagePath)
        domainPackageDir.mkdirs()
        File domainClassFile = new File(domainPackageDir, 'Something.groovy')
        domainClassFile.createNewFile()

        // the controller source file doesn't really need to exist but we need a
        // fully qualified path to where it would be...
        File controllersDir = new File(grailsAppDir, 'controllers')
        File controllerPackageDir = new File(controllersDir, packagePath)
        File controllerClassFile = new File(controllerPackageDir,
                                            'SomethingController.groovy')

        SourceUnit domainSourceUnit = Mock()
        ModuleNode ast = new ModuleNode(domainSourceUnit)
        ast.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        ast.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        domainSourceUnit.getAST() >> ast
        domainSourceUnit.getName() >> domainClassFile.absolutePath

        SourceUnit controllerSourceUnit = Mock()
        ModuleNode controllerAst = new ModuleNode(controllerSourceUnit)
        controllerAst.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        controllerAst.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        controllerSourceUnit.getAST() >> controllerAst
        controllerSourceUnit.getName() >> controllerClassFile.absolutePath

        expect: 'Something should be recognized as a domain because grails-app/domain/org/grails/compiler/injection/Something.groovy exists'
        GrailsASTUtils.isDomainClass(new ClassNode(Something), domainSourceUnit)

        and: 'SomethingElse should NOT be recognized as a domain because grails-app/domain/org/grails/compiler/injection/SomethingElse.groovy does NOT exist'
        !GrailsASTUtils.isDomainClass(new ClassNode(SomethingElse), controllerSourceUnit)
    }

    void 'test domain class in app/domain'() {
        setup:
        File projectDir = new File(tmpDir, "projectDir")

        // create /projectDir/app/domain/ under java.io.tmpdir
        File grailsAppDir = new File(projectDir, 'app')
        File domainDir = new File(grailsAppDir, 'domain')

        String packagePath = Something.package.name.replace('.' as char, File.separatorChar)

        // create the source file that would contain the source for the
        // relevant domain class...
        File domainPackageDir = new File(domainDir, packagePath)
        domainPackageDir.mkdirs()
        File domainSomethingFile = new File(domainPackageDir, 'Something.groovy')
        domainSomethingFile.createNewFile()
        SourceUnit domainSomethingSourceUnit = Mock()
        ModuleNode ast = new ModuleNode(domainSomethingSourceUnit)
        ast.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        ast.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        domainSomethingSourceUnit.getAST() >> ast
        domainSomethingSourceUnit.getName() >> domainSomethingFile.absolutePath

        expect: 'Something should be recognized as a domain because app/domain/org/grails/compiler/injection/Something.groovy exists'
        GrailsASTUtils.isDomainClass(new ClassNode(Something), domainSomethingSourceUnit)
    }

    void 'test domain class in app/models/'() {
        setup:
        File projectDir = new File(tmpDir, "projectDir")

        // create /projectDir/app/models/ under java.io.tmpdir
        File grailsAppDir = new File(projectDir, 'app')
        File modelsDir = new File(grailsAppDir, 'models')

        String packagePath = Something.package.name.replace('.' as char, File.separatorChar)

        // create the source file that would contain the source for the
        // relevant domain class...
        File domainPackageDir = new File(modelsDir, packagePath)
        domainPackageDir.mkdirs()
        File domainSomethingFile = new File(domainPackageDir, 'Something.groovy')
        domainSomethingFile.createNewFile()
        SourceUnit domainSomethingSourceUnit = Mock()
        ModuleNode ast = new ModuleNode(domainSomethingSourceUnit)
        ast.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        ast.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        domainSomethingSourceUnit.getAST() >> ast
        domainSomethingSourceUnit.getName() >> domainSomethingFile.absolutePath

        expect: 'Something should not be recognized as a domain because it in app/models/'
        !GrailsASTUtils.isDomainClass(new ClassNode(Something), domainSomethingSourceUnit)
    }

    void 'test domain class annotated with @grails.persistence.Entity'() {
        setup:
        File projectDir = new File(tmpDir, "projectDir")

        // create /projectDir/app/models/ under java.io.tmpdir
        File grailsAppDir = new File(projectDir, 'app')
        File modelsDir = new File(grailsAppDir, 'models')

        String packagePath = SomeEntity.package.name.replace('.' as char, File.separatorChar)

        // create the source file that would contain the source for the
        // relevant domain class...
        File modelsPackageDir = new File(modelsDir, packagePath)
        modelsPackageDir.mkdirs()
        File someEntityFile = new File(modelsPackageDir, 'SomethingElse.groovy')
        someEntityFile.createNewFile()
        SourceUnit someEntitySourceUnit = Mock()
        ModuleNode ast = new ModuleNode(someEntitySourceUnit)
        ast.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        ast.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        someEntitySourceUnit.getAST() >> ast
        someEntitySourceUnit.getName() >> someEntityFile.absolutePath

        expect: 'SomeEntity should be recognized as a domain because annotated with @grails.persistence.Entity'
        GrailsASTUtils.isDomainClass(new ClassNode(SomeEntity), someEntitySourceUnit)
    }

    void 'test domain class annotated with @jakarta.persistence.Entity'() {
        setup:
        File projectDir = new File(tmpDir, "projectDir")

        // create /projectDir/app/models/ under java.io.tmpdir
        File grailsAppDir = new File(projectDir, 'app')
        File modelsDir = new File(grailsAppDir, 'models')

        String packagePath = SomeJpaEntity.package.name.replace('.' as char, File.separatorChar)

        // create the source file that would contain the source for the
        // relevant domain class...
        File modelsPackageDir = new File(modelsDir, packagePath)
        modelsPackageDir.mkdirs()
        File someEntityFile = new File(modelsPackageDir, 'SomeJpaEntity.groovy')
        someEntityFile.createNewFile()
        SourceUnit someEntitySourceUnit = Mock()
        ModuleNode ast = new ModuleNode(someEntitySourceUnit)
        ast.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        ast.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        someEntitySourceUnit.getAST() >> ast
        someEntitySourceUnit.getName() >> someEntityFile.absolutePath

        expect: 'SomeJpaEntity should be recognized as a domain because annotated with @grails.persistence.Entity'
        GrailsASTUtils.isJpaEntityClass(new ClassNode(SomeJpaEntity))
        !GrailsASTUtils.isDomainClass(new ClassNode(SomeJpaEntity), someEntitySourceUnit)
    }

    void 'Test domain class artefact path'() {
        given:
        SourceUnit sourceUnit = Mock()
        ModuleNode moduleNode = new ModuleNode(sourceUnit)
        moduleNode.putNodeMetaData('PROJECT_DIR', '/Users/grails/grails-demo-project')
        moduleNode.putNodeMetaData('GRAILS_APP_DIR', '/Users/grails/grails-demo-project/grails-app')
        sourceUnit.getAST() >> moduleNode
        sourceUnit.getName() >> '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy'

        ClassNode classNode = Mock(ClassNode)
        classNode.getModule() >> moduleNode

        expect:
        GrailsASTUtils.getGrailsArtefactPath(classNode) == 'domain'
    }

    void 'Test Domain class artefact type'() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Domain")
class Post {
}
''', '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy')

        def classNode = gcl.getClassNode('Post')

        expect:
        GrailsASTUtils.getGrailsArtefactType(classNode) == 'Domain'
    }

    void 'Test Controller class and UrlMappings artefact type'() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class PostController {
}

@grails.artefact.Artefact("UrlMappings")
class UrlMappings {
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/UrlMappings.groovy')

        def postController = gcl.getClassNode('PostController')
        def urlMappings = gcl.getClassNode('UrlMappings')

        expect:
        GrailsASTUtils.getGrailsArtefactType(postController) == 'Controller'
        GrailsASTUtils.getGrailsArtefactType(urlMappings) == 'UrlMappings'
    }

    void 'Test TagLib class artefact type'() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        def clazz = gcl.parseClass('''
@grails.artefact.Artefact("TagLib")
class PostTagLib {
}
''', '/Users/grails/grails-demo-project/grails-app/controllers/org/demo/PostTagLib.groovy')

        def postTagLib = gcl.getClassNode('PostTagLib')

        expect:
        GrailsASTUtils.getGrailsArtefactType(postTagLib) == 'TagLib'
    }

    void 'Test not annotated class with NULL artefact type'() {
        given:
        def gcl = new GrailsAwareClassLoader(getClass().getClassLoader())
        gcl.disabledGlobalASTTransformations = true
        gcl.disabledGrailsAwareInjectionOperation = true
        def clazz = gcl.parseClass('''
class Post {
}
''', '/Users/grails/grails-demo-project/grails-app/domain/org/demo/Post.groovy')

        def classNode = gcl.getClassNode('Post')

        expect:
        !GrailsASTUtils.getGrailsArtefactType(classNode)
    }

    void 'Test Controller class artefact type from ClassNode'() {
        given:
        ClassNode classNode = ClassHelper.make(Object)
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(Artefact))
        annotationNode.addMember("value", new PropertyExpression(
                new ClassExpression(ClassHelper.make(ControllerArtefactHandler)), "TYPE"))
        classNode.addAnnotation(annotationNode)

        when:
        String artefactType = GrailsASTUtils.getGrailsArtefactType(classNode)

        then:
        artefactType == "Controller"
    }

}

class Something {}
class SomethingElse {}

@Entity
class SomeEntity {}

@jakarta.persistence.Entity
class SomeJpaEntity {
    @jakarta.persistence.Id
    Long id
}
