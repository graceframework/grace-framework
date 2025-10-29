package grails.plugin.json.view

import spock.lang.Specification

import grails.plugin.json.view.test.JsonViewTest

/**
 * Created by graemerocher on 27/09/2016.
 */
class GStringRenderSpec extends Specification implements JsonViewTest {

    void 'Test render an exception type'() {
        when: 'An exception is rendered'
        def renderResult = render('''
model {
    String value = 'abc'
}

json {
    example1 "abc"
    example2 "${value}"
    example3 "$value"
    example4 "$value".toString()
}
''')

        then: 'The exception is rendered'
        renderResult.json.example1 == 'abc'
        renderResult.json.example2 == 'abc'
        renderResult.json.example3 == 'abc'
        renderResult.json.example4 == 'abc'
    }

}
