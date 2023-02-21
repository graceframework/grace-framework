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
package org.grails.plugins.databinding;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import grails.core.GrailsApplication;
import grails.databinding.TypedStructuredBindingEditor;
import grails.databinding.converters.FormattedValueConverter;
import grails.databinding.converters.ValueConverter;
import grails.databinding.events.DataBindingListener;
import grails.web.databinding.GrailsWebDataBinder;

import org.grails.databinding.bindingsource.DataBindingSourceCreator;
import org.grails.databinding.converters.DefaultConvertersConfiguration;
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry;
import org.grails.web.databinding.bindingsource.DefaultDataBindingSourceRegistry;
import org.grails.web.databinding.bindingsource.HalJsonDataBindingSourceCreator;
import org.grails.web.databinding.bindingsource.HalXmlDataBindingSourceCreator;
import org.grails.web.databinding.bindingsource.JsonApiDataBindingSourceCreator;
import org.grails.web.databinding.bindingsource.JsonDataBindingSourceCreator;
import org.grails.web.databinding.bindingsource.XmlDataBindingSourceCreator;

/**
 * Plugin for configuring the data binding features of Grails
 *
 * @author Graeme Rocher
 * @author Michael Yan
 *
 * @since 2022.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder
@EnableConfigurationProperties(DataBindingConfigurationProperties.class)
@ImportAutoConfiguration(DefaultConvertersConfiguration.class)
public class DataBindingConfiguration {

    private final DataBindingConfigurationProperties configurationProperties;

    public DataBindingConfiguration(DataBindingConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public GrailsWebDataBinder grailsWebDataBinder(
            ObjectProvider<GrailsApplication> grailsApplication,
            ObjectProvider<ValueConverter> valueConverters,
            ObjectProvider<FormattedValueConverter> formattedValueConverters,
            ObjectProvider<TypedStructuredBindingEditor> structuredBindingEditors,
            ObjectProvider<DataBindingListener> dataBindingListeners,
            ObjectProvider<MessageSource> messageSource) {

        GrailsWebDataBinder dataBinder = new GrailsWebDataBinder(grailsApplication.getIfAvailable());
        dataBinder.setConvertEmptyStringsToNull(this.configurationProperties.isConvertEmptyStringsToNull());
        dataBinder.setTrimStrings(this.configurationProperties.isTrimStrings());
        dataBinder.setAutoGrowCollectionLimit(this.configurationProperties.getAutoGrowCollectionLimit());

        ValueConverter[] defaultValueConverters = valueConverters.orderedStream().toArray(ValueConverter[]::new);
        AnnotationAwareOrderComparator.sort(defaultValueConverters);
        dataBinder.setValueConverters(defaultValueConverters);

        FormattedValueConverter[] defaultFormattedValueConverters = formattedValueConverters.orderedStream()
                .toArray(FormattedValueConverter[]::new);
        dataBinder.setFormattedValueConverters(defaultFormattedValueConverters);

        TypedStructuredBindingEditor[] defaultStructuredBindingEditors = structuredBindingEditors.orderedStream()
                .toArray(TypedStructuredBindingEditor[]::new);
        dataBinder.setStructuredBindingEditors(defaultStructuredBindingEditors);

        DataBindingListener[] defaultDataBindingListeners = dataBindingListeners.orderedStream()
                .toArray(DataBindingListener[]::new);
        dataBinder.setDataBindingListeners(defaultDataBindingListeners);

        dataBinder.setMessageSource(messageSource.getIfAvailable());
        return dataBinder;
    }

    @Bean
    public XmlDataBindingSourceCreator xmlDataBindingSourceCreator() {
        return new XmlDataBindingSourceCreator();
    }

    @Bean
    public JsonDataBindingSourceCreator jsonDataBindingSourceCreator() {
        return new JsonDataBindingSourceCreator();
    }

    @Bean
    public HalJsonDataBindingSourceCreator halJsonDataBindingSourceCreator() {
        return new HalJsonDataBindingSourceCreator();
    }

    @Bean
    public HalXmlDataBindingSourceCreator halXmlDataBindingSourceCreator() {
        return new HalXmlDataBindingSourceCreator();
    }

    @Bean
    public JsonApiDataBindingSourceCreator jsonApiDataBindingSourceCreator() {
        return new JsonApiDataBindingSourceCreator();
    }

    @Bean
    public DataBindingSourceRegistry dataBindingSourceRegistry(DataBindingSourceCreator... creators) {
        DefaultDataBindingSourceRegistry registry = new DefaultDataBindingSourceRegistry();
        registry.setDataBindingSourceCreators(creators);
        registry.initialize();
        return registry;
    }

}
