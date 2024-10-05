/*
 * Copyright 2024 the original author or authors.
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
package org.grails.plugins.domain;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

import grails.core.GrailsApplication;
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator;
import org.grails.datastore.gorm.validation.constraints.factory.ConstraintFactory;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.validation.ValidatorRegistry;
import org.grails.plugins.domain.support.ConstraintEvaluatorAdapter;
import org.grails.plugins.domain.support.DefaultConstraintEvaluatorFactoryBean;
import org.grails.plugins.domain.support.DefaultMappingContextFactoryBean;
import org.grails.plugins.domain.support.ValidatorRegistryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configure} for Domain Class
 *
 * @author Michael Yan
 *
 * @since 2023.1.0
 */
@AutoConfiguration
@AutoConfigureOrder(50)
public class DomainClassAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MappingContext grailsDomainClassMappingContext(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<MessageSource> messageSource, ObjectProvider<ConstraintFactory<?>> ConstraintFactoryProvider) throws Exception {
        DefaultMappingContextFactoryBean mappingContextFactoryBean = new DefaultMappingContextFactoryBean(grailsApplication.getObject(), messageSource.getObject());
        ConstraintFactory<?>[] constraintFactories = ConstraintFactoryProvider.orderedStream().toArray(ConstraintFactory[]::new);
        mappingContextFactoryBean.setConstraintFactories(List.of(constraintFactories));
        return mappingContextFactoryBean.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConstraintsEvaluator validateableConstraintsEvaluator(
            ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<MappingContext> grailsDomainClassMappingContext,
            ObjectProvider<MessageSource> messageSource) throws Exception {
        return new DefaultConstraintEvaluatorFactoryBean(grailsApplication.getObject(),
                grailsDomainClassMappingContext.getObject(), messageSource.getObject()).getObject();
    }

    @Bean("org.grails.beans.ConstraintsEvaluator")
    @ConditionalOnMissingBean
    public ConstraintEvaluatorAdapter constraintEvaluatorAdapter(ObjectProvider<ConstraintsEvaluator> validateableConstraintsEvaluator) {
        return new ConstraintEvaluatorAdapter(validateableConstraintsEvaluator.getObject());
    }

    @Bean
    @ConditionalOnMissingBean
    public ValidatorRegistry gormValidatorRegistry(ObjectProvider<MappingContext> grailsDomainClassMappingContext) throws Exception {
        return new ValidatorRegistryFactoryBean(grailsDomainClassMappingContext.getObject()).getObject();
    }

}
