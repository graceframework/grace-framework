/*
 * Copyright 2021-2023 the original author or authors.
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
package org.grails.plugins.i18n;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grails.web")
public class WebLocaleProperties {

    private String paramName = "lang";

    /**
     * Locale to use. By default, this locale is overridden by the "Session"
     * header.
     */
    private Locale locale;

    /**
     * Define how the locale should be resolved.
     */
    private LocaleResolver localeResolver = LocaleResolver.SESSION;

    public String getParamName() {
        return this.paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public LocaleResolver getLocaleResolver() {
        return this.localeResolver;
    }

    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    public enum LocaleResolver {

        /**
         * Always use the configured locale.
         */
        FIXED,

        /**
         * Use the "Accept-Language" header or the configured locale if the header is not
         * set.
         */
        ACCEPT_HEADER,

        /**
         * Use a locale attribute in the user's session.
         */
        SESSION,

        /**
         * Use a cookie.
         */
        COOKIE

    }
}
