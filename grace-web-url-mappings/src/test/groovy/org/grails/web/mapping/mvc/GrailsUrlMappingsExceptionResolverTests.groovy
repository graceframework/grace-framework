package org.grails.web.mapping.mvc

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.InternalResourceView

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.util.Environment
import grails.util.GrailsWebMockUtil
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import grails.web.mapping.UrlMappingsHolder

import org.grails.config.PropertySourcesConfig
import org.grails.exceptions.reporting.DefaultStackTraceFilterer
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.support.MockApplicationContext
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.grails.web.servlet.view.CompositeViewResolver

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Test case for {@link org.grails.web.mapping.mvc.GrailsUrlMappingsExceptionResolver}.
 */
class GrailsUrlMappingsExceptionResolverTests {

    private application = new DefaultGrailsApplication()
    private resolver = new GrailsUrlMappingsExceptionResolver()
    private mockContext = new MockServletContext()
    private mockCtx = new MockApplicationContext()

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @BeforeEach
    void setUp() throws Exception {
        mockCtx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())
        def mainContext = new MockApplicationContext();
        mainContext.registerMockBean(UrlConverter.BEAN_NAME, new CamelCaseUrlConverter());
        application.mainContext = mainContext
    }

    @Test
    void testGetRootCause() {
        def ex = new Exception()
        assertEquals ex, GrailsUrlMappingsExceptionResolver.getRootCause(ex)

        def root = new Exception("root")
        ex = new RuntimeException(root)
        assertEquals root, GrailsUrlMappingsExceptionResolver.getRootCause(ex)

        ex = new IllegalStateException(ex)
        assertEquals root, GrailsUrlMappingsExceptionResolver.getRootCause(ex)

        assertThrows(NullPointerException) {
            GrailsUrlMappingsExceptionResolver.getRootCause(null)
        }
    }

    @Test
    void testResolveExceptionToView() {
        def mappings = new DefaultUrlMappingEvaluator(mockCtx).evaluateMappings {
            "500"(view: "myView")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def webRequest = GrailsWebMockUtil.bindMockWebRequest(mockCtx,
                new GrailsMockHttpServletRequest(), new GrailsMockHttpServletResponse())

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        ViewResolver viewResolver = new DummyViewResolver()
        mockCtx.registerMockBean "viewResolver", viewResolver
        mockCtx.registerMockBean 'grailsApplication', application
        mockCtx.registerMockBean CompositeViewResolver.BEAN_NAME, new CompositeViewResolver(viewResolvers: [viewResolver])
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties
        resolver.grailsApplication = application
        resolver.stackFilterer = new DefaultStackTraceFilterer()

        def ex = new Exception()
        def request = webRequest.currentRequest
        def response = webRequest.currentResponse
        def handler = new Object()
        def modelAndView = resolver.resolveException(request, response, handler, ex)

        assertNotNull modelAndView, "should have returned a ModelAndView"
        assertEquals "/myView", modelAndView.view.url
    }

    @Test
    void testResolveExceptionToController() {
        def mappings = new DefaultUrlMappingEvaluator(mockCtx).evaluateMappings {
            "500"(controller: "foo", action: "bar")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean GrailsApplication.APPLICATION_ID, application
        mockCtx.registerMockBean "multipartResolver", new StandardServletMultipartResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties
        resolver.grailsApplication = application
        resolver.stackFilterer = new DefaultStackTraceFilterer()

        def ex = new Exception()
        def request = webRequest.currentRequest
        MockHttpServletResponse response = webRequest.currentResponse
        def handler = new Object()
        def modelAndView = resolver.resolveException(request, response, handler, ex)

        assertNotNull modelAndView, "should have returned a ModelAndView"
        assertTrue modelAndView.empty

        assertEquals "/foo/bar", response.getForwardedUrl()
    }

    @Test
    void testResolveExceptionToControllerWhenResponseCommitted() {
        def mappings = new DefaultUrlMappingEvaluator(mockCtx).evaluateMappings {
            "500"(controller: "foo", action: "bar")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean GrailsApplication.APPLICATION_ID, application
        mockCtx.registerMockBean "multipartResolver", new StandardServletMultipartResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties
        resolver.grailsApplication = application
        resolver.stackFilterer = new DefaultStackTraceFilterer()

        def ex = new Exception()
        def request = webRequest.currentRequest
        MockHttpServletResponse response = webRequest.currentResponse
        def handler = new Object()
        response.setCommitted(true)
        def modelAndView = resolver.resolveException(request, response, handler, ex)

        assertNotNull modelAndView, "should have returned a ModelAndView"
        assertFalse modelAndView.empty
    }

    @Test
    void testLogRequestWithException() {
        def config = new ConfigSlurper().parse('''
grails.exceptionresolver.params.exclude = ['jennysPhoneNumber']
''')

        def request = new MockHttpServletRequest()
        request.setRequestURI("/execute/me")
        request.setMethod "GET"
        request.addParameter "foo", "bar"
        request.addParameter "one", "two"
        request.addParameter "jennysPhoneNumber", "8675309"

        System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
        def resolver = new GrailsUrlMappingsExceptionResolver(
                grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
        resolver.stackFilterer = new DefaultStackTraceFilterer()
        def msg = resolver.getRequestLogMessage(new RuntimeException("bad things happened"), request)

        assertEquals '''RuntimeException occurred when processing request:
URI: /execute/me
Method: GET
Message: bad things happened
Parameters:
  - foo: bar
  - one: two
  - jennysPhoneNumber: [FILTERED]

Filtered stacktrace:
'''.replaceAll('[\n\r]', ''), msg.replaceAll('[\n\r]', '')
    }

    @Test
    void testLogRequest() {
        def config = new ConfigSlurper().parse('''
grails.exceptionresolver.params.exclude = ['jennysPhoneNumber']
''')

        def request = new MockHttpServletRequest()
        request.setRequestURI("/execute/me")
        request.setMethod "GET"
        request.addParameter "foo", "bar"
        request.addParameter "one", "two"
        request.addParameter "jennysPhoneNumber", "8675309"

        System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
        def resolver = new GrailsUrlMappingsExceptionResolver(
                grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
        resolver.stackFilterer = new DefaultStackTraceFilterer()
        def msg = resolver.getRequestLogMessage(request)

        assertEquals '''Exception occurred when processing request:
URI: /execute/me
Method: GET
Parameters:
  - foo: bar
  - one: two
  - jennysPhoneNumber: [FILTERED]

Filtered stacktrace:
'''.replaceAll('[\n\r]', ''), msg.replaceAll('[\n\r]', '')
    }

    @Test
    void testDisablingRequestParameterLogging() {
        def oldEnvName = Environment.current.name
        try {
            def request = new MockHttpServletRequest()
            request.setRequestURI("/execute/me")
            request.setMethod "GET"
            request.addParameter "foo", "bar"
            request.addParameter "one", "two"

            def msgWithParameters = '''Exception occurred when processing request:
URI: /execute/me
Method: GET
Parameters:
  - foo: bar
  - one: two

Filtered stacktrace:'''.replaceAll('[\n\r]', '')
            def msgWithoutParameters = '''Exception occurred when processing request:
URI: /execute/me
Method: GET

Filtered stacktrace:'''.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            def resolver = new GrailsUrlMappingsExceptionResolver(grailsApplication: application)
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            def msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            resolver = new GrailsUrlMappingsExceptionResolver(grailsApplication: application)
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            resolver = new GrailsUrlMappingsExceptionResolver(grailsApplication: application)
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            def config = new ConfigSlurper().parse('''
grails.exceptionresolver.logRequestParameters = false
''')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            resolver = new GrailsUrlMappingsExceptionResolver(
                    grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            resolver = new GrailsUrlMappingsExceptionResolver(
                    grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            resolver = new GrailsUrlMappingsExceptionResolver(
                    grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            config = new ConfigSlurper().parse('''
grails.exceptionresolver.logRequestParameters = true
''')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            resolver = new GrailsUrlMappingsExceptionResolver(
                    grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            resolver = new GrailsUrlMappingsExceptionResolver(
                    grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            resolver = new GrailsUrlMappingsExceptionResolver(
                    grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')
        }
        finally {
            System.setProperty(Environment.KEY, oldEnvName)
        }
    }
}

class DummyViewResolver implements ViewResolver {

    @Override
    View resolveViewName(String viewName, Locale locale) {
        new InternalResourceView(viewName)
    }

}
