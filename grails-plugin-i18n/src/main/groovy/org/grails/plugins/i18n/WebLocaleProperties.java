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
        return paramName;
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
