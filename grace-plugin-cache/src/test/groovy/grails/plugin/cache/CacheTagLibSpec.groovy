package grails.plugin.cache

import grails.testing.web.taglib.TagLibUnitTest
import org.grails.core.exceptions.GrailsRuntimeException
import org.grails.plugin.cache.GrailsCacheManager
import org.grails.web.gsp.GroovyPagesTemplateRenderer
import org.springframework.cache.Cache
import spock.lang.Specification

import jakarta.servlet.ServletException


class CacheTagLibSpec extends Specification implements TagLibUnitTest<CacheTagLib> {

    GrailsCacheManager grailsCacheManager
    GroovyPagesTemplateRenderer groovyPagesTemplateRenderer

    def setup(){
        grailsCacheManager = Mock(GrailsCacheManager)
        groovyPagesTemplateRenderer = Mock(GroovyPagesTemplateRenderer)
        tagLib.grailsCacheManager = grailsCacheManager
        tagLib.groovyPagesTemplateRenderer = groovyPagesTemplateRenderer
    }

    def "test ServletException thrown from cache manager"(){
        when:
        String output = tagLib.block()

        then:
        1 * grailsCacheManager.getCache(_) >> {throw new ServletException("i hate caches")}
        !output
    }

    def "test ServletException thrown from cache manager where body has value"(){
        when:
        String output = tagLib.block(null, { return "not cached" } as Closure)

        then:
        1 * grailsCacheManager.getCache(_) >> {throw new ServletException("i hate caches")}
        output == "not cached"
    }

    def "test GrailsRuntimeException thrown from cache manager"(){
        when:
        String output = tagLib.block()

        then:
        1 * grailsCacheManager.getCache(_) >> {throw new GrailsRuntimeException("i hate caches")}
        !output
    }

    def "test GrailsRuntimeException thrown from cache manager where body has value"(){
        when:
        String output = tagLib.block(null, { return "not cached" } as Closure)

        then:
        1 * grailsCacheManager.getCache(_) >> {throw new GrailsRuntimeException("i hate caches")}
        output == "not cached"
    }

    def "test block method"(){
        given:
        Cache mockCache = Mock(Cache)
        Cache.ValueWrapper mockValue = Mock(Cache.ValueWrapper)

        when:
        String output = tagLib.block()

        then:
        1 * grailsCacheManager.getCache(_) >> mockCache
        1 * mockCache.get(_) >> mockValue
        1 * mockValue.get() >> "CACHED"
        output == "CACHED"
    }

    def "test block method where value returns empty"(){
        given:
        Cache mockCache = Mock(Cache)
        Cache.ValueWrapper mockValue = Mock(Cache.ValueWrapper)

        when:
        String output = tagLib.block()

        then:
        1 * grailsCacheManager.getCache(_) >> mockCache
        1 * mockCache.get(_) >> mockValue
        1 * mockValue.get() >> null
        !output
    }

    def "test block method where cache returns null"(){
        given:
        Cache mockCache = Mock(Cache)

        when:
        String output = tagLib.block()

        then:
        1 * grailsCacheManager.getCache(_) >> mockCache
        1 * mockCache.get(_) >> null
        !output
    }

}