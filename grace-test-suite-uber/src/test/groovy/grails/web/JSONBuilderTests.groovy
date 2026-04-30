package grails.web

import groovy.json.StreamingJsonBuilder

import grails.core.DefaultGrailsApplication
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.2
 */
class JSONBuilderTests {

    @BeforeEach
    void setUp() {
        def initializer = new ConvertersConfigurationInitializer(grailsApplication: new DefaultGrailsApplication())
        initializer.initialize()
    }

    @Test
    void testSimple() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        builder { rootprop "something" }

        assertEquals '{"rootprop":"something"}', writer.toString()
    }

    @Test
    void testArrays() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        builder {
            categories 'a', 'b', 'c'
            rootprop "something"
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something"}', writer.toString()
    }

    @Test
    void testSubObjects() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        builder {
            categories 'a', 'b', 'c'
            rootprop "something"
            test {
                subprop 10
            }
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10}}', writer.toString()
    }

    @Test
    void testAssignedObjects() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        builder {
            categories 'a', 'b', 'c'
            rootprop "something"
            test {
                subprop 10
            }
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10}}', writer.toString()
    }

    @Test
    void testNamedArgumentHandling() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        builder {
            categories 'a', 'b', 'c'
            rootprop  "something"
            test {
                subprop 10
                three 1, 2, 3
            }
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10,"three":[1,2,3]}}', writer.toString()
    }

    @Test
    void testArrayOfClosures() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        def arrayOfMap = [[bar: "hello"]]
        builder {
            foo arrayOfMap
        }

        assertEquals '{"foo":[{"bar":"hello"}]}', writer.toString()
    }

    @Test
    void testRootElementList() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        def results = ['one', 'two', 'three']

        builder results

        assertEquals '["one","two","three"]', writer.toString()
    }

    @Test
    void testExampleFromReferenceGuide() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        def list = [[title: 'one'], [title: 'two'], [title: 'three']]

        builder list

        assertEquals '[{"title":"one"},{"title":"two"},{"title":"three"}]', writer.toString()
    }

    @Test
    void testAppendToArray() {
        def writer = new StringWriter()
        def builder = new StreamingJsonBuilder(writer)

        def list = ['one', 'two', 'three']

        builder {
            books list, { String item ->
                title item
            }
        }

        assertEquals '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}', writer.toString()
    }
}
