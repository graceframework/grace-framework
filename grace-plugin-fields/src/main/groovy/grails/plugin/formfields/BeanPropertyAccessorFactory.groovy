/*
 * Copyright 2012-2025 the original author or authors.
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
package grails.plugin.formfields

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.regex.Pattern

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.context.support.StaticMessageSource

import grails.core.GrailsApplication
import grails.core.support.proxy.ProxyHandler
import grails.gorm.validation.DefaultConstrainedProperty

import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.scaffolding.model.property.Constrained
import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.model.property.DomainPropertyFactory

/**
 * @author Rob Fletcher
 * @since 2024.0.0
 */
@CompileStatic
class BeanPropertyAccessorFactory {

    private GrailsApplication grailsApplication
    private ConstraintsEvaluator constraintsEvaluator
    private ProxyHandler proxyHandler
    private DomainPropertyFactory domainPropertyFactory
    private MappingContext grailsDomainClassMappingContext

    BeanPropertyAccessorFactory(GrailsApplication grailsApplication,
                                MappingContext grailsDomainClassMappingContext,
                                ConstraintsEvaluator constraintsEvaluator,
                                DomainPropertyFactory domainPropertyFactory,
                                ProxyHandler proxyHandler) {
        this.grailsApplication = grailsApplication
        this.constraintsEvaluator = constraintsEvaluator
        this.proxyHandler = proxyHandler
        this.domainPropertyFactory = domainPropertyFactory
        this.grailsDomainClassMappingContext = grailsDomainClassMappingContext
    }

    BeanPropertyAccessor accessorFor(bean, String propertyPath) {
        if (bean == null) {
            new PropertyPathAccessor(propertyPath)
        } else {
            resolvePropertyFromPath(bean, propertyPath)
        }
    }

    private PersistentEntity resolveDomainClass(Class beanClass) {
        grailsDomainClassMappingContext.getPersistentEntity(beanClass.name)
    }

    private BeanPropertyAccessor resolvePropertyFromPath(Object bean, String pathFromRoot) {
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean)
        List<String> pathElements = pathFromRoot.tokenize('.')

        def params = [rootBean: bean, rootBeanType: bean.getClass(), pathFromRoot: pathFromRoot, grailsApplication: grailsApplication]

        DomainProperty domainProperty = resolvePropertyFromPathComponents(beanWrapper, pathElements, params)

        if (domainProperty != null) {
            new DelegatingBeanPropertyAccessorImpl(bean, params.value, params.propertyType as Class, pathFromRoot, domainProperty)
        } else {
            new BeanPropertyAccessorImpl(params)
        }
    }

    private DomainProperty resolvePropertyFromPathComponents(BeanWrapper beanWrapper, List<String> pathElements, Map<String, Object> params) {
        String propertyName = pathElements.remove(0)
        PersistentEntity beanClass = resolveDomainClass(beanWrapper.wrappedClass)
        Class propertyType = resolvePropertyType(beanWrapper, beanClass, propertyName)
        def value = beanWrapper.getPropertyValue(propertyName)
        if (pathElements.empty) {
            params.value = value
            params.propertyType = propertyType

            PersistentProperty persistentProperty
            String nameWithoutIndex = stripIndex(propertyName)
            if (beanClass != null) {
                persistentProperty = beanClass.getPropertyByName(nameWithoutIndex)
                if (!persistentProperty && beanClass.isIdentityName(nameWithoutIndex)) {
                    persistentProperty = beanClass.identity
                }
            }

            if (persistentProperty != null) {
                domainPropertyFactory.build(persistentProperty)
            } else {
                params.entity = beanClass
                params.beanType = beanWrapper.wrappedClass
                params.propertyType = propertyType
                params.propertyName = nameWithoutIndex
                params.domainProperty = null
                params.constraints = resolveConstraints(beanWrapper, (String) params.propertyName)
                null
            }
        } else {
            resolvePropertyFromPathComponents(beanWrapperFor(propertyType, value), pathElements, params)
        }
    }

    private Constrained resolveConstraints(BeanWrapper beanWrapper, String propertyName) {
        grails.gorm.validation.Constrained constraint = constraintsEvaluator.evaluate(beanWrapper.wrappedClass)[propertyName]
        constraint = constraint ?: createDefaultConstraint(beanWrapper, propertyName)
        new Constrained(constraint)
    }

    private grails.gorm.validation.Constrained createDefaultConstraint(BeanWrapper beanWrapper, String propertyName) {
        def defaultConstraint = new DefaultConstrainedProperty(beanWrapper.wrappedClass, propertyName,
                beanWrapper.getPropertyType(propertyName), new DefaultConstraintRegistry(new StaticMessageSource()))
        defaultConstraint.nullable = true
        defaultConstraint
    }

    private Class resolvePropertyType(BeanWrapper beanWrapper, PersistentEntity beanClass, String propertyName) {
        Class propertyType = null
        if (beanClass) {
            propertyType = resolveDomainPropertyType(beanClass, propertyName)
        }
        propertyType ?: resolveNonDomainPropertyType(beanWrapper, propertyName)
    }

    private Class resolveDomainPropertyType(PersistentEntity beanClass, String propertyName) {
        String propertyNameWithoutIndex = stripIndex(propertyName)
        PersistentProperty persistentProperty = beanClass.getPropertyByName(propertyNameWithoutIndex)
        if (!persistentProperty && beanClass.isIdentityName(propertyNameWithoutIndex)) {
            persistentProperty = beanClass.identity
        }
        if (!persistentProperty) {
            return null
        }
        boolean isIndexed = propertyName =~ INDEXED_PROPERTY_PATTERN
        if (isIndexed) {
            if (persistentProperty instanceof Basic) {
                persistentProperty.componentType
            } else if (persistentProperty instanceof Association) {
                persistentProperty.associatedEntity.javaClass
            }
        } else {
            persistentProperty.type
        }
    }

    @CompileDynamic
    private Class resolveNonDomainPropertyType(BeanWrapper beanWrapper, String propertyName) {
        Class<?> type = beanWrapper.getPropertyType(propertyName)
        if (type == null) {
            def match = propertyName =~ INDEXED_PROPERTY_PATTERN
            if (match) {
                Type genericType = beanWrapper.getPropertyDescriptor(match[0][1]).readMethod.genericReturnType
                if (genericType instanceof ParameterizedType) {
                    switch (genericType.rawType) {
                        case Collection:
                            type = genericType.actualTypeArguments[0]
                            break
                        case Map:
                            type = genericType.actualTypeArguments[1]
                            break
                    }
                } else {
                    type = Object
                }
            }
        }
        type
    }

    private BeanWrapper beanWrapperFor(Class type, value) {
        value ? PropertyAccessorFactory.forBeanPropertyAccess(proxyHandler.unwrapIfProxy(value)) : new BeanWrapperImpl(type)
    }

    private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

    @CompileDynamic
    @PackageScope
    static String stripIndex(String propertyName) {
        def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
        matcher.matches() ? matcher[0][1] : propertyName
    }

}
