package org.grails.cli.command.completers

import org.jline.reader.ParsedLine
import spock.lang.*

class RegexCompletorSpec extends Specification {

    @Unroll("String '#source' is not matching")
    def "Simple pattern matches"() {
        given: "a regex completor and an empty candidate list"
        def completor = new RegexCompletor(/!\w+/)
        def candidateList = []
        def parsedLine = Stub(ParsedLine) {
            word() >> source
        }

        when: "the completor is invoked for a given string"
        completor.complete(null, parsedLine, candidateList)

        then: "that string is the sole candidate and the return value is 0"
        candidateList.size() == 1
        candidateList[0].value() == source

        where:
        source << [ "!ls", "!test_stuff" ]
    }

    @Unroll("String '#source' is incorrectly matching")
    def "Non matching strings"() {
        given: "a regex completor and an empty candidate list"
        def completor = new RegexCompletor(/!\w+/)
        def candidateList = []
        def parsedLine = Stub(ParsedLine) {
            word() >> source
        }

        when: "the completor is invoked for a given (non-matching) string"
        completor.complete(null, parsedLine, candidateList)

        then: "the candidate list is empty and the return value is -1"
        candidateList.size() == 0

        where:
        source << [ "!ls ls", "!", "test", "" ]
    }
}
