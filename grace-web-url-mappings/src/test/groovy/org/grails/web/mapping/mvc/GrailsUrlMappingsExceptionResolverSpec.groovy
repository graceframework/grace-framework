package org.grails.web.mapping.mvc

import grails.web.mapping.UrlMappingsHolder
import grails.web.mapping.exceptions.UrlMappingException
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

import jakarta.servlet.http.HttpServletRequest

class GrailsUrlMappingsExceptionResolverSpec extends Specification {

    def "exception not thrown if an UrlMappingException is thrown while trying to match a request uri with a UrlMappingInfo "() {
        given:
        GrailsUrlMappingsExceptionResolver urlMappingsExceptionResolver = new GrailsUrlMappingsExceptionResolver()

        when:
        def urlMappingsHolder = Mock(UrlMappingsHolder)
        urlMappingsHolder.match(_ as String) >> { String uri ->
            throw new UrlMappingException('Unable to establish controller name to dispatch for')
        }
        HttpServletRequest request = new MockHttpServletRequest()
        Map params = urlMappingsExceptionResolver.extractRequestParamsWithUrlMappingHolder(urlMappingsHolder, request)

        then:
        noExceptionThrown()
        params.isEmpty()
    }
}