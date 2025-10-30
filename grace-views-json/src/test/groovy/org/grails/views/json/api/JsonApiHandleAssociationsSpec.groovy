package org.grails.views.json.api

import groovy.json.JsonException
import spock.lang.Specification

import grails.persistence.Entity

import org.grails.testing.GrailsUnitTest
import org.grails.views.json.test.JsonRenderResult
import org.grails.views.json.test.JsonViewTest

class JsonApiHandleAssociationsSpec extends Specification implements JsonViewTest, GrailsUnitTest {

    void setup() {
        mappingContext.addPersistentEntities(Author, PublishedBook, Publisher)
    }

    void 'more than one associated objects should produce valid JSON'() {
        given:
        PublishedBook returnOfTheKing = new PublishedBook(
                title: 'The Return of the King',
                author: new Author(name: 'J.R.R. Tolkien'),
                publisher: new Publisher(name: 'George Allen & Unwin')
        )
        returnOfTheKing.id = 3
        returnOfTheKing.author.id = 9
        returnOfTheKing.publisher.id = 81

        when:
        JsonRenderResult result = render('''
import org.grails.views.json.api.PublishedBook

model {
    PublishedBook book
}

json jsonapi.render(book)
''', [book: returnOfTheKing])

        then: 'should not throw exception'
        notThrown(JsonException)
    }

}

@Entity
class PublishedBook {

    String title
    Author author
    Publisher publisher

}

@Entity
class Publisher {

    String name

}
