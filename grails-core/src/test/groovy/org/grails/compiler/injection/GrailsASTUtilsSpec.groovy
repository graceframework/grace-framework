package org.grails.compiler.injection

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.TempDir

import grails.persistence.Entity

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

        SourceUnit controllerSourceUnit = Mock()
        controllerSourceUnit.getName() >> controllerClassFile.absolutePath

        expect: 'Something should be recognized as a domain because grails-app/domain/org/grails/compiler/injection/Something.groovy exists'
        GrailsASTUtils.isDomainClass(new ClassNode(Something), controllerSourceUnit)

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
        someEntitySourceUnit.getName() >> someEntityFile.absolutePath

        expect: 'SomeEntity should be recognized as a domain because annotated with @grails.persistence.Entity'
        GrailsASTUtils.isDomainClass(new ClassNode(SomeEntity), someEntitySourceUnit)
    }

    void 'test domain class annotated with @javax.persistence.Entity'() {
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
        someEntitySourceUnit.getName() >> someEntityFile.absolutePath

        expect: 'SomeJpaEntity should be recognized as a domain because annotated with @grails.persistence.Entity'
        GrailsASTUtils.isDomainClass(new ClassNode(SomeJpaEntity), someEntitySourceUnit)
    }

    void 'test domain class annotated with @grails.gorm.annotation.Entity'() {
        setup:
        File projectDir = new File(tmpDir, "projectDir")

        // create /projectDir/app/models/ under java.io.tmpdir
        File grailsAppDir = new File(projectDir, 'app')
        File modelsDir = new File(grailsAppDir, 'models')

        String packagePath = SomeGormEntity.package.name.replace('.' as char, File.separatorChar)

        // create the source file that would contain the source for the
        // relevant domain class...
        File modelsPackageDir = new File(modelsDir, packagePath)
        modelsPackageDir.mkdirs()
        File someEntityFile = new File(modelsPackageDir, 'SomeGormEntity.groovy')
        someEntityFile.createNewFile()
        SourceUnit someEntitySourceUnit = Mock()
        someEntitySourceUnit.getName() >> someEntityFile.absolutePath

        expect: 'SomeGormEntity should be recognized as a domain because annotated with @grails.gorm.annotation.Entity'
        GrailsASTUtils.isDomainClass(new ClassNode(SomeGormEntity), someEntitySourceUnit)
    }
}

class Something {}
class SomethingElse {}

@Entity
class SomeEntity {}

@javax.persistence.Entity
class SomeJpaEntity {
    @javax.persistence.Id
    Long id
}

@grails.gorm.annotation.Entity
class SomeGormEntity {
}