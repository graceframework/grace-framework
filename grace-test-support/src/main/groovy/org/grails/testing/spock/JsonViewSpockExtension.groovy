package org.grails.testing.spock

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

import grails.testing.views.json.JsonViewUnitTest

@CompileStatic
class JsonViewSpockExtension implements IGlobalExtension {

    JsonViewSetupSpecInterceptor jsonViewSetupSpecInterceptor = new JsonViewSetupSpecInterceptor()

    @Override
    void visitSpec(SpecInfo spec) {
        if (JsonViewUnitTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupSpecInterceptor(jsonViewSetupSpecInterceptor)
        }
    }
}
