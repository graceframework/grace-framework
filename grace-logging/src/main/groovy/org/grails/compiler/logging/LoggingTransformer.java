/*
 * Copyright 2011-2025 the original author or authors.
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
package org.grails.compiler.logging;

import java.lang.reflect.Modifier;

import groovy.lang.GroovyClassLoader;
import groovy.util.logging.Slf4j;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.LogASTTransformation;

import grails.compiler.ast.AstTransformer;
import grails.compiler.ast.ClassInjector;

/**
 * Adds a log field to all artifacts.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
@AstTransformer
public class LoggingTransformer implements ClassInjector {

    private static final ClassNode SLF4J = ClassHelper.make(Slf4j.class);

    @Override
    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        performInjection(source, classNode);
    }

    @Override
    public void performInjection(SourceUnit source, ClassNode classNode) {
        if (classNode.getNodeMetaData(Slf4j.class) != null) {
            return;
        }

        // if already annotated skip
        if (!classNode.getAnnotations(SLF4J).isEmpty()) {
            return;
        }

        FieldNode logField = classNode.getField("log");
        if (logField != null) {
            if (!Modifier.isPrivate(logField.getModifiers())) {
                return;
            }
        }

        AnnotationNode annotationNode = new AnnotationNode(SLF4J);
        LogASTTransformation logASTTransformation = new LogASTTransformation();
        logASTTransformation.setCompilationUnit(new CompilationUnit(new GroovyClassLoader(getClass().getClassLoader())));
        logASTTransformation.visit(new ASTNode[] { annotationNode, classNode }, source);
        classNode.putNodeMetaData(Slf4j.class, annotationNode);
    }

    @Override
    public boolean shouldInject(ClassNode classNode) {
        return true;
    }

}
