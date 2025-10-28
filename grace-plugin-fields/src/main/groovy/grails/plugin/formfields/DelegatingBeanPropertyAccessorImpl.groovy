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

import java.lang.constant.Constable
import java.lang.constant.ConstantDesc

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.apache.commons.lang3.ClassUtils
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

import grails.gorm.Entity
import grails.util.GrailsNameUtils
import grails.validation.Validateable
import grails.web.databinding.WebDataBinding

import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.scaffolding.model.property.Constrained
import org.grails.scaffolding.model.property.DomainProperty

/**
 * @author James Kleeh
 * @since 2024.0.0
 */
@CompileStatic
@Canonical(includes = ['beanType', 'propertyName', 'propertyType'])
class DelegatingBeanPropertyAccessorImpl implements BeanPropertyAccessor {

    private DomainProperty domainProperty
    private Object rootBean
    private Object value
    private String pathFromRoot
    final Class beanType
    final String propertyName
    final Class propertyType

    DelegatingBeanPropertyAccessorImpl(Object rootBean, Object value, Class propertyType,
                                       String pathFromRoot, DomainProperty domainProperty) {
        this.rootBean = rootBean
        this.value = value
        this.pathFromRoot = pathFromRoot
        this.domainProperty = domainProperty
        this.propertyType = propertyType
        this.propertyName = domainProperty.name
        this.beanType = domainProperty.beanType
    }

    @Override
    Object getRootBean() {
        rootBean
    }

    @Override
    Class getRootBeanType() {
        rootBean.getClass()
    }

    @Override
    String getPathFromRoot() {
        pathFromRoot
    }

    @Override
    PersistentEntity getEntity() {
        domainProperty.domainClass
    }

    @Override
    List<Class> getBeanSuperclasses() {
        getSuperclassesAndInterfaces(beanType)
    }

    @Override
    List<Class> getPropertyTypeSuperclasses() {
        getSuperclassesAndInterfaces(propertyType)
    }

    @Override
    Object getValue() {
        value
    }

    @Override
    PersistentProperty getDomainProperty() {
        domainProperty.persistentProperty
    }

    @Override
    Constrained getConstraints() {
        domainProperty.constrained
    }

    @Override
    List<String> getLabelKeys() {
        List<String> labelKeys = new ArrayList<>()
        if (rootBean) {
            String simpleName = GrailsNameUtils.getPropertyName(rootBeanType.simpleName)
            labelKeys.add("${simpleName}.${pathFromRoot}.label".replaceAll(/\[(.+)\]/, '').toString())
        }
        labelKeys.addAll(domainProperty.labelKeys)
        labelKeys.unique()
    }

    @Override
    String getDefaultLabel() {
        domainProperty.defaultLabel
    }

    @Override
    List<FieldError> getErrors() {
        Errors errors
        if (rootBean instanceof Validateable) {
            errors = ((Validateable) rootBean).errors
        } else if (rootBean instanceof GormValidateable) {
            errors = ((GormValidateable) rootBean).errors
        }
        if (errors) {
            errors.getFieldErrors(pathFromRoot)
        } else {
            []
        }
    }

    @Override
    boolean isRequired() {
        domainProperty.required
    }

    @Override
    boolean isInvalid() {
        !errors.isEmpty()
    }

    @Override
    int hashCode() {
        return Objects.hash(beanType, propertyName, propertyType)
    }

    @Override
    boolean equals(Object obj) {
        this.hashCode() == obj?.hashCode()
    }

    private List<Class> getSuperclassesAndInterfaces(Class type) {
        List<Class> superclasses = new ArrayList<>()
        superclasses.addAll(ClassUtils.getAllSuperclasses(ClassUtils.primitiveToWrapper(type)))
        for (Object it in ClassUtils.getAllInterfaces(type)) {
            Class interfaceCls = (Class) it
            String name = interfaceCls.name
            if (name.indexOf('$') == -1) {
                if (interfaceCls.package != GormEntity.package) {
                    superclasses.add(interfaceCls)
                }
            }
        }
        superclasses.removeAll([Object, GroovyObject, Serializable, Cloneable, Comparable, Constable, ConstantDesc,
                                WebDataBinding, DirtyCheckable, Entity])
        return superclasses.unique()
    }

}
