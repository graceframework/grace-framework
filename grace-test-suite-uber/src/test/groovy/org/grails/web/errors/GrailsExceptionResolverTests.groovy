package org.grails.web.errors

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.InternalResourceView

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.util.Environment
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter

import org.grails.config.PropertySourcesConfig
import org.grails.exceptions.reporting.DefaultStackTraceFilterer
import org.grails.support.MockApplicationContext

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

/**
 * Test case for {@link org.grails.web.errors.GrailsExceptionResolver}.
 */
class GrailsExceptionResolverTests {

    private application = new DefaultGrailsApplication()
    private mockCtx = new MockApplicationContext()

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @BeforeEach
    void setUp() throws Exception {
        mockCtx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())
        def mainContext = new MockApplicationContext();
        mainContext.registerMockBean(UrlConverter.BEAN_NAME, new CamelCaseUrlConverter())
        application.mainContext = mainContext
    }

    @Test
    void testGetRootCause() {
        def ex = new Exception()
        assertEquals ex, GrailsExceptionResolver.getRootCause(ex)

        def root = new Exception("root")
        ex = new RuntimeException(root)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        ex = new IllegalStateException(ex)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        assertThrows(NullPointerException) {
            GrailsExceptionResolver.getRootCause(null)
        }
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
        def resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
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
        def resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
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
            def resolver = new GrailsExceptionResolver(grailsApplication: application)
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            def msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            resolver = new GrailsExceptionResolver(grailsApplication: application)
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            resolver = new GrailsExceptionResolver(grailsApplication: application)
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            def config = new ConfigSlurper().parse('''
grails.exceptionresolver.logRequestParameters = false
''')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithoutParameters, msg.replaceAll('[\n\r]', '')

            config = new ConfigSlurper().parse('''
grails.exceptionresolver.logRequestParameters = true
''')

            System.setProperty(Environment.KEY, Environment.DEVELOPMENT.name)
            resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.PRODUCTION.name)
            resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
            resolver.stackFilterer = new DefaultStackTraceFilterer()
            msg = resolver.getRequestLogMessage(request)
            assertEquals msgWithParameters, msg.replaceAll('[\n\r]', '')

            System.setProperty(Environment.KEY, Environment.TEST.name)
            resolver = new GrailsExceptionResolver(grailsApplication: new DefaultGrailsApplication(config: new PropertySourcesConfig().merge(config)))
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
