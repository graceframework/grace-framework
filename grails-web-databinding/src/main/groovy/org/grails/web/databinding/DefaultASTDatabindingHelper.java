/*
 * Copyright 2012-2023 the original author or authors.
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
package org.grails.web.databinding;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;

import grails.util.CollectionUtils;
import grails.util.GrailsNameUtils;

import org.grails.compiler.injection.GrailsASTUtils;

public class DefaultASTDatabindingHelper implements ASTDatabindingHelper {

    public static final String CONSTRAINTS_FIELD_NAME = "constraints";

    public static final String BINDABLE_CONSTRAINT_NAME = "bindable";

    public static final String DEFAULT_DATABINDING_WHITELIST = "$defaultDatabindingWhiteList";

    public static final String NO_BINDABLE_PROPERTIES = "$_NO_BINDABLE_PROPERTIES_$";

    private static final Map<ClassNode, Set<String>> CLASS_NODE_TO_WHITE_LIST_PROPERTY_NAMES = new HashMap<>();

    private static final Set<String> DOMAIN_CLASS_PROPERTIES_TO_EXCLUDE_BY_DEFAULT = CollectionUtils.newSet("id",
            "version", "dateCreated", "lastUpdated");

    private static final List<ClassNode> SIMPLE_TYPES = Arrays.asList(
            new ClassNode(Boolean.class),
            new ClassNode(Boolean.TYPE),
            new ClassNode(Byte.class),
            new ClassNode(Byte.TYPE),
            new ClassNode(Character.class),
            new ClassNode(Character.TYPE),
            new ClassNode(Short.class),
            new ClassNode(Short.TYPE),
            new ClassNode(Integer.class),
            new ClassNode(Integer.TYPE),
            new ClassNode(Long.class),
            new ClassNode(Long.TYPE),
            new ClassNode(Float.class),
            new ClassNode(Float.TYPE),
            new ClassNode(Double.class),
            new ClassNode(Double.TYPE),
            new ClassNode(BigInteger.class),
            new ClassNode(BigDecimal.class),
            new ClassNode(String.class),
            new ClassNode(URL.class));

    public void injectDatabindingCode(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        addDefaultDatabindingWhitelistField(source, classNode);
    }

    private void addDefaultDatabindingWhitelistField(SourceUnit sourceUnit, ClassNode classNode) {
        FieldNode defaultWhitelistField = classNode.getDeclaredField(DEFAULT_DATABINDING_WHITELIST);
        if (defaultWhitelistField != null) {
            return;
        }

        Set<String> propertyNamesToIncludeInWhiteList = getPropertyNamesToIncludeInWhiteList(sourceUnit, classNode);

        ListExpression listExpression = new ListExpression();
        if (propertyNamesToIncludeInWhiteList.size() > 0) {
            for (String propertyName : propertyNamesToIncludeInWhiteList) {
                listExpression.addExpression(new ConstantExpression(propertyName));

                FieldNode declaredField = getDeclaredFieldInInheritanceHierarchy(classNode, propertyName);
                boolean isSimpleType = false;
                if (declaredField != null) {
                    ClassNode type = declaredField.getType();
                    if (type != null) {
                        isSimpleType = SIMPLE_TYPES.contains(type);
                    }
                }
                if (!isSimpleType) {
                    listExpression.addExpression(new ConstantExpression(propertyName + "_*"));
                    listExpression.addExpression(new ConstantExpression(propertyName + ".*"));
                }
            }
        }
        else {
            listExpression.addExpression(new ConstantExpression(NO_BINDABLE_PROPERTIES));
        }

        classNode.addField(DEFAULT_DATABINDING_WHITELIST,
                Modifier.STATIC | Modifier.PUBLIC | Modifier.FINAL, new ClassNode(List.class),
                listExpression);
    }

    private FieldNode getDeclaredFieldInInheritanceHierarchy(ClassNode classNode, String propertyName) {
        FieldNode fieldNode = classNode.getDeclaredField(propertyName);
        if (fieldNode == null) {
            if (!classNode.getSuperClass().equals(new ClassNode(Object.class))) {
                return getDeclaredFieldInInheritanceHierarchy(classNode.getSuperClass(), propertyName);
            }
        }
        return fieldNode;
    }

    private Set<String> getPropertyNamesToIncludeInWhiteListForParentClass(SourceUnit sourceUnit, ClassNode parentClassNode) {
        Set<String> propertyNames;
        if (CLASS_NODE_TO_WHITE_LIST_PROPERTY_NAMES.containsKey(parentClassNode)) {
            propertyNames = CLASS_NODE_TO_WHITE_LIST_PROPERTY_NAMES.get(parentClassNode);
        }
        else {
            propertyNames = getPropertyNamesToIncludeInWhiteList(sourceUnit, parentClassNode);
        }
        return propertyNames;
    }

    private Set<String> getPropertyNamesToIncludeInWhiteList(SourceUnit sourceUnit, ClassNode classNode) {
        Set<String> unbindablePropertyNames = new HashSet<>();
        Set<String> bindablePropertyNames = new HashSet<>();
        if (!classNode.getSuperClass().equals(new ClassNode(Object.class))) {
            Set<String> parentClassPropertyNames = getPropertyNamesToIncludeInWhiteListForParentClass(sourceUnit, classNode.getSuperClass());
            bindablePropertyNames.addAll(parentClassPropertyNames);
        }

        FieldNode constraintsFieldNode = classNode.getDeclaredField(CONSTRAINTS_FIELD_NAME);
        if (constraintsFieldNode != null && constraintsFieldNode.hasInitialExpression()) {
            Expression constraintsInitialExpression = constraintsFieldNode.getInitialExpression();
            if (constraintsInitialExpression instanceof ClosureExpression) {

                Map<String, Map<String, Expression>> constraintsInfo =
                        GrailsASTUtils.getConstraintMetadata((ClosureExpression) constraintsInitialExpression);

                for (Entry<String, Map<String, Expression>> constraintConfig : constraintsInfo.entrySet()) {
                    String propertyName = constraintConfig.getKey();
                    Map<String, Expression> mapEntryExpressions = constraintConfig.getValue();
                    for (Entry<String, Expression> entry : mapEntryExpressions.entrySet()) {
                        String constraintName = entry.getKey();
                        if (BINDABLE_CONSTRAINT_NAME.equals(constraintName)) {
                            Expression valueExpression = entry.getValue();
                            Boolean bindableValue = null;
                            if (valueExpression instanceof ConstantExpression) {
                                Object constantValue = ((ConstantExpression) valueExpression).getValue();
                                if (constantValue instanceof Boolean) {
                                    bindableValue = (Boolean) constantValue;
                                }
                            }
                            if (bindableValue != null) {
                                if (Boolean.TRUE.equals(bindableValue)) {
                                    unbindablePropertyNames.remove(propertyName);
                                    bindablePropertyNames.add(propertyName);
                                }
                                else {
                                    bindablePropertyNames.remove(propertyName);
                                    unbindablePropertyNames.add(propertyName);
                                }
                            }
                            else {
                                GrailsASTUtils.warning(sourceUnit, valueExpression, "The bindable constraint for property [" +
                                        propertyName + "] in class [" + classNode.getName() +
                                        "] has a value which is not a boolean literal and will be ignored.");
                            }
                        }
                    }
                }
            }
        }

        Set<String> fieldsInTransientsList = getPropertyNamesExpressedInTransientsList(classNode);
        boolean isDomainClass = GrailsASTUtils.isDomainClass(classNode, sourceUnit);

        Set<String> propertyNamesToIncludeInWhiteList = new HashSet<>(bindablePropertyNames);
        List<FieldNode> fields = classNode.getFields();
        for (FieldNode fieldNode : fields) {
            String fieldName = fieldNode.getName();
            if ((!unbindablePropertyNames.contains(fieldName)) &&
                    (bindablePropertyNames.contains(fieldName) || shouldFieldBeInWhiteList(fieldNode, fieldsInTransientsList, isDomainClass))) {
                propertyNamesToIncludeInWhiteList.add(fieldName);
            }
        }

        Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
        for (Entry<String, MethodNode> methodEntry : declaredMethodsMap.entrySet()) {
            MethodNode value = methodEntry.getValue();
            if (classNode.equals(value.getDeclaringClass())) {
                Parameter[] parameters = value.getParameters();
                if (parameters != null && parameters.length == 1) {
                    String methodName = value.getName();
                    if (methodName.startsWith("set")) {
                        Parameter parameter = parameters[0];
                        ClassNode paramType = parameter.getType();
                        if (!paramType.equals(new ClassNode(Object.class))) {
                            String restOfMethodName = methodName.substring(3);
                            String propertyName = GrailsNameUtils.getPropertyName(restOfMethodName);
                            if (!unbindablePropertyNames.contains(propertyName) &&
                                    (!isDomainClass || !DOMAIN_CLASS_PROPERTIES_TO_EXCLUDE_BY_DEFAULT.contains(propertyName))) {
                                propertyNamesToIncludeInWhiteList.add(propertyName);
                            }
                        }
                    }
                }
            }
        }
        CLASS_NODE_TO_WHITE_LIST_PROPERTY_NAMES.put(classNode, propertyNamesToIncludeInWhiteList);
        Map<String, ClassNode> allAssociationMap = GrailsASTUtils.getAllAssociationMap(classNode);
        for (String associationName : allAssociationMap.keySet()) {
            if (!propertyNamesToIncludeInWhiteList.contains(associationName) && !unbindablePropertyNames.contains(associationName)) {
                propertyNamesToIncludeInWhiteList.add(associationName);
            }
        }
        return propertyNamesToIncludeInWhiteList;
    }

    private boolean shouldFieldBeInWhiteList(FieldNode fieldNode, Set<String> fieldsInTransientsList, boolean isDomainClass) {
        boolean shouldInclude = true;
        int modifiers = fieldNode.getModifiers();
        String fieldName = fieldNode.getName();
        if ((modifiers & Modifier.STATIC) != 0 ||
                (modifiers & Modifier.TRANSIENT) != 0 ||
                fieldsInTransientsList.contains(fieldName) ||
                (fieldNode.getType().equals(new ClassNode(Object.class)) && !fieldNode.getType().isUsingGenerics())) {
            shouldInclude = false;
        }
        else if (isDomainClass) {
            if (DOMAIN_CLASS_PROPERTIES_TO_EXCLUDE_BY_DEFAULT.contains(fieldName)) {
                shouldInclude = false;
            }
        }
        return shouldInclude;
    }

    private Set<String> getPropertyNamesExpressedInTransientsList(ClassNode classNode) {
        Set<String> transientFields = new HashSet<>();
        FieldNode transientsField = classNode.getField("transients");
        if (transientsField != null && transientsField.isStatic()) {
            Expression initialValueExpression = transientsField.getInitialValueExpression();
            if (initialValueExpression instanceof ListExpression) {
                ListExpression le = (ListExpression) initialValueExpression;
                List<Expression> expressions = le.getExpressions();
                for (Expression expr : expressions) {
                    if (expr instanceof ConstantExpression) {
                        ConstantExpression ce = (ConstantExpression) expr;
                        Object contantValue = ce.getValue();
                        if (contantValue instanceof String) {
                            transientFields.add((String) contantValue);
                        }
                    }
                }
            }
        }
        return transientFields;
    }

}
