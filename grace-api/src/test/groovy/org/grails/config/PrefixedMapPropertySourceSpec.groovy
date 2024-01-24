package org.grails.config

import org.springframework.core.env.MapPropertySource

import spock.lang.Specification

/**
 * @author Michael Yan
 */
class PrefixedMapPropertySourceSpec extends Specification {
    void "Get property from prefixed map property source"() {
        given:
        PrefixedMapPropertySource propertySource = new PrefixedMapPropertySource(
                "grails.plugins.comment",
                new MapPropertySource("comment-plugin", [author: 'Michael', version: '1.0']))

        expect:
        propertySource.getProperty('grails.plugins.comment.author') == 'Michael'
        propertySource.getProperty('grails.plugins.comment.version') == '1.0'
        propertySource.getProperty('author') == null
        propertySource.getPropertyNames() == ['grails.plugins.comment.author', 'grails.plugins.comment.version'] as String[]
    }
}
