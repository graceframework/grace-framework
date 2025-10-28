package grails.plugin.formfields

import spock.lang.Specification

import grails.testing.web.taglib.TagLibUnitTest

import static jodd.lagarto.dom.jerry.Jerry.jerry

class DefaultFieldTemplateSpec extends Specification implements TagLibUnitTest<FormFieldsTagLib> {

    Map model = [:]

    void setup() {
        model.invalid = false
        model.label = 'label'
        model.property = 'property'
        model.required = false
        model.widget = '<input name="property">'
        views['/default/_wrapper.gsp'] = '''\
<g:set var="classes" value="fieldcontain "/>
<g:if test="${required}">
    <g:set var="classes" value="${classes + 'required'}"/>
</g:if>
<g:if test="${invalid}">
    <g:set var="classes" value="${classes + 'error'}"/>
</g:if>
<div class="${classes}">
    <label for="${prefix}${property}">${label}<g:if test="${required}"><span class="required-indicator">*</span></g:if></label>
    <%= widget %>
</div>'''
    }

    void 'default rendering'() {
        when:
        def output = tagLib.renderDefaultField(model)

        then:
        def root = jerry(output.toString()).children()
        root.is('div.fieldcontain')

        and:
        def label = root.find('label')
        label.text() == 'label'
        label.attr('for') == 'property'

        and:
        label.next().is('input[name=property]')
    }

    void 'container marked as invalid'() {
        given:
        model.invalid = true

        when:
        def output = tagLib.renderDefaultField(model)

        then:
        jerry(output.toString()).children().hasClass('error')
    }

    void 'container marked as required'() {
        given:
        model.required = true

        when:
        def output = tagLib.renderDefaultField(model)

        then:
        def root = jerry(output.toString()).children()
        root.hasClass('required')

        and:
        def indicator = root.find('label .required-indicator')
        indicator.size()
        indicator.text() == '*'
    }

}
