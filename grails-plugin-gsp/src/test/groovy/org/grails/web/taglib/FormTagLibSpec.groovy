package org.grails.web.taglib

import grails.core.GrailsApplication
import grails.core.GrailsUrlMappingsClass
import grails.testing.web.taglib.TagLibUnitTest
import grails.util.MockRequestDataValueProcessor
import org.grails.buffer.FastStringWriter
import org.grails.core.AbstractGrailsClass
import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.grails.plugins.web.taglib.FormTagLib
import spock.lang.Specification

class FormTagLibSpec extends Specification implements TagLibUnitTest<FormTagLib> {


     def setup() {
        tagLib.requestDataValueProcessor = new MockRequestDataValueProcessor()
     }

    def setupSpec() {
        def mappingsClosure = {
            "/admin/books"(controller:'books', namespace:'admin')
            "/books"(controller:'books')
        }
        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, new FormTagLibTests.MockGrailsUrlMappingsClass(mappingsClosure))

    }

    private static final class MockGrailsUrlMappingsClass extends AbstractGrailsClass implements GrailsUrlMappingsClass {
        Closure mappingClosure
        public MockGrailsUrlMappingsClass(Closure mappingClosure) {
            super(FormTagLibTests.class, "UrlMappings")
            this.mappingClosure = mappingClosure
        }
        @Override
        public Closure getMappingsClosure() {
            return mappingClosure
        }

        @Override
        public List getExcludePatterns() {
            return null
        }
    }

    def testFieldImplDoesNotApplyAttributesFromPreviousInvocation() {
        unRegisterRequestDataValueProcessor()
        // GRAILS-8250
        when:
        def attrs = [:]
        def out = new FastStringWriter()
        attrs.name = 'A'
        attrs.type = 'text'
        attrs.tagName = 'textField'

        then:
        tagLib.fieldImpl out, attrs
        assert '<input type="text" name="A" value="" id="A" />' == out.toString()

        when:
        out = new FastStringWriter()
        attrs.name = 'B'
        attrs.type = 'text'
        attrs.tagName = 'textField'
        tagLib.fieldImpl out, attrs

        then:
        assert '<input type="text" name="B" value="" id="B" />' == out.toString()
    }


    def testFormTagWithAlternativeMethod() {
        unRegisterRequestDataValueProcessor()
        when:
        def template = '<g:form url="/foo/bar" method="delete"></g:form>'

        then:
        String output = ('<form action="/foo/bar" method="post" ><input type="hidden" name="_method" value="DELETE" id="_method" /></form>')
        assert output == applyTemplate(template)
    }


    private void unRegisterRequestDataValueProcessor() {
        tagLib.requestDataValueProcessor = null
    }

    void testTextFieldTagWithRequestDataValueProcessor() {

        when:
        String template = '<g:textField name="testField" value="1" />'
        String output = applyTemplate(template)

        then:
        assert  output == '<input type="text" name="testField" value="1_PROCESSED_" id="testField" />'

        when:
        template = '<g:textField name="testField" value="${value}" />'
        output = applyTemplate(template, [value:/foo > " & < '/])

        then:
        assert output == '<input type="text" name="testField" value="foo &gt; &quot; &amp; &lt; &#39;_PROCESSED_" id="testField" />'
    }

    void testFormWithURL() {
        when:
        unRegisterRequestDataValueProcessor()
        String output = tagLib.form(new TreeMap([url:[controller:'con', action:'action'], id:'formElementId']))

        then:
        assert output == '<form action="/con/action" method="post" id="formElementId" ></form>'
    }


    void testFormWithURLAndRequestDataValueProcessor() {

        given:
        final StringWriter sw = new StringWriter()

        when:
        String output = tagLib.form(new TreeMap([url:[controller:'con', action:'action'], id:'formElementId']))

        then:
        assert output == '<form action="/con/action" method="post" id="formElementId" ><input type="hidden" name="requestDataValueProcessorHiddenName" value="hiddenValue" />\n</form>'
    }

}
