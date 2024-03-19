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

import java.net.URL;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;

import grails.compiler.ast.AstTransformer;
import grails.compiler.ast.GroovyPageInjector;

@AstTransformer
public class GroovyPageBytecodeOptimizer implements GroovyPageInjector {

    private static final String RUN_METHOD = "run";

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {

        // search run method in GSP script and get codeblock
        MethodNode runMethod = classNode.getMethod(RUN_METHOD, new Parameter[0]);
        if (runMethod != null && runMethod.getCode() instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) runMethod.getCode();

            //scan all MethodExpressionCalls to optimize them
            GroovyPageOptimizerVisitor groovyPageVisitor = new GroovyPageOptimizerVisitor(classNode);
            groovyPageVisitor.visitBlockStatement(block);
        }
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    @Override
    public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    //Avoid other injection
    public boolean shouldInject(URL url) {
        return false;
    }

}
