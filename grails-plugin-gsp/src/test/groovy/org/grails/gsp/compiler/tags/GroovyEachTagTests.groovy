package org.grails.gsp.compiler.tags

import org.grails.taglib.GrailsTagException
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class GroovyEachTagTests {

    @Test
    void testEachWithSafeDereference() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))

        assertThrows(GrailsTagException) {
            tag.doStartTag()
        }

        tag.setAttributes('"in"': 'test?')

        tag.doStartTag()

       assertEquals("for( "+tag.getForeachRenamedIt()+" in test ) {"+ System.getProperty("line.separator")+ "changeItVariable(" + tag.getForeachRenamedIt() + ")" + System.getProperty("line.separator"),sw.toString())
    }

    @Test
    void testSimpleEach() {
        def sw = new StringWriter()
        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))

        shouldFail {
            tag.doStartTag()
        }

        tag.setAttributes('"in"': 'test')

        tag.doStartTag()

        assertEquals("for( "+tag.getForeachRenamedIt()+" in test ) {"+ System.getProperty("line.separator")+ "changeItVariable(" + tag.getForeachRenamedIt() + ")" + System.getProperty("line.separator"),sw.toString())
    }

    @Test
    void testEachWithVar() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))
        tag.setAttributes('"in"': 'test', '"var"':"i")

        tag.doStartTag()

        assertEquals("for( i in test ) {"+ System.getProperty("line.separator"),sw.toString())
    }

    @Test
    void testEachWithStatusOnly() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))
        tag.setAttributes('"in"': 'test', '"status"':"i")
        assertThrows(GrailsTagException) {
            tag.doStartTag()
        }
    }

    @Test
    void testEachWithStatusAndVar() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))
        tag.setAttributes('"in"': 'test', '"status"':"i",'"var"':"i")

        assertThrows(GrailsTagException) {
            tag.doStartTag()
        }
        tag.setAttributes('"var"':'j')
        tag.doStartTag()

        assert sw.toString().replaceAll('[\r\n]', '') == "loop:{int i = 0for( j in test ) {"
    }
}
