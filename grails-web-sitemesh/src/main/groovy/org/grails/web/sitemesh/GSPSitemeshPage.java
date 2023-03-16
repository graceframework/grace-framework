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
package org.grails.web.sitemesh;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.module.sitemesh.parser.AbstractHTMLPage;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.compatability.Content2HTMLPage;

import org.grails.buffer.StreamCharBuffer;

/**
 * Grails/GSP specific implementation of Sitemesh's AbstractHTMLPage
 *
 * g:capture* tags in RenderTagLib are used to capture head, meta, title, component and body contents.
 * No html parsing is required for templating since capture tags are added at GSP compilation time.
 *
 * @author Lari Hotari, Sagire Software Oy
 */
public class GSPSitemeshPage extends AbstractHTMLPage implements Content {

    StreamCharBuffer headBuffer;

    StreamCharBuffer bodyBuffer;

    StreamCharBuffer pageBuffer;

    StreamCharBuffer titleBuffer;

    boolean used;

    boolean titleCaptured;

    Map<String, StreamCharBuffer> contentBuffers;

    private boolean renderingLayout;

    public GSPSitemeshPage() {
        this(false);
    }

    public GSPSitemeshPage(boolean renderingLayout) {
        reset();
        this.renderingLayout = renderingLayout;
    }

    public void reset() {
        this.headBuffer = null;
        this.bodyBuffer = null;
        this.pageBuffer = null;
        this.titleBuffer = null;
        this.used = false;
        this.titleCaptured = false;
        this.contentBuffers = null;
        this.renderingLayout = false;
    }

    public void addProperty(String name, Object value) {
        addProperty(name, (value == null ? null : String.valueOf(value)));
    }

    @Override
    public void addProperty(String name, String value) {
        super.addProperty(name, value);
        this.used = true;
    }

    @Override
    public void writeHead(Writer out) throws IOException {
        if (this.headBuffer == null) {
            return;
        }

        if (this.titleCaptured) {
            if (this.titleBuffer != null) {
                int headlen = this.headBuffer.length();
                this.titleBuffer.clear();
                if (this.headBuffer.length() < headlen) {
                    this.headBuffer.writeTo(out);
                    return;
                }
            }
            String headAsString = this.headBuffer.toString();
            // strip out title for sitemesh version of <head>
            out.write(headAsString.replaceFirst("(?is)<title(\\s[^>]*)?>(.*?)</title>", ""));
        }
        else {
            this.headBuffer.writeTo(out);
        }
    }

    @Override
    public void writeBody(Writer out) throws IOException {
        if (this.bodyBuffer != null) {
            this.bodyBuffer.writeTo(out);
        }
        else if (this.pageBuffer != null) {
            // no body was captured, so write the whole page content
            this.pageBuffer.writeTo(out);
        }
    }

    @Override
    public void writePage(Writer out) throws IOException {
        if (this.pageBuffer != null) {
            this.pageBuffer.writeTo(out);
        }
    }

    public String getHead() {
        if (this.headBuffer != null) {
            return this.headBuffer.toString();
        }
        return null;
    }

    @Override
    public String getBody() {
        if (this.bodyBuffer != null) {
            return this.bodyBuffer.toString();
        }
        return null;
    }

    @Override
    public String getPage() {
        if (this.pageBuffer != null) {
            return this.pageBuffer.toString();
        }
        return null;
    }

    public int originalLength() {
        return this.pageBuffer.size();
    }

    public void writeOriginal(Writer writer) throws IOException {
        writePage(writer);
    }

    public void setHeadBuffer(StreamCharBuffer headBuffer) {
        this.headBuffer = headBuffer;
        applyStreamCharBufferSettings(headBuffer);
        this.used = true;
    }

    private void applyStreamCharBufferSettings(StreamCharBuffer buffer) {
        if (!this.renderingLayout && buffer != null) {
            buffer.setPreferSubChunkWhenWritingToOtherBuffer(true);
        }
    }

    public void setBodyBuffer(StreamCharBuffer bodyBuffer) {
        this.bodyBuffer = bodyBuffer;
        applyStreamCharBufferSettings(bodyBuffer);
        this.used = true;
    }

    public void setPageBuffer(StreamCharBuffer pageBuffer) {
        this.pageBuffer = pageBuffer;
        applyStreamCharBufferSettings(pageBuffer);
    }

    public void setTitleBuffer(StreamCharBuffer titleBuffer) {
        this.titleBuffer = titleBuffer;
        applyStreamCharBufferSettings(titleBuffer);
    }

    public StreamCharBuffer getTitleBuffer() {
        return this.titleBuffer;
    }

    public boolean isUsed() {
        return this.used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * @param tagName "tagName" name of buffer (without "page." prefix)
     * @param buffer
     */
    public void setContentBuffer(String tagName, StreamCharBuffer buffer) {
        this.used = true;
        if (this.contentBuffers == null) {
            this.contentBuffers = new HashMap<String, StreamCharBuffer>();
        }
        String propertyName = "page." + tagName;
        this.contentBuffers.put(propertyName, buffer);
        // just mark that the property is set
        super.addProperty(propertyName, "");
    }

    /**
     * @param name propertyName of contentBuffer (with "page." prefix)
     * @return the buffer for the specified name
     */
    public Object getContentBuffer(String name) {
        if (this.contentBuffers == null) {
            return null;
        }
        return this.contentBuffers.get(name);
    }

    public static HTMLPage content2htmlPage(Content content) {
        HTMLPage htmlPage = null;
        if (content instanceof HTMLPage) {
            htmlPage = (HTMLPage) content;
        }
        else if (content instanceof TokenizedHTMLPage2Content) {
            htmlPage = ((TokenizedHTMLPage2Content) content).getPage();
        }
        else {
            htmlPage = new Content2HTMLPage(content, null);
        }
        return htmlPage;
    }

    public boolean isTitleCaptured() {
        return this.titleCaptured;
    }

    public void setTitleCaptured(boolean titleCaptured) {
        this.titleCaptured = titleCaptured;
    }

}
