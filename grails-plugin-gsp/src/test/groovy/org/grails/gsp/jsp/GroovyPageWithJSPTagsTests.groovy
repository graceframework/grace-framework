package org.grails.gsp.jsp

import grails.testing.spock.OnceBefore
import grails.testing.web.taglib.TagLibUnitTest
import grails.web.http.HttpHeaders
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.grails.web.pages.GroovyPagesServlet
import org.springframework.context.MessageSource
import org.springframework.web.servlet.support.JstlUtils
import spock.lang.*
import javax.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class GroovyPageWithJSPTagsTests extends Specification implements TagLibUnitTest<ApplicationTagLib> {

    static final Closure JSP_CONFIG = { config ->
        config.grails.gsp.tldScanPattern = 'classpath*:/META-INF/spring*.tld,classpath*:/META-INF/fmt.tld,classpath*:/META-INF/c.tld,classpath*:/META-INF/core.tld,classpath*:/META-INF/c-1_0-rt.tld'
    }

    @Override
    Closure doWithConfig() { JSP_CONFIG }

    @OnceBefore
    void onInit() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        GroovySystem.metaClassRegistry.removeMetaClass HttpServletRequest
        GroovySystem.metaClassRegistry.removeMetaClass MockHttpServletRequest

        webRequest.getCurrentRequest().setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, new GroovyPagesServlet())
    }

    def setup() {
        getRequest().addHeader(HttpHeaders.ACCEPT_LANGUAGE, Locale.ENGLISH)
    }

    def cleanupSpec() {
        GroovySystem.metaClassRegistry.removeMetaClass TagLibraryResolverImpl
    }

    // test for GRAILS-4573
    def testIterativeTags() {
        when:
        //JstlUtils.exposeLocalizationContext(request, grailsApplication.mainContext.getBean("messageSource", MessageSource))
        def template = '''
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<c:forEach var="i" begin="1" end="3"><c:out value="${i}" /> . <c:out value="${i}" /><br/></c:forEach>
</body>
</html>
'''
        String output = applyTemplate(template)
        //printCompiledSource(template)

        then:
        output.contains("1 . 1<br/>2 . 2<br/>3 . 3<br/>")
    }

    @PendingFeature(reason="until we upgrade to next version of test support")
    def testGRAILS3797() {

        when:
            messageSource.addMessage("A_ICON",request.locale, "test")

            def template = '''
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<html>
  <body>
      <g:form controller="search" action="search" method="get">
        <g:textField name="q" value="" />
        <g:actionSubmit value="search" /><br/>
        <img src="<spring:theme code="A_ICON" alt="icon"/>"/>
      </g:form>
  </body>
</html>
'''
        String output = applyTemplate(template)

        then:
        output.contains('<img src="test"/>')

    }

    void testDynamicAttributes() {

        when:
            def template = '''
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags/form" %>
<html>
  <body>
      <spring:form action="action" grails="rocks">
        
      </spring:form>
  </body>
</html>
'''
        String output = applyTemplate(template)

        then:
        output.contains('grails="rocks"')

    }



    // test for GRAILS-3845
    def testNestedJSPTags() {
        when:
        def template = '''
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<title>test</title>
</head>
<body>
<c:choose>
<c:when test="${1==1}">
hello
</c:when>
<c:when test="${1==0}">
goodbye
</c:when>

</c:choose>
</body>
</html>
'''
        String output = applyTemplate(template)

        then:
        output.contains 'hello'
        !output.contains("goodbye")
    }

    def testGSPCantOverrideDefaultNamespaceWithJSP() {
        when:
        def template = '<%@ taglib prefix="g" uri="http://java.sun.com/jsp/jstl/fmt" %><g:formatNumber number="10" format=".00"/>'
        String output = applyTemplate(template)

        then:
        output == '10.00'
    }

    def testGSPWithIterativeJSPTag() {
        when:
        def template = '''
 <%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<g:set var="foo" value="${[1,2,3]}" />
<c:forEach items="${foo}" var="num"><p>${num}</p></c:forEach>
'''

        String output = applyTemplate(template)

        then:
        output.trim() == '''<p>1</p><p>2</p><p>3</p>'''
    }

    def testSimpleTagWithValue() {
        when:
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><fmt:formatNumber value="${10}" pattern=".00"/>'
        String output = applyTemplate(template)

        then:
        output == '10.00'
    }

    def testInvokeJspTagAsMethod() {
        when:
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>${fmt.formatNumber(value:10, pattern:".00")}'
        String output = applyTemplate(template)

        then:
        output == '10.00'
    }

    def testInvokeJspTagAsMethodWithBody() {
        when:
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>${fmt.formatNumber(pattern:".00",10)}'
        String output = applyTemplate(template)

        then:
        output == '10.00'
    }

    def testSimpleTagWithBody() {
        when:
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><fmt:formatNumber pattern=".00">10</fmt:formatNumber>'
        String output = applyTemplate(template)

        then:
        output == '10.00'
    }

    def testSpringJSPTags() {
        when:
        def template ='''<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<form:form commandName="address" action="do">
<b>Zip: </b><form:input path="zip"/>
</form:form>'''

        request.setAttribute "address", new TestJspTagAddress(zip:"342343")
        request.setAttribute "command", new TestJspTagAddress(zip:"342343")
        String output = applyTemplate(template).trim()

        then:
        output == '''<form id="command" commandName="address" action="do" method="post">\n<b>Zip: </b><input id="zip" name="zip" type="text" value="342343"/>\n</form>'''
    }
}

class TestJspTagAddress {
    String zip
}
