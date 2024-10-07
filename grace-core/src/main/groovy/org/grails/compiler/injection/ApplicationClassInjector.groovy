/*
 * Copyright 2014-2024 the original author or authors.
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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.springframework.util.ClassUtils

import grails.compiler.ast.AstTransformer
import grails.compiler.ast.GrailsArtefactClassInjector
import grails.util.BuildSettings

import org.grails.core.artefact.ApplicationArtefactHandler
import org.grails.io.support.UrlResource

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.classX
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt

/**
 * Injector for the 'Application' class
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
@AstTransformer
class ApplicationClassInjector implements GrailsArtefactClassInjector {

    public static final String EXCLUDE_MEMBER = 'exclude'

    ApplicationArtefactHandler applicationArtefactHandler = new ApplicationArtefactHandler()

    private static final List<Integer> TRANSFORMED_INSTANCES = []

    @Override
    String[] getArtefactTypes() {
        [ApplicationArtefactHandler.TYPE] as String[]
    }

    @Override
    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        performInjection(source, classNode)
    }

    @Override
    void performInjection(SourceUnit source, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, classNode)
    }

    @Override
    @CompileDynamic
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        if (applicationArtefactHandler.isArtefact(classNode)) {
            Integer objectId = Integer.valueOf(System.identityHashCode(classNode))
            if (!TRANSFORMED_INSTANCES.contains(objectId)) {
                TRANSFORMED_INSTANCES << objectId

                List<Statement> statements = [
                        stmt(callX(classX(System), 'setProperty', args(propX(classX(BuildSettings), 'MAIN_CLASS_NAME'),
                                constX(classNode.name))))
                ]
                classNode.addStaticInitializerStatements(statements, true)

                ClassLoader classLoader = getClass().classLoader
                if (ClassUtils.isPresent('org.springframework.boot.autoconfigure.SpringBootApplication', classLoader)) {
                    GrailsASTUtils.addAnnotationOrGetExisting(classNode,
                            ClassHelper.make(classLoader.loadClass('org.springframework.boot.autoconfigure.SpringBootApplication')))
                }
            }
        }
    }

    @Override
    boolean shouldInject(URL url) {
        if (url == null) {
            return false
        }
        UrlResource res = new UrlResource(url)
        res.filename.endsWith('Application.groovy')
    }

}
