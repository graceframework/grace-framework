package com.opensymphony.module.sitemesh.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomTagTest {

    @Test
    public void testWritesOutUserDefinedTag() {
        assertEquals("<hello/>", new CustomTag("hello", Tag.EMPTY).getContents());
        assertEquals("<hello>", new CustomTag("hello", Tag.OPEN).getContents());
        assertEquals("</hello>", new CustomTag("hello", Tag.CLOSE).getContents());
    }

    @Test
    public void testWritesAttributes() {
        CustomTag tag = new CustomTag("hello", Tag.EMPTY);
        tag.addAttribute("color", "green");
        tag.addAttribute("stuff", null);
        assertEquals("<hello color=\"green\" stuff/>", tag.getContents());
    }

    @Test
    public void testAllowsAttributesToBeManipulated() {
        CustomTag tag = new CustomTag("hello", Tag.OPEN);
        assertEquals("<hello>", tag.getContents());

        tag.addAttribute("a", "aaa");
        tag.addAttribute("b", "bbb");
        assertEquals("<hello a=\"aaa\" b=\"bbb\">", tag.getContents());

        tag.removeAttribute("b", false);
        assertEquals("<hello a=\"aaa\">", tag.getContents());

        tag.setAttributeValue("a", false, "zzz");
        assertEquals("<hello a=\"zzz\">", tag.getContents());
      
        tag.addAttribute("c", "ccc");
        int index = tag.getAttributeIndex("c", true);
        assertEquals(1, index);
        assertEquals("ccc", tag.getAttributeValue(index));
        assertEquals("c", tag.getAttributeName(index));
    }
}
