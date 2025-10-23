package com.demo

import grails.plugin.cache.Cacheable
import grails.plugin.cache.CustomCacheKeyGenerator
import grails.plugin.cache.GrailsConcurrentMapCacheManager
import org.grails.testing.GrailsUnitTest
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import spock.lang.Specification

class ClassMarkedCacheableSpec extends Specification implements GrailsUnitTest {

    @Override
    Closure doWithSpring() {{ ->
        // TODO The plugin should provide a convenient mechanism for setting these up...
        grailsCacheManager(GrailsConcurrentMapCacheManager)
        customCacheKeyGenerator(CustomCacheKeyGenerator)
    }}

    void 'test that expected methods have been configured as cacheable'() {
        given:
        def obj = new ClassMarkedCacheable()
        applicationContext.autowireCapableBeanFactory.autowireBeanProperties(obj, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)

        expect: 'invocation counters to be 0'
        obj.methodOneCounter == 0
        obj.methodTwoCounter == 0

        when: 'invoking a caching method the first time'
        def result = obj.methodOne()

        then: 'the method should be invoked and the counter incremented'
        result == 'One'
        obj.methodOneCounter == 1

        when: 'invoking a caching method the first time'
        result = obj.methodTwo()

        then: 'the method should be invoked and the counter incremented'
        result == 'Two'
        obj.methodTwoCounter == 1

        when: 'invoking a caching method the second time'
        result = obj.methodOne()

        then: 'the cached value should be returned and the counter not incremented'
        result == 'One'
        obj.methodOneCounter == 1

        when: 'invoking a caching method the second time'
        result = obj.methodTwo()

        then: 'the cached value should be returned and the counter not incremented'
        result == 'Two'
        obj.methodTwoCounter == 1
    }
}

@Cacheable('demo')
class ClassMarkedCacheable {

    int methodOneCounter = 0
    int methodTwoCounter = 0

    def methodOne() {
        methodOneCounter++
        'One'
    }

    def methodTwo() {
        methodTwoCounter++
        'Two'
    }
}
