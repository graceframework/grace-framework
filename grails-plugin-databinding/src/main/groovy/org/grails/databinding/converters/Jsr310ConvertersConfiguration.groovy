/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.databinding.converters

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import grails.databinding.TypedStructuredBindingEditor
import grails.databinding.converters.FormattedValueConverter
import grails.databinding.converters.ValueConverter

import org.grails.plugins.databinding.DataBindingConfigurationProperties

class Jsr310ConvertersConfiguration {

    Set<String> formatStrings = []

    Jsr310ConvertersConfiguration() {
    }

    Jsr310ConvertersConfiguration(DataBindingConfigurationProperties configurationProperties) {
        this.formatStrings = configurationProperties.dateFormats as Set<String>
    }

    FormattedValueConverter offsetDateTimeConverter() {
        new FormattedValueConverter() {

            @Override
            Object convert(Object value, String format) {
                OffsetDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                OffsetDateTime
            }

        }
    }

    ValueConverter offsetDateTimeValueConverter() {
        new Jsr310DateValueConverter<OffsetDateTime>() {

            @Override
            OffsetDateTime convert(Object value) {
                convert(value) { String format ->
                    OffsetDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                OffsetDateTime
            }

        }
    }

    TypedStructuredBindingEditor offsetDateTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<OffsetDateTime>() {

            @Override
            OffsetDateTime getDate(Calendar c) {
                OffsetDateTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                OffsetDateTime
            }

        }
    }

    FormattedValueConverter offsetTimeConverter() {
        new FormattedValueConverter() {

            @Override
            Object convert(Object value, String format) {
                OffsetTime.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                OffsetTime
            }

        }
    }

    ValueConverter offsetTimeValueConverter() {
        new Jsr310DateValueConverter<OffsetTime>() {

            @Override
            OffsetTime convert(Object value) {
                convert(value) { String format ->
                    OffsetTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                OffsetTime
            }

        }
    }

    TypedStructuredBindingEditor offsetTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<OffsetTime>() {

            @Override
            OffsetTime getDate(Calendar c) {
                OffsetTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                OffsetTime
            }

        }
    }

    FormattedValueConverter localDateTimeConverter() {
        new FormattedValueConverter() {

            @Override
            Object convert(Object value, String format) {
                LocalDateTime.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                LocalDateTime
            }

        }
    }

    ValueConverter localDateTimeValueConverter() {
        new Jsr310DateValueConverter<LocalDateTime>() {

            @Override
            LocalDateTime convert(Object value) {
                convert(value) { String format ->
                    LocalDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                LocalDateTime
            }

        }
    }

    TypedStructuredBindingEditor localDateTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<LocalDateTime>() {

            @Override
            LocalDateTime getDate(Calendar c) {
                LocalDateTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                LocalDateTime
            }

        }
    }

    FormattedValueConverter localDateConverter() {
        new FormattedValueConverter() {

            @Override
            Object convert(Object value, String format) {
                LocalDate.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                LocalDate
            }

        }
    }

    ValueConverter localDateValueConverter() {
        new Jsr310DateValueConverter<LocalDate>() {

            @Override
            LocalDate convert(Object value) {
                convert(value) { String format ->
                    LocalDate.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                LocalDate
            }

        }
    }

    TypedStructuredBindingEditor localDateStructuredBindingEditor() {
        new CustomDateBindingEditor<LocalDate>() {

            @Override
            LocalDate getDate(Calendar c) {
                LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
            }

            @Override
            Class<?> getTargetType() {
                LocalDate
            }

        }
    }

    FormattedValueConverter localTimeConverter() {
        new FormattedValueConverter() {

            @Override
            Object convert(Object value, String format) {
                LocalTime.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                LocalTime
            }

        }
    }

    ValueConverter localTimeValueConverter() {
        new Jsr310DateValueConverter<LocalTime>() {

            @Override
            LocalTime convert(Object value) {
                convert(value) { String format ->
                    LocalTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                LocalTime
            }

        }
    }

    TypedStructuredBindingEditor localTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<LocalTime>() {

            @Override
            LocalTime getDate(Calendar c) {
                LocalTime.of(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
            }

            @Override
            Class<?> getTargetType() {
                LocalTime
            }

        }
    }

    FormattedValueConverter zonedDateTimeConverter() {
        new FormattedValueConverter() {

            @Override
            Object convert(Object value, String format) {
                ZonedDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                ZonedDateTime
            }

        }
    }

    ValueConverter zonedDateTimeValueConverter() {
        new Jsr310DateValueConverter<ZonedDateTime>() {

            @Override
            ZonedDateTime convert(Object value) {
                convert(value) { String format ->
                    ZonedDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                ZonedDateTime
            }

        }
    }

    TypedStructuredBindingEditor zonedDateTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<ZonedDateTime>() {

            @Override
            ZonedDateTime getDate(Calendar c) {
                ZonedDateTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                ZonedDateTime
            }

        }
    }

    ValueConverter periodValueConverter() {
        new Jsr310DateValueConverter<Period>() {

            @Override
            Period convert(Object value) {
                Period.parse((CharSequence) value)
            }

            @Override
            Class<?> getTargetType() {
                Period
            }

        }
    }

    ValueConverter instantStringValueConverter() {
        new ValueConverter() {

            @Override
            boolean canConvert(Object value) {
                value instanceof CharSequence
            }

            @Override
            Object convert(Object value) {
                Instant.parse((CharSequence) value)
            }

            @Override
            Class<?> getTargetType() {
                Instant
            }

        }
    }

    ValueConverter instantValueConverter() {
        new ValueConverter() {

            @Override
            boolean canConvert(Object value) {
                value instanceof Number
            }

            @Override
            Object convert(Object value) {
                Instant.ofEpochMilli(((Number) value).longValue())
            }

            @Override
            Class<?> getTargetType() {
                Instant
            }

        }
    }

    abstract class Jsr310DateValueConverter<T> implements ValueConverter {

        @Override
        boolean canConvert(Object value) {
            value instanceof String
        }

        T convert(Object value, Closure callable) {
            T dateValue
            if (value instanceof String) {
                if (!value) {
                    return null
                }
                def firstException
                formatStrings.each { String format ->
                    if (dateValue == null) {
                        try {
                            dateValue = (T)callable.call(format)
                        } catch (Exception e) {
                            firstException = firstException ?: e
                        }
                    }
                }
                if (dateValue == null && firstException) {
                    throw firstException
                }
            }
            dateValue
        }

        @Override
        abstract Class<?> getTargetType()

    }

    abstract class CustomDateBindingEditor<T> extends AbstractStructuredDateBindingEditor<T> implements TypedStructuredBindingEditor<T> {

    }

}
