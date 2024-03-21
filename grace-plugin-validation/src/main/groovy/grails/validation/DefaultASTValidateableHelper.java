/*
 * Copyright 2011-2023 the original author or authors.
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
package grails.validation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.groovy.ast.tools.AnnotatedNodeUtils;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import grails.compiler.ast.GrailsArtefactClassInjector;
import grails.gorm.validation.ConstrainedProperty;
import grails.util.GrailsNameUtils;

import org.grails.compiler.injection.ASTErrorsHelper;
import org.grails.compiler.injection.ASTValidationErrorsHelper;
import org.grails.web.plugins.support.ValidationSupport;

public class DefaultASTValidateableHelper implements ASTValidateableHelper {

    private static final String CONSTRAINED_PROPERTIES_PROPERTY_NAME = "$constraints";

    private static final String VALIDATE_METHOD_NAME = "validate";

    public void injectValidateableCode(ClassNode classNode, boolean defaultNullable) {
        ASTErrorsHelper errorsHelper = new ASTValidationErrorsHelper();
        errorsHelper.injectErrorsCode(classNode);
        addConstraintsField(classNode);
        addStaticInitializer(classNode);
        addGetConstraintsMethod(classNode, defaultNullable);
        addValidateMethod(classNode);
    }

    protected void addConstraintsField(final ClassNode classNode) {
        FieldNode field = classNode.getField(CONSTRAINED_PROPERTIES_PROPERTY_NAME);
        if (field == null || !field.getDeclaringClass().equals(classNode)) {
            classNode.addField(CONSTRAINED_PROPERTIES_PROPERTY_NAME,
                    Modifier.STATIC | Modifier.PRIVATE, new ClassNode(Map.class),
                    new ConstantExpression(null));
        }
    }

    private void addStaticInitializer(ClassNode classNode) {
        Expression nullOutConstrainedPropertiesExpression = new BinaryExpression(
                new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME),
                Token.newSymbol(Types.EQUALS, 0, 0), new ConstantExpression(null));
        List<Statement> statements = new ArrayList<>();
        statements.add(new ExpressionStatement(nullOutConstrainedPropertiesExpression));
        classNode.addStaticInitializerStatements(statements, true);
    }

    protected void addGetConstraintsMethod(ClassNode classNode, boolean defaultNullable) {
        String getConstraintsMethodName = "getConstraints";
        MethodNode getConstraintsMethod = classNode.getMethod(getConstraintsMethodName, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (getConstraintsMethod == null || !getConstraintsMethod.getDeclaringClass().equals(classNode)) {
            BooleanExpression isConstraintsPropertyNull = new BooleanExpression(new BinaryExpression(
                    new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME), Token.newSymbol(
                    Types.COMPARE_EQUAL, 0, 0), new ConstantExpression(null)));

            BlockStatement ifConstraintsPropertyIsNullBlockStatement = new BlockStatement();
            ArgumentListExpression getConstrainedPropertiesForClassArguments = new ArgumentListExpression();
            getConstrainedPropertiesForClassArguments.addExpression(new VariableExpression("this"));
            getConstrainedPropertiesForClassArguments.addExpression(new ConstantExpression(defaultNullable));
            Expression getConstraintsMethodCall = new StaticMethodCallExpression(ClassHelper.make(ValidationSupport.class),
                    "getConstrainedPropertiesForClass", getConstrainedPropertiesForClassArguments);
            Expression initializeConstraintsFieldExpression = new BinaryExpression(
                    new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME), Token.newSymbol(Types.EQUALS, 0, 0),
                    getConstraintsMethodCall);
            Statement ifConstraintsPropertyIsNullStatement = new IfStatement(isConstraintsPropertyNull,
                    ifConstraintsPropertyIsNullBlockStatement, new ExpressionStatement(new EmptyExpression()));

            ifConstraintsPropertyIsNullBlockStatement.addStatement(new ExpressionStatement(initializeConstraintsFieldExpression));
            if (!defaultNullable) {
                Map<String, ClassNode> propertiesToConstrain = getPropertiesToEnsureConstraintsFor(classNode);
                for (Map.Entry<String, ClassNode> entry : propertiesToConstrain.entrySet()) {
                    String propertyName = entry.getKey();
                    ClassNode propertyType = entry.getValue();
                    String cpName = "$" + propertyName + "$constrainedProperty";
                    ArgumentListExpression constrainedPropertyConstructorArgumentList = new ArgumentListExpression();
                    constrainedPropertyConstructorArgumentList.addExpression(new ClassExpression(classNode));
                    constrainedPropertyConstructorArgumentList.addExpression(new ConstantExpression(propertyName));
                    constrainedPropertyConstructorArgumentList.addExpression(new ClassExpression(propertyType));
                    ConstructorCallExpression constrainedPropertyCtorCallExpression = new ConstructorCallExpression(
                            new ClassNode(ConstrainedProperty.class), constrainedPropertyConstructorArgumentList);
                    Expression declareConstrainedPropertyExpression = new DeclarationExpression(
                            new VariableExpression(cpName, ClassHelper.OBJECT_TYPE),
                            Token.newSymbol(Types.EQUALS, 0, 0),
                            constrainedPropertyCtorCallExpression);

                    ArgumentListExpression applyConstraintMethodArgumentList = new ArgumentListExpression();
                    applyConstraintMethodArgumentList.addExpression(new ConstantExpression(ConstrainedProperty.NULLABLE_CONSTRAINT));
                    applyConstraintMethodArgumentList.addExpression(new ConstantExpression(defaultNullable));

                    Expression applyNullableConstraintMethodCallExpression = new MethodCallExpression(
                            new VariableExpression(cpName), "applyConstraint", applyConstraintMethodArgumentList);
                    ArgumentListExpression putMethodArgumentList = new ArgumentListExpression();
                    putMethodArgumentList.addExpression(new ConstantExpression(propertyName));
                    putMethodArgumentList.addExpression(new VariableExpression(cpName));
                    MethodCallExpression addToConstraintsMapExpression = new MethodCallExpression(
                            new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME), "put", putMethodArgumentList);
                    BlockStatement addNullableConstraintBlock = new BlockStatement();
                    addNullableConstraintBlock.addStatement(new ExpressionStatement(declareConstrainedPropertyExpression));
                    addNullableConstraintBlock.addStatement(new ExpressionStatement(applyNullableConstraintMethodCallExpression));
                    addNullableConstraintBlock.addStatement(new ExpressionStatement(addToConstraintsMapExpression));

                    Expression constraintsMapContainsKeyExpression = new MethodCallExpression(
                            new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME, ClassHelper.make(Map.class)),
                            "containsKey", new ArgumentListExpression(new ConstantExpression(propertyName)));
                    BooleanExpression ifPropertyIsAlreadyConstrainedExpression = new BooleanExpression(constraintsMapContainsKeyExpression);
                    Statement ifPropertyIsAlreadyConstrainedStatement = new IfStatement(
                            ifPropertyIsAlreadyConstrainedExpression,
                            new ExpressionStatement(new EmptyExpression()),
                            addNullableConstraintBlock);
                    ifConstraintsPropertyIsNullBlockStatement.addStatement(ifPropertyIsAlreadyConstrainedStatement);
                }
            }

            BlockStatement methodBlockStatement = new BlockStatement();
            methodBlockStatement.addStatement(ifConstraintsPropertyIsNullStatement);

            Statement returnStatement = new ReturnStatement(new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME));
            methodBlockStatement.addStatement(returnStatement);

            MethodNode methodNode = new MethodNode(getConstraintsMethodName, Modifier.STATIC | Modifier.PUBLIC,
                    new ClassNode(Map.class), GrailsArtefactClassInjector.ZERO_PARAMETERS, null, methodBlockStatement);
            if (classNode.redirect() == null) {
                classNode.addMethod(methodNode);
                AnnotatedNodeUtils.markAsGenerated(classNode, methodNode);
            }
            else {
                classNode.redirect().addMethod(methodNode);
                AnnotatedNodeUtils.markAsGenerated(classNode.redirect(), methodNode);
            }
        }
    }

    /**
     * Retrieves a Map describing all of the properties which need to be constrained for the class
     * represented by classNode.  The keys in the Map will be property names and the values are the
     * type of the corresponding property.
     *
     * @param classNode the class to inspect
     * @return a Map describing all of the properties which need to be constrained
     */
    protected Map<String, ClassNode> getPropertiesToEnsureConstraintsFor(ClassNode classNode) {
        Map<String, ClassNode> fieldsToConstrain = new HashMap<>();
        List<FieldNode> allFields = classNode.getFields();
        for (FieldNode field : allFields) {
            if (!field.isStatic()) {
                PropertyNode property = classNode.getProperty(field.getName());
                if (property != null) {
                    fieldsToConstrain.put(field.getName(), field.getType());
                }
            }
        }
        Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
        for (Entry<String, MethodNode> methodEntry : declaredMethodsMap.entrySet()) {
            MethodNode value = methodEntry.getValue();
            if (!value.isStatic() && value.isPublic() && classNode.equals(value.getDeclaringClass()) && value.getLineNumber() > 0) {
                Parameter[] parameters = value.getParameters();
                if (parameters == null || parameters.length == 0) {
                    String methodName = value.getName();
                    if (methodName.startsWith("get")) {
                        ClassNode returnType = value.getReturnType();
                        String restOfMethodName = methodName.substring(3);
                        String propertyName = GrailsNameUtils.getPropertyName(restOfMethodName);

                        fieldsToConstrain.put(propertyName, returnType);
                    }
                }
            }
        }

        ClassNode superClass = classNode.getSuperClass();
        if (!superClass.equals(new ClassNode(Object.class))) {
            fieldsToConstrain.putAll(getPropertiesToEnsureConstraintsFor(superClass));
        }
        return fieldsToConstrain;
    }

    protected void addValidateMethod(ClassNode classNode) {
        String fieldsToValidateParameterName = "$fieldsToValidate";
        MethodNode listArgValidateMethod = classNode.getMethod(VALIDATE_METHOD_NAME,
                new Parameter[] { new Parameter(new ClassNode(List.class), fieldsToValidateParameterName) });
        if (listArgValidateMethod == null) {
            BlockStatement validateMethodCode = new BlockStatement();
            ArgumentListExpression validateInstanceArguments = new ArgumentListExpression();
            validateInstanceArguments.addExpression(new VariableExpression("this"));
            validateInstanceArguments.addExpression(new VariableExpression(fieldsToValidateParameterName, ClassHelper.LIST_TYPE));
            ClassNode validationSupportClassNode = ClassHelper.make(ValidationSupport.class);
            StaticMethodCallExpression invokeValidateInstanceExpression = new StaticMethodCallExpression(validationSupportClassNode,
                    "validateInstance", validateInstanceArguments);
            validateMethodCode.addStatement(new ExpressionStatement(invokeValidateInstanceExpression));
            Parameter fieldsToValidateParameter = new Parameter(new ClassNode(List.class), fieldsToValidateParameterName);
            MethodNode methodNode = new MethodNode(
                    VALIDATE_METHOD_NAME, Modifier.PUBLIC, ClassHelper.boolean_TYPE,
                    new Parameter[] { fieldsToValidateParameter }, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, validateMethodCode);
            classNode.addMethod(methodNode);
            AnnotatedNodeUtils.markAsGenerated(classNode, methodNode);
        }
        MethodNode noArgValidateMethod = classNode.getMethod(VALIDATE_METHOD_NAME, GrailsArtefactClassInjector.ZERO_PARAMETERS);
        if (noArgValidateMethod == null) {
            BlockStatement validateMethodCode = new BlockStatement();

            ArgumentListExpression validateInstanceArguments = new ArgumentListExpression();
            validateInstanceArguments.addExpression(new CastExpression(new ClassNode(List.class), new ConstantExpression(null)));
            Expression callListArgValidateMethod = new MethodCallExpression(new VariableExpression("this"),
                    VALIDATE_METHOD_NAME, validateInstanceArguments);
            validateMethodCode.addStatement(new ReturnStatement(callListArgValidateMethod));
            MethodNode methodNode = new MethodNode(
                    VALIDATE_METHOD_NAME, Modifier.PUBLIC, ClassHelper.boolean_TYPE,
                    GrailsArtefactClassInjector.ZERO_PARAMETERS, GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY, validateMethodCode);
            classNode.addMethod(methodNode);
            AnnotatedNodeUtils.markAsGenerated(classNode, methodNode);
        }
    }

}
