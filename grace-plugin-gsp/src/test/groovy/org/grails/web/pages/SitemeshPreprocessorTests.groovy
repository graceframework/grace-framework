package org.grails.web.pages

import org.grails.gsp.compiler.SitemeshPreprocessor
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class SitemeshPreprocessorTests {

    @Test
    void testSimpleParse() {
        def gspBody = '''
<html>
        <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>This is the title</title></head>
        <body onload="test();">
            body text
        </body>
</html>
'''
        def preprocessor = new SitemeshPreprocessor()
        def gspBodyExpected = '''
<html>
        <sitemesh:captureHead>
        <sitemesh:captureMeta gsp_sm_xmlClosingForEmptyTag="" http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <sitemesh:wrapTitleTag><sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:wrapTitleTag></sitemesh:captureHead>
        <sitemesh:captureBody onload="test();">
            body text
        </sitemesh:captureBody>
</html>
'''
        assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
    }

    @Test
    void testContentParse() {
        def gspBody = '''
<html>
        <head><title>This is the title</title></head>
        <body onload="test();">
            body text
        </body>
        <content tag="nav">
            content test
        </content>
</html>
'''
        def preprocessor = new SitemeshPreprocessor()
        def gspBodyExpected = '''
<html>
        <sitemesh:captureHead><sitemesh:wrapTitleTag><sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:wrapTitleTag></sitemesh:captureHead>
        <sitemesh:captureBody onload="test();">
            body text
        </sitemesh:captureBody>
        <sitemesh:captureContent tag="nav">
            content test
        </sitemesh:captureContent>
</html>
'''
        assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
    }

    @Test
    void testContentParse2() {
        def gspBody = '''
<html>
        <head><title>This is the title</title></head>
        <body onload="test();">
            body text
        </body>
        <content tag="nav">
            content test
        </content>
        <content tag="nav">
            content test
        </content>
</html>
'''
        def preprocessor = new SitemeshPreprocessor()
        def gspBodyExpected = '''
<html>
        <sitemesh:captureHead><sitemesh:wrapTitleTag><sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:wrapTitleTag></sitemesh:captureHead>
        <sitemesh:captureBody onload="test();">
            body text
        </sitemesh:captureBody>
        <sitemesh:captureContent tag="nav">
            content test
        </sitemesh:captureContent>
        <sitemesh:captureContent tag="nav">
            content test
        </sitemesh:captureContent>
</html>
'''
        assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
    }

    @Test
    void testSitemeshParameterParse() {
        def gspBody = '''
<html>
        <head><title>This is the title</title>
            <parameter name="foo" value="bar" />
        </head>
        <body>
            body text
        </body>
</html>
'''
        def preprocessor = new SitemeshPreprocessor()
        def gspBodyExpected = '''
<html>
        <sitemesh:captureHead><sitemesh:wrapTitleTag><sitemesh:captureTitle>This is the title</sitemesh:captureTitle></sitemesh:wrapTitleTag>
            <sitemesh:parameter name="foo" value="bar" />
        </sitemesh:captureHead>
        <sitemesh:captureBody>
            body text
        </sitemesh:captureBody>
</html>
'''
        assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
    }

    @Test
    void testOtherParse() {
        def gspBody = '''
<html>
        <head ><titlenot>This is not the title</titlenot></head>
        <body>
            body text
        </body>
</html>
'''
        def preprocessor = new SitemeshPreprocessor()
        def gspBodyExpected = '''
<html>
        <sitemesh:captureHead ><titlenot>This is not the title</titlenot></sitemesh:captureHead>
        <sitemesh:captureBody>
            body text
        </sitemesh:captureBody>
</html>
'''
        assertEquals(gspBodyExpected, preprocessor.addGspSitemeshCapturing(gspBody))
    }
}
