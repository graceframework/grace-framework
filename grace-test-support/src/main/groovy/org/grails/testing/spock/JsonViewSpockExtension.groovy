package org.grails.testing.spock

import grails.testing.views.json.JsonViewUnitTest
import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.model.SpecInfo

@CompileStatic
class JsonViewSpockExtension extends AbstractGlobalExtension {

    JsonViewSetupSpecInterceptor jsonViewSetupSpecInterceptor = new JsonViewSetupSpecInterceptor()

    void visitSpec(SpecInfo spec) {
        if (JsonViewUnitTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupSpecInterceptor(jsonViewSetupSpecInterceptor)
        }
    }
}
