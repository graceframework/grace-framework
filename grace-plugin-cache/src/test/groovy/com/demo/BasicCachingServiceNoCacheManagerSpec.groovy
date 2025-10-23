package com.demo

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class BasicCachingServiceNoCacheManagerSpec extends Specification implements ServiceUnitTest<BasicCachingService> {

    void 'test invoking cacheable method when no cache manager is present'() {
        when: 'a cached method is invoked the first time'
        def result = service.getData()

        then: 'the code in the method is executed'
        result == 'Hello World!'
        service.invocationCounter == 1

        when: 'a cached method is invoked the second time'
        result = service.getData()

        then: 'the code in the method is still executed because no cache manager is present'
        result == 'Hello World!'
        service.invocationCounter == 2
    }
}
