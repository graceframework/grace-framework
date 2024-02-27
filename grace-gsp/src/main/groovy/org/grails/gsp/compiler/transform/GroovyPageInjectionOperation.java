/*
 * Copyright 2011-2022 the original author or authors.
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
package org.grails.gsp.compiler.transform;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;

import grails.compiler.ast.ClassInjector;
import grails.compiler.ast.GroovyPageInjector;

import org.grails.compiler.injection.GrailsAwareInjectionOperation;

/**
 * A GroovyPage compiler injection operation that uses a specified array of ClassInjector instances to
 * attempt AST injection.
 *
 * @author Stephane Maldini
 * @since 2.0
 */
public class GroovyPageInjectionOperation extends GrailsAwareInjectionOperation {

    private GroovyPageInjector[] groovyPageInjectors;

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        for (GroovyPageInjector classInjector : getGroovyPageInjectors()) {
            try {
                classInjector.performInjection(source, context, classNode);
            }
            catch (RuntimeException e) {
                System.err.println("Error occurred calling AST injector [" + classInjector.getClass() + "]: " + e.getMessage());
                e.printStackTrace(System.err);
                throw e;
            }
        }
    }

    private GroovyPageInjector[] getGroovyPageInjectors() {
        if (this.groovyPageInjectors == null) {
            List<GroovyPageInjector> injectors = new ArrayList<>();
            for (ClassInjector ci : getClassInjectors()) {
                if (ci instanceof GroovyPageInjector) {
                    injectors.add((GroovyPageInjector) ci);
                }
            }
            this.groovyPageInjectors = injectors.toArray(new GroovyPageInjector[0]);
        }
        return this.groovyPageInjectors;
    }

}
