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
package org.grails.compiler.injection;

import java.lang.reflect.Field;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * Base implementation for the artefact type transformation.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
@GroovyASTTransformation
public abstract class AbstractArtefactTypeAstTransformation implements ASTTransformation {

    protected String resolveArtefactType(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode) {
        Expression value = annotationNode.getMember("value");

        if (value != null) {
            if (value instanceof ConstantExpression) {
                ConstantExpression ce = (ConstantExpression) value;
                return ce.getText();
            }
            if (value instanceof PropertyExpression) {
                PropertyExpression pe = (PropertyExpression) value;

                Expression objectExpression = pe.getObjectExpression();
                if (objectExpression instanceof ClassExpression) {
                    ClassExpression ce = (ClassExpression) objectExpression;
                    try {
                        Field field = ce.getType().getTypeClass().getDeclaredField(pe.getPropertyAsString());
                        return (String) field.get(null);
                    }
                    catch (Exception ignored) {
                    }
                }
            }
        }

        throw new RuntimeException("Class [" + classNode.getName() +
                "] contains an invalid @Artefact annotation. No artefact found for value specified.");
    }

    protected boolean isApplied(ClassNode cNode) {
        return GrailsASTUtils.isApplied(cNode, getAstAppliedMarkerClass());
    }

    protected void markApplied(ClassNode classNode) {
        GrailsASTUtils.markApplied(classNode, getAstAppliedMarkerClass());
    }

    protected Class<?> getAstAppliedMarkerClass() {
        return getClass();
    }

}
