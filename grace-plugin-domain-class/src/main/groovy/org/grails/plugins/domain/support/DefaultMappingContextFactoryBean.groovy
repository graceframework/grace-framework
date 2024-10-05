/*
 * Copyright 2004-2024 the original author or authors.
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
package org.grails.plugins.domain.support

import groovy.transform.CompileStatic
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.core.env.PropertyResolver

import grails.core.GrailsApplication
import grails.core.GrailsClass

import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.validation.constraints.factory.ConstraintFactory
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettingsBuilder
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext

/**
 * A factory bean for creating the default mapping context where an implementation of GORM is not present
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.3
 */
@CompileStatic
class DefaultMappingContextFactoryBean implements FactoryBean<MappingContext>, InitializingBean {

    protected final PropertyResolver configuration
    protected final GrailsApplication grailsApplication
    protected final MessageSource messageSource
    protected final ApplicationContext applicationContext
    protected MappingContext mappingContext

    private List<ConstraintFactory> constraintFactories = new ArrayList<>()

    DefaultMappingContextFactoryBean(GrailsApplication grailsApplication, MessageSource messageSource) {
        this.configuration = grailsApplication.config
        this.messageSource = messageSource
        this.grailsApplication = grailsApplication
        if (messageSource instanceof ApplicationContext) {
            this.applicationContext = (ApplicationContext) messageSource
        }
        else {
            this.applicationContext = null
        }
    }

    @Override
    MappingContext getObject() throws Exception {
        if (this.mappingContext == null) {
            afterPropertiesSet()
        }
        this.mappingContext
    }

    @Override
    Class<?> getObjectType() {
        MappingContext
    }

    @Override
    boolean isSingleton() {
        true
    }

    void setConstraintFactories(List<ConstraintFactory> constraintFactories) {
        this.constraintFactories = constraintFactories
    }

    @Override
    void afterPropertiesSet() throws Exception {
        ConnectionSourceSettingsBuilder builder = new ConnectionSourceSettingsBuilder(this.configuration)
        ConnectionSourceSettings settings = builder.build()

        this.mappingContext = new KeyValueMappingContext('default', settings)
        DefaultValidatorRegistry validatorRegistry = new DefaultValidatorRegistry(this.mappingContext, settings, this.messageSource)
        for (ConstraintFactory factory in this.constraintFactories) {
            validatorRegistry.addConstraintFactory(factory)
        }
        this.mappingContext.setValidatorRegistry(validatorRegistry)

        GrailsClass[] persistentClasses = this.grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
        this.mappingContext.addPersistentEntities(persistentClasses*.clazz as Class[])
    }

}
