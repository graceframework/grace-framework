package org.grails.plugins.i18n;

import groovy.lang.Binding;
import org.springframework.context.MessageSource;

import grails.binding.GroovyShellBindingCustomizer;

public class I18nGroovyShellBindingCustomizer implements GroovyShellBindingCustomizer {

    private final MessageSource messageSource;

    public I18nGroovyShellBindingCustomizer(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void customize(Binding binding) {
        binding.setVariable("i18n", messageSource);
    }

}
