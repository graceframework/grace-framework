/*
 * Copyright 2014-2023 the original author or authors.
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

import java.lang.reflect.Modifier

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.compiler.traits.TraitInjector
import grails.core.ArtefactHandler

import org.grails.core.io.support.GrailsFactoriesLoader

/**
 * A global transformation that applies Grails' transformations to classes within a Grails project
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@CompileStatic
class GlobalGrailsClassInjectorTransformation implements ASTTransformation, CompilationUnitAware {

    static final ClassNode ARTEFACT_HANDLER_CLASS = ClassHelper.make(ArtefactHandler)
    static final ClassNode ARTEFACT_CLASS_NODE = ClassHelper.make(Artefact)
    static final ClassNode CLASS_INJECTOR_CLASS = ClassHelper.make(ClassInjector)
    static final ClassNode TRAIT_INJECTOR_CLASS = ClassHelper.make(TraitInjector)
    static final ClassNode APPLICATION_CONTEXT_COMMAND_CLASS = ClassHelper.make('grails.dev.commands.ApplicationCommand')

    CompilationUnit compilationUnit

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode ast = source.getAST()

        if (!GrailsASTUtils.isProjectSource(source)) {
            return
        }

        List<ArtefactHandler> artefactHandlers = GrailsFactoriesLoader.loadFactories(ArtefactHandler)

        File compilationTargetDirectory = resolveCompilationTargetDirectory(source)

        List<ClassNode> classNodes = new ArrayList<>(ast.getClasses())
        for (ClassNode classNode : classNodes) {
            if (updateGrailsFactoriesWithType(classNode, ARTEFACT_HANDLER_CLASS, compilationTargetDirectory)) {
                continue
            }
            if (updateGrailsFactoriesWithType(classNode, APPLICATION_CONTEXT_COMMAND_CLASS, compilationTargetDirectory)) {
                continue
            }
            if (updateGrailsFactoriesWithType(classNode, TRAIT_INJECTOR_CLASS, compilationTargetDirectory)) {
                continue
            }
            if (updateGrailsFactoriesWithType(classNode, CLASS_INJECTOR_CLASS, compilationTargetDirectory)) {
                continue
            }

            if (!GrailsASTUtils.isGrailsSource(classNode)) {
                continue
            }

            for (ArtefactHandler handler in artefactHandlers) {
                if (handler.isArtefact(classNode)) {
                    if (!classNode.getAnnotations(ARTEFACT_CLASS_NODE)) {
                        AnnotationNode annotationNode = new AnnotationNode(new ClassNode(Artefact))
                        annotationNode.addMember('value', new ConstantExpression(handler.type))
                        classNode.addAnnotation(annotationNode)

                        ArtefactTypeAstTransformation.performInjectionOnNode(source, classNode, handler.type, compilationUnit)
                        TraitInjectionUtils.processTraitsForNode(source, classNode, handler.type, compilationUnit)
                    }
                }
            }
        }
    }

    static File resolveCompilationTargetDirectory(SourceUnit source) {
        File targetDirectory
        if (source.class.name == 'org.codehaus.jdt.groovy.control.EclipseSourceUnit') {
            targetDirectory = GroovyEclipseCompilationHelper.resolveEclipseCompilationTargetDirectory(source)
        }
        else {
            targetDirectory = source.configuration.targetDirectory
        }

        targetDirectory ?: new File('build/classes/main')
    }

    static boolean updateGrailsFactoriesWithType(ClassNode classNode, ClassNode superType, File compilationTargetDirectory) {
        if (GrailsASTUtils.isSubclassOfOrImplementsInterface(classNode, superType)) {
            if (Modifier.isAbstract(classNode.getModifiers())) {
                return false
            }

            String classNodeName = classNode.name
            String superTypeName = superType.name
            Properties props = new Properties()

            // generate META-INF/grails.factories
            File factoriesFile = new File(compilationTargetDirectory, 'META-INF/grails.factories')
            if (!factoriesFile.parentFile.exists()) {
                factoriesFile.parentFile.mkdirs()
            }
            loadFromFile(props, factoriesFile)

            File sourceDirectory = findSourceDirectory(compilationTargetDirectory)
            File sourceFactoriesFile = new File(sourceDirectory, 'src/main/resources/META-INF/grails.factories')
            loadFromFile(props, sourceFactoriesFile)

            addToProps(props, superTypeName, classNodeName)

            factoriesFile.withWriter { Writer writer ->
                props.store(writer, 'Grails Factories File')
            }
            return true
        }
        false
    }

    private static void loadFromFile(Properties props, File factoriesFile) {
        if (factoriesFile.exists()) {
            Properties fileProps = new Properties()
            factoriesFile.withInputStream { InputStream input ->
                fileProps.load(input)
                fileProps.each { Map.Entry prop ->
                    addToProps(props, (String) prop.key, (String) prop.value)
                }
            }
        }
    }

    private static Properties addToProps(Properties props, String superTypeName, String classNodeNames) {
        List<String> classNodesNameList = classNodeNames.tokenize(',')
        classNodesNameList.forEach(classNodeName -> {
            String existing = props.getProperty(superTypeName)
            if (!existing) {
                props.put(superTypeName, classNodeName)
            }
            else if (existing && !existing.contains(classNodeName)) {
                props.put(superTypeName, [existing, classNodeName].join(','))
            }
        })
        props
    }

    private static File findSourceDirectory(File compilationTargetDirectory) {
        File sourceDirectory = compilationTargetDirectory
        while (sourceDirectory && !(sourceDirectory.name in ['build', 'target'])) {
            sourceDirectory = sourceDirectory.parentFile
        }
        sourceDirectory.parentFile
    }

}
