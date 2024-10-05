/*
 * Copyright 2017-2024 the original author or authors.
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
import org.springframework.context.MessageSource

import grails.core.GrailsApplication

import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.MappingContext
import org.grails.validation.ConstraintEvalUtils

/**
 * A factory bean for creating the default ConstraintsEvaluator where an implementation of GORM is not present
 *
 * @author James Kleeh
 * @author Michael Yan
 * @since 3.3
 */
@CompileStatic
class DefaultConstraintEvaluatorFactoryBean implements FactoryBean<ConstraintsEvaluator> {

    private final GrailsApplication grailsApplication
    private final MappingContext grailsDomainClassMappingContext
    private final MessageSource messageSource

    DefaultConstraintEvaluatorFactoryBean(GrailsApplication grailsApplication,
                                          MappingContext grailsDomainClassMappingContext,
                                          MessageSource messageSource) {
        this.grailsApplication = grailsApplication
        this.grailsDomainClassMappingContext = grailsDomainClassMappingContext
        this.messageSource = messageSource
    }

    @Override
    ConstraintsEvaluator getObject() throws Exception {
        ConstraintRegistry registry = new DefaultConstraintRegistry(this.messageSource)

        new DefaultConstraintEvaluator(registry, this.grailsDomainClassMappingContext,
                ConstraintEvalUtils.getDefaultConstraints(this.grailsApplication.config))
    }

    @Override
    Class<?> getObjectType() {
        ConstraintsEvaluator
    }

    @Override
    boolean isSingleton() {
        true
    }

}
