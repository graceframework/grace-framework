/*
 * Copyright 2014-2023 the original author or authors.
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
package org.grails.web.beans

import java.text.NumberFormat
import java.text.SimpleDateFormat

import javax.servlet.ServletContext

import groovy.transform.CompileStatic
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.beans.propertyeditors.CustomNumberEditor
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import grails.databinding.DataBinder
import grails.util.Environment

import org.grails.web.binding.CompositeEditor
import org.grails.web.binding.StructuredDateEditor
import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 * @author Graeme Rocher
 * @since 2.4
 */
@CompileStatic
class PropertyEditorRegistryUtils {

    private static final String PROPERTY_EDITOR_REGISTRARS = 'org.codehaus.groovy.grails.PROPERTY_EDITOR_REGISTRARS'
    private static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'"

    /**
     * Registers all known
     *
     * @param grailsWebRequest
     * @param registry
     * @param locale
     */
    static void registerCustomEditors(GrailsWebRequest grailsWebRequest, PropertyEditorRegistry registry, Locale locale) {
        // Formatters for the different number types.
        NumberFormat floatFormat = NumberFormat.getInstance(locale)
        NumberFormat integerFormat = NumberFormat.getIntegerInstance(locale)

        SimpleDateFormat dateFormat = new SimpleDateFormat(DataBinder.DEFAULT_DATE_FORMAT, locale)

        registry.registerCustomEditor(Date, new CustomDateEditor(dateFormat, true))
        registry.registerCustomEditor(BigDecimal, new CustomNumberEditor(BigDecimal, floatFormat, true))
        registry.registerCustomEditor(BigInteger, new CustomNumberEditor(BigInteger, floatFormat, true))
        registry.registerCustomEditor(Double, new CustomNumberEditor(Double, floatFormat, true))
        registry.registerCustomEditor(double, new CustomNumberEditor(Double, floatFormat, true))
        registry.registerCustomEditor(Float, new CustomNumberEditor(Float, floatFormat, true))
        registry.registerCustomEditor(float, new CustomNumberEditor(Float, floatFormat, true))
        registry.registerCustomEditor(Long, new CustomNumberEditor(Long, integerFormat, true))
        registry.registerCustomEditor(long, new CustomNumberEditor(Long, integerFormat, true))
        registry.registerCustomEditor(Integer, new CustomNumberEditor(Integer, integerFormat, true))
        registry.registerCustomEditor(int, new CustomNumberEditor(Integer, integerFormat, true))
        registry.registerCustomEditor(Short, new CustomNumberEditor(Short, integerFormat, true))
        registry.registerCustomEditor(short, new CustomNumberEditor(Short, integerFormat, true))
        registry.registerCustomEditor(Date, new CompositeEditor(new StructuredDateEditor(dateFormat, true),
                new CustomDateEditor(new SimpleDateFormat(JSON_DATE_FORMAT), true)))
        registry.registerCustomEditor(Calendar, new StructuredDateEditor(dateFormat, true))

        ServletContext servletContext = grailsWebRequest != null ? grailsWebRequest.getServletContext() : null
        registerCustomEditorsFromContext(servletContext, registry)
    }

    /**
     * Collects all PropertyEditorRegistrars in the application context and
     * calls them to register their custom editors
     *
     * @param servletContext
     * @param registry The PropertyEditorRegistry instance
     */
    private static void registerCustomEditorsFromContext(ServletContext servletContext, PropertyEditorRegistry registry) {
        if (servletContext == null) {
            return
        }

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext)
        if (context == null) {
            return
        }

        Map<String, PropertyEditorRegistrar> editors = (Map<String, PropertyEditorRegistrar>) servletContext.getAttribute(PROPERTY_EDITOR_REGISTRARS)
        if (editors == null) {
            editors = context.getBeansOfType(PropertyEditorRegistrar)
            if (!Environment.isDevelopmentMode()) {
                servletContext.setAttribute(PROPERTY_EDITOR_REGISTRARS, editors)
            }
        }
        for (PropertyEditorRegistrar editorRegistrar : editors.values()) {
            editorRegistrar.registerCustomEditors(registry)
        }
    }

}