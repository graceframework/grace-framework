/*
 * Copyright 2004-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.web.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import groovy.xml.streamingmarkupsupport.StreamingMarkupWriter;

/**
 * A simple XML Stream Writer that leverages the StreamingMarkupWriter of Groovy
 *
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class XMLStreamWriter {

    protected StreamingMarkupWriter writer;

    protected Mode mode = Mode.INIT;

    protected Stack<String> tagStack = new Stack<String>();

    private char quoteChar = '"';

    public XMLStreamWriter(StreamingMarkupWriter writer) {
        this.writer = writer;
    }

    public XMLStreamWriter startDocument(String encoding, String version) throws IOException {
        if (this.mode != Mode.INIT) {
            throw new IllegalStateException();
        }
        this.writer.unescaped().write(String.format("<?xml version=\"%s\" encoding=\"%s\"?>", version, encoding));
        return this;
    }

    protected void startTag() throws IOException {
        this.writer.unescaped().write('<');
    }

    public XMLStreamWriter startNode(String tag) throws IOException {
        if (this.mode == Mode.TAG) {
            endStartTag();
        }

        startTag();
        this.writer.unescaped().write(tag);

        this.tagStack.push(tag);
        this.mode = Mode.TAG;
        return this;
    }

    public XMLStreamWriter end() throws IOException {
        Writer ue = this.writer.unescaped();
        if (this.mode == Mode.TAG) {
            ue.write(" />");
            if (this.tagStack.pop() == null) {
                throw new IllegalStateException();
            }
        }
        else if (this.mode == Mode.CONTENT) {
            ue.write('<');
            ue.write('/');
            String t = this.tagStack.pop();
            if (t == null) {
                throw new IllegalStateException();
            }
            ue.write(t);
            ue.write('>');
        }
        this.mode = Mode.CONTENT;
        return this;
    }

    public XMLStreamWriter attribute(String name, String value) throws IOException {
        if (this.mode != Mode.TAG) {
            throw new IllegalStateException();
        }
        Writer ue = this.writer.unescaped();
        ue.write(" ");
        ue.write(name);
        ue.write('=');
        ue.write(this.quoteChar);
        this.writer.setWritingAttribute(true);
        this.writer.escaped().write(value);
        this.writer.setWritingAttribute(false);
        ue.write(this.quoteChar);

        return this;
    }

    protected void endStartTag() throws IOException {
        this.writer.unescaped().write('>');
    }

    public XMLStreamWriter characters(String data) throws IOException {
        if (this.mode == Mode.TAG) {
            endStartTag();
        }
        this.mode = Mode.CONTENT;
        this.writer.escaped().write(data);

        return this;
    }

    protected enum Mode {
        INIT,
        TAG,
        CONTENT
    }

}
