package org.grails.web.converters

import org.grails.web.json.JSONArray
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class JSONArrayTests {

    @Test
    void testEquals() {
        assertEquals(getJSONArray(), getJSONArray())
    }

    @Test
    void testHashCode() {
        assertEquals(getJSONArray().hashCode(), getJSONArray().hashCode())
    }

    private getJSONArray() { new JSONArray(['a', 'b', 'c']) }
}
