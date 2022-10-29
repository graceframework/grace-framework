package org.grails.web.util;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BoundedCharsAsEncodedBytesCounterTest {

    private static final String TEST_STRING = "Hello \u00f6\u00e4\u00e5\u00d6\u00c4\u00c5!";

    @Test
    public void testCalculation() throws Exception {
        BoundedCharsAsEncodedBytesCounter counter = new BoundedCharsAsEncodedBytesCounter(1024, StandardCharsets.ISO_8859_1.name());
        counter.getCountingWriter();
        counter.update(TEST_STRING);
        assertEquals(13, counter.size());
        assertEquals(13, TEST_STRING.getBytes(StandardCharsets.ISO_8859_1).length);
        counter.update(TEST_STRING);
        assertEquals(26, counter.size());
    }
}
