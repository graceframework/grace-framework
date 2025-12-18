package org.grails.web.errors

import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletRequest

import org.apache.groovy.util.Maps
import org.springframework.http.HttpStatusCode
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.validation.SimpleErrors
import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

import grails.validation.ValidationException

import org.grails.exceptions.reporting.DefaultStackTraceFilterer

class GrailsExceptionResolverSpec extends Specification {

    def "The viewName will be '/error' if throw a ValidationException"() {
        given:
        HttpServletRequest request = new MockHttpServletRequest()
        HttpServletResponse response = new MockHttpServletResponse()
        GrailsExceptionResolver exceptionResolver = new GrailsExceptionResolver()
        exceptionResolver.stackFilterer = new DefaultStackTraceFilterer()
        exceptionResolver.setExceptionMappings(Maps.of('java.lang.Exception', '/error') as Properties)

        when:
        ValidationException validationException = new ValidationException('Validation Exception', new SimpleErrors('This is a simple error'))
        ModelAndView modelAndView = exceptionResolver.resolveException(request, response, null, validationException)

        then:
        modelAndView.viewName == '/error'
        modelAndView.status == HttpStatusCode.valueOf(500)
    }

}
