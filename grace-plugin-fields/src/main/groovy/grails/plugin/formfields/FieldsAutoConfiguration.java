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
package grails.plugin.formfields;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import grails.core.GrailsApplication;
import grails.core.support.proxy.ProxyHandler;
import grails.plugins.GrailsPluginManager;
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.scaffolding.model.property.DomainPropertyFactory;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for Fields
 *
 * @author Michael Yan
 * @since 6.1
 */
@AutoConfiguration
@AutoConfigureOrder(100)
public class FieldsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BeanPropertyAccessorFactory beanPropertyAccessorFactory(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<MappingContext> grailsDomainClassMappingContext,
            ObjectProvider<ConstraintsEvaluator> validateableConstraintsEvaluator,
            ObjectProvider<DomainPropertyFactory> domainPropertyFactory,
            ObjectProvider<ProxyHandler> proxyHandler) {
        return new BeanPropertyAccessorFactory(grailsApplication.getObject(),
                grailsDomainClassMappingContext.getObject(),
                validateableConstraintsEvaluator.getObject(),
                domainPropertyFactory.getObject(),
                proxyHandler.getObject());
    }

    @Bean
    @ConditionalOnMissingBean
    public FormFieldsTemplateService formFieldsTemplateService(ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<GrailsPluginManager> pluginManager,
            ObjectProvider<GrailsConventionGroovyPageLocator> groovyPageLocator) {
        return new FormFieldsTemplateService(grailsApplication.getObject(), pluginManager.getObject(), groovyPageLocator.getObject());
    }

}
