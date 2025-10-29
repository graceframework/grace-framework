package grails.plugin.json.view

import spock.lang.Specification

import grails.views.resolve.GenericViewUriResolver

/**
 * Created by graemerocher on 25/08/15.
 */
class GenericViewUriResolverSpec extends Specification {

    void 'test resolve template URIs'() {
        given:
        def resolver = new GenericViewUriResolver('.gson')

        expect:
        resolver.resolveTemplateUri('foo', 'bar') == '/foo/_bar.gson'
    }

    void 'test resolve template URIs with a namespace'() {
        given:
        def resolver = new GenericViewUriResolver('.gson')

        expect:
        resolver.resolveTemplateUri('namespace', 'foo', 'bar') == '/namespace/foo/_bar.gson'
    }

}
