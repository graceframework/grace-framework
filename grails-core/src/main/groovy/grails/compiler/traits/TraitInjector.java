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
package grails.compiler.traits;

import org.codehaus.groovy.ast.ClassNode;
import org.springframework.core.Ordered;

/**
 * Functional interface to support injecting {@link grails.artefact.Artefact}
 * with {@link groovy.transform.Trait}
 *
 * @author Jeff Brown
 * @author Michael Yan
 * @since 3.0
 */
public interface TraitInjector extends Ordered {

    Class<?> getTrait();

    String[] getArtefactTypes();

    /**
     * Check TraitInjector supports classNode to inject
     *
     * @param classNode The classNode to check
     * @return True if classNode supports
     * @since 2022.3.0
     */
    default boolean supports(ClassNode classNode) {
        return true;
    }

    @Override
    default int getOrder() {
        return 0;
    }

}
