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

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.SourceUnit
import spock.lang.Specification
import spock.lang.TempDir

import grails.gorm.annotation.Entity

/**
 * @author Michael Yan
 * @since 2024.0.0
 */
class GrailsASTUtilsSpec extends Specification {

    @TempDir
    File tmpDir

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
        ModuleNode ast = new ModuleNode(someEntitySourceUnit)
        ast.putNodeMetaData('PROJECT_DIR', projectDir.absolutePath)
        ast.putNodeMetaData('GRAILS_APP_DIR', grailsAppDir.absolutePath)
        someEntitySourceUnit.getAST() >> ast
        someEntitySourceUnit.getName() >> someEntityFile.absolutePath

        expect: 'SomeGormEntity should be recognized as a domain because annotated with @grails.gorm.annotation.Entity'
        GrailsASTUtils.isDomainClass(new ClassNode(SomeGormEntity), someEntitySourceUnit)
    }

}

@Entity
class SomeGormEntity {
}
