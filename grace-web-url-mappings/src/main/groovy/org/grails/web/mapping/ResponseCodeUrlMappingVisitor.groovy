/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.web.mapping

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.SourceUnit

@CompileStatic
class ResponseCodeUrlMappingVisitor extends ClassCodeVisitorSupport {

    boolean insideMapping = false
    List<String> responseCodes = []

    @Override
    void visitProperty(PropertyNode node) {
        if (node?.name == 'mappings') {
            insideMapping = true
        }
        super.visitProperty(node)
        if (node?.name == 'mappings') {
            insideMapping = false
        }
    }

    @Override
    void visitMethodCallExpression(MethodCallExpression call) {
        if (insideMapping && call.methodAsString =~ /^\d{3}$/ && !responseCodes.contains(call.methodAsString)) {
            responseCodes << call.methodAsString
        }
        super.visitMethodCallExpression(call)
    }

    @Override
    void visitExpressionStatement(ExpressionStatement statement) {
        super.visitExpressionStatement(statement)
    }

    @Override
    protected SourceUnit getSourceUnit() {
        null
    }

}
