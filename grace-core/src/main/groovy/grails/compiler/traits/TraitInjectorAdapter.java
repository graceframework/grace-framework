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
package grails.compiler.traits;

import java.util.Arrays;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;

import org.grails.compiler.injection.GrailsASTUtils;

/**
 * Adapter of {@link TraitInjector}
 *
 * @author Michael Yan
 * @since 2024.0.0
 */
public abstract class TraitInjectorAdapter implements TraitInjector {

    @Override
    public String[] getArtefactTypes() {
        return new String[0];
    }

    @Override
    public boolean supports(ClassNode classNode) {
        if (classNode.isEnum() || classNode instanceof InnerClassNode || classNode.getName().contains("$")) {
            return false;
        }
        String artefactType = GrailsASTUtils.getGrailsArtefactType(classNode);
        return artefactType != null && (Arrays.asList(getArtefactTypes()).contains(artefactType) || Arrays.asList(getArtefactTypes()).contains("*"));
    }

}
