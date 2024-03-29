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
package org.grails.databinding.converters;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;

import grails.databinding.TypedStructuredBindingEditor;
import grails.databinding.converters.FormattedValueConverter;
import grails.databinding.converters.ValueConverter;

import org.grails.databinding.converters.web.LocaleAwareBigDecimalConverter;
import org.grails.databinding.converters.web.LocaleAwareNumberConverter;
import org.grails.plugins.databinding.DataBindingConfigurationProperties;

/**
 * Default converters configuration.
 */
@AutoConfiguration
public class DefaultConvertersConfiguration {

    private final DataBindingConfigurationProperties configurationProperties;

    private final LocaleResolver localResolver;

    private final Jsr310ConvertersConfiguration jsr310ConvertersConfiguration;

    public DefaultConvertersConfiguration(ObjectProvider<LocaleResolver> localeResolverProvider,
            DataBindingConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
        this.jsr310ConvertersConfiguration = new Jsr310ConvertersConfiguration(configurationProperties);
        this.localResolver = localeResolverProvider.getIfAvailable();
    }

    /**
     * @return The default currency converter
     */
    @Bean
    public CurrencyValueConverter defaultCurrencyConverter() {
        return new CurrencyValueConverter();
    }

    @Bean
    public ValueConverter defaultGrailsBigDecimalConverter() {
        LocaleAwareBigDecimalConverter converter = new LocaleAwareBigDecimalConverter();
        converter.setTargetType(BigDecimal.class);
        converter.setLocaleResolver(this.localResolver);
        return converter;
    }

    @Bean
    public FormattedValueConverter offsetDateTimeConverter() {
        return this.jsr310ConvertersConfiguration.offsetDateTimeConverter();
    }

    @Bean
    public ValueConverter offsetDateTimeValueConverter() {
        return this.jsr310ConvertersConfiguration.offsetDateTimeValueConverter();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public TypedStructuredBindingEditor offsetDateTimeStructuredBindingEditor() {
        return this.jsr310ConvertersConfiguration.offsetDateTimeStructuredBindingEditor();
    }

    @Bean
    public FormattedValueConverter offsetTimeConverter() {
        return this.jsr310ConvertersConfiguration.offsetTimeConverter();
    }

    @Bean
    public ValueConverter offsetTimeValueConverter() {
        return this.jsr310ConvertersConfiguration.offsetTimeValueConverter();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public TypedStructuredBindingEditor offsetTimeStructuredBindingEditor() {
        return this.jsr310ConvertersConfiguration.offsetTimeStructuredBindingEditor();
    }

    @Bean
    public FormattedValueConverter localDateTimeConverter() {
        return this.jsr310ConvertersConfiguration.localDateTimeConverter();
    }

    @Bean
    public ValueConverter localDateTimeValueConverter() {
        return this.jsr310ConvertersConfiguration.localDateTimeValueConverter();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public TypedStructuredBindingEditor localDateTimeStructuredBindingEditor() {
        return this.jsr310ConvertersConfiguration.localDateTimeStructuredBindingEditor();
    }

    @Bean
    public FormattedValueConverter localDateConverter() {
        return this.jsr310ConvertersConfiguration.localDateConverter();
    }

    @Bean
    public ValueConverter localDateValueConverter() {
        return this.jsr310ConvertersConfiguration.localDateValueConverter();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public TypedStructuredBindingEditor localDateStructuredBindingEditor() {
        return this.jsr310ConvertersConfiguration.localDateStructuredBindingEditor();
    }

    @Bean
    public FormattedValueConverter localTimeConverter() {
        return this.jsr310ConvertersConfiguration.localTimeConverter();
    }

    @Bean
    public ValueConverter localTimeValueConverter() {
        return this.jsr310ConvertersConfiguration.localTimeValueConverter();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public TypedStructuredBindingEditor localTimeStructuredBindingEditor() {
        return this.jsr310ConvertersConfiguration.localTimeStructuredBindingEditor();
    }

    @Bean
    public FormattedValueConverter zonedDateTimeConverter() {
        return this.jsr310ConvertersConfiguration.zonedDateTimeConverter();
    }

    @Bean
    public ValueConverter zonedDateTimeValueConverter() {
        return this.jsr310ConvertersConfiguration.zonedDateTimeValueConverter();
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public TypedStructuredBindingEditor zonedDateTimeStructuredBindingEditor() {
        return this.jsr310ConvertersConfiguration.zonedDateTimeStructuredBindingEditor();
    }

    @Bean
    public ValueConverter periodValueConverter() {
        return this.jsr310ConvertersConfiguration.periodValueConverter();
    }

    @Bean
    public ValueConverter instantStringValueConverter() {
        return this.jsr310ConvertersConfiguration.instantStringValueConverter();
    }

    @Bean
    public ValueConverter instantValueConverter() {
        return this.jsr310ConvertersConfiguration.instantValueConverter();
    }

    @Bean
    public UUIDConverter defaultUUIDConverter() {
        return new UUIDConverter();
    }

    @Bean
    public ValueConverter defaultGrailsBigIntegerConverter() {
        LocaleAwareBigDecimalConverter converter = new LocaleAwareBigDecimalConverter();
        converter.setTargetType(BigInteger.class);
        converter.setLocaleResolver(this.localResolver);
        return converter;
    }

    @Bean
    public DateConversionHelper defaultDateConverter() {
        DateConversionHelper converter = new DateConversionHelper();
        converter.setDateParsingLenient(this.configurationProperties.isDateParsingLenient());
        converter.setFormatStrings(this.configurationProperties.getDateFormats());
        return converter;
    }

    @Bean
    public TimeZoneConverter defaultTimeZoneConverter() {
        return new TimeZoneConverter();
    }

    @Bean
    public LocaleAwareNumberConverter defaultShortConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(Short.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultshortConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(short.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultIntegerConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(Integer.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultintConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(int.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultFloatConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(Float.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultfloatConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(float.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultLongConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(Long.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultlongConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(long.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultDoubleConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(Double.class);
        return converter;
    }

    @Bean
    public LocaleAwareNumberConverter defaultdoubleConverter() {
        LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(this.localResolver);
        converter.setTargetType(double.class);
        return converter;
    }

}
