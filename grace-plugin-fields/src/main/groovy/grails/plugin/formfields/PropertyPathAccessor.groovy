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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.FieldError

import grails.gorm.validation.DefaultConstrainedProperty
import grails.util.GrailsNameUtils

import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.scaffolding.model.property.Constrained

import static java.util.Collections.EMPTY_LIST
import static org.apache.commons.lang3.StringUtils.substringAfterLast
import static grails.plugin.formfields.BeanPropertyAccessorFactory.stripIndex

/**
 * @author Rob Fletcher
 * @since 2024.0.0
 */
@CompileStatic
@Canonical(includes = ['beanType', 'propertyName', 'propertyType'])
class PropertyPathAccessor implements BeanPropertyAccessor {

    final String pathFromRoot
    final String propertyName = stripIndex pathFromRoot.contains('.') ? substringAfterLast(pathFromRoot, '.') : pathFromRoot
    final Class beanType = null
    final Class propertyType = Object

    PropertyPathAccessor(String pathFromRoot) {
        this.pathFromRoot = pathFromRoot
    }

    @Override
    String getDefaultLabel() {
        GrailsNameUtils.getNaturalName(propertyName)
    }

    @Override
    Object getRootBean() { null }

    @Override
    Class getRootBeanType() { null }

    @Override
    PersistentEntity getEntity() { null }

    @Override
    List<Class> getBeanSuperclasses() { EMPTY_LIST }

    @Override
    List<Class> getPropertyTypeSuperclasses() { EMPTY_LIST }

    @Override
    Object getValue() { null }

    @Override
    Constrained getConstraints() {
        new Constrained(new DefaultConstrainedProperty(Object, propertyName, String, new DefaultConstraintRegistry(new StaticMessageSource())))
    }

    @Override
    PersistentProperty getDomainProperty() { null }

    @Override
    List<String> getLabelKeys() { EMPTY_LIST }

    @Override
    List<FieldError> getErrors() { EMPTY_LIST }

    @Override
    boolean isRequired() { false }

    @Override
    boolean isInvalid() { false }

}
