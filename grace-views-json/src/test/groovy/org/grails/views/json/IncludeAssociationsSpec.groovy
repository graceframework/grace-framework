package org.grails.views.json

import spock.lang.Specification

import org.grails.testing.GrailsUnitTest
import org.grails.views.json.test.JsonViewTest

class IncludeAssociationsSpec extends Specification implements JsonViewTest, GrailsUnitTest {

    void 'test includeAssociations with json api'() {
        given: 'A collection'
        mappingContext.addPersistentEntities(Player, Team)
        Player player = new Player(name: 'Cantona')
        player.id = 1
        def players = [player]

        when: 'A collection type is rendered'
        def renderResult = render('''
import groovy.transform.*
import org.grails.views.json.*

@Field Collection<Player> players

json jsonapi.render(players, [associations: false])
''', [players: players]) {
            uri = '/foo'
        }

        then: 'The result is an array'
        renderResult.jsonText == '{"data":[{"type":"player","id":"1","attributes":{"name":"Cantona"}}],"links":{"self":"/foo"}}'
    }

}
