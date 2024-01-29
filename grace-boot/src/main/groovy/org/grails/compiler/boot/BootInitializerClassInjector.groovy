/*
 * Copyright 2015-2023 the original author or authors.
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
package org.grails.compiler.boot

import java.lang.reflect.Modifier

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.web.WebApplicationInitializer

import grails.boot.GrailsPluginApplication
import grails.compiler.ast.AstTransformer
import grails.compiler.ast.GlobalClassInjectorAdapter
import grails.plugins.metadata.PluginSource
import grails.util.Environment

import org.grails.boot.context.web.GrailsServletInitializer
import org.grails.compiler.injection.GrailsASTUtils
import org.grails.core.artefact.ApplicationArtefactHandler

/**
 * A transformation that automatically produces a Spring servlet initializer for a class that extends GrailsConfiguration.
 * Given a class "Application", it produces:
 *
 * <pre>
 * <code>
 *
 * class ApplicationLoader extends SpringBootServletInitializer {
 *
 *     protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
 *         return application.sources(Application)
 *     }
 * }
 * </code>
 * </pre>
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@AstTransformer
class BootInitializerClassInjector extends GlobalClassInjectorAdapter {

    public static final ClassNode PLUGIN_SOURCE_ANNOTATION = ClassHelper.make(PluginSource)

    ApplicationArtefactHandler applicationArtefactHandler = new ApplicationArtefactHandler()

    @Override
    void performInjectionInternal(SourceUnit source, ClassNode classNode) {
        // if this is a plugin source, then exit
        if (classNode.getAnnotations(PLUGIN_SOURCE_ANNOTATION)) {
            return
        }
        // don't generate for plugins
        if (classNode.getNodeMetaData('isPlugin')) {
            return
        }

        if (applicationArtefactHandler.isArtefact(classNode)
                && !GrailsASTUtils.isSubclassOfOrImplementsInterface(classNode, GrailsPluginApplication.name)) {
            List<MethodNode> methods = classNode.getMethods('main')
            for (MethodNode mn in methods) {
                if (Modifier.isStatic(mn.modifiers) && Modifier.isPublic(mn.modifiers)) {
                    Statement mainMethodBody = mn.code
                    if (mainMethodBody instanceof BlockStatement) {
                        BlockStatement bs = (BlockStatement) mainMethodBody
                        if (!bs.statements.isEmpty()) {
                            MethodCallExpression methodCallExpression = new MethodCallExpression(
                                    new ClassExpression(ClassHelper.make(System)),
                                    'setProperty',
                                    new ArgumentListExpression(
                                            new ConstantExpression(Environment.STANDALONE),
                                            new ConstantExpression(Boolean.TRUE.toString())
                                    )
                            )
                            ExpressionStatement statement = new ExpressionStatement(methodCallExpression)
                            bs.statements.add(0, statement)
                        }
                    }

                    ClassNode loaderClassNode = new ClassNode("${classNode.name}Loader",
                            Modifier.PUBLIC, ClassHelper.make(GrailsServletInitializer))

                    loaderClassNode.addInterface(ClassHelper.make(WebApplicationInitializer))

                    ClassNode springApplicationBuilder = ClassHelper.make(SpringApplicationBuilder)

                    Parameter parameter = new Parameter(springApplicationBuilder, 'application')
                    BlockStatement methodBody = new BlockStatement()

                    methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(
                            new VariableExpression(parameter), 'sources', new ClassExpression(classNode))))
                    loaderClassNode.addMethod(new MethodNode('configure', Modifier.PROTECTED,
                            springApplicationBuilder, [parameter] as Parameter[], [] as ClassNode[], methodBody))
                    source.getAST().addClass(loaderClassNode)

                    break
                }
            }
        }
    }

}
