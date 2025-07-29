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
package grails.compiler.ast;

import java.util.Arrays;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;

import org.grails.compiler.injection.GrailsASTUtils;

/**
 * Interface specific to Grails artefacts that returns the artefact type.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
public interface GrailsArtefactClassInjector extends ClassInjector {

    ArgumentListExpression ZERO_ARGS = new ArgumentListExpression();

    ClassNode[] EMPTY_CLASS_ARRAY = new ClassNode[0];

    Parameter[] ZERO_PARAMETERS = new Parameter[0];

    default String[] getArtefactTypes() {
        return new String[0];
    }

    @Override
    default boolean shouldInject(ClassNode classNode) {
        if (classNode.isEnum() || classNode instanceof InnerClassNode || classNode.getName().contains("$")) {
            return false;
        }
        String artefactType = GrailsASTUtils.getGrailsArtefactType(classNode);
        return artefactType != null && (Arrays.asList(getArtefactTypes()).contains(artefactType) || Arrays.asList(getArtefactTypes()).contains("*"));
    }

}
