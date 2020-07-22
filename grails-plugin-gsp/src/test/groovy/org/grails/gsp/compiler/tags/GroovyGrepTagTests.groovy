package org.grails.gsp.compiler.tags

import org.grails.taglib.GrailsTagException
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class GroovyGrepTagTests {

    @Test
    void testDoStartTag() {
        def sw = new StringWriter()

        def tag = new GroovyGrepTag()
        tag.init(out: new PrintWriter(sw))

        assertThrows(GrailsTagException, {
            tag.doStartTag()
        }, "Should throw exception for required attributes")

        tag.setAttributes('"in"': 'test', '"filter"':'\${~/regex/}')

        tag.doStartTag()

        assertEquals("for( "+tag.getForeachRenamedIt()+" in test.grep(~/regex/) ) {"+System.getProperty("line.separator")+ "changeItVariable(" + tag.getForeachRenamedIt() + ")" + System.getProperty("line.separator"), sw.toString())
    }

    @Test
    void testWithStatus() {
        def sw = new StringWriter()

        def tag = new GroovyGrepTag()
        tag.init(out: new PrintWriter(sw))

        tag.setAttributes('"in"': 'test', '"filter"':'\${~/regex/}','"status"':"i",'"var"':"t")

        tag.doStartTag()

        assertEquals("loop:{int i = 0for( t in test.grep(~/regex/) ) {", sw.toString().replaceAll('[\r\n]', ''))
    }
}
