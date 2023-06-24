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
package grails.gsp.taglib.compiler;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import grails.gsp.TagLib;

import org.grails.compiler.injection.ArtefactTypeAstTransformation;
import org.grails.core.artefact.gsp.TagLibArtefactHandler;

/**
 * A transformation that makes an Artefact on a {@link grails.gsp.TagLib}
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@GroovyASTTransformation
public class TagLibArtefactTypeAstTransformation extends ArtefactTypeAstTransformation {

    @Override
    protected String resolveArtefactType(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode) {
        return TagLibArtefactHandler.TYPE;
    }

    @Override
    protected Class<?> getAnnotationTypeClass() {
        return TagLib.class;
    }

}
