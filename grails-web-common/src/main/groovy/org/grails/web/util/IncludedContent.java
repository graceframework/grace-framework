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
package org.grails.web.util;

import java.io.IOException;
import java.io.Writer;

import groovy.lang.Writable;

import grails.util.GrailsWebUtil;

import org.grails.buffer.StreamCharBuffer;

/**
 * Represents some content that has been used in an include request.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 */
public class IncludedContent implements Writable {

    private String contentType = GrailsWebUtil.getContentType("text/html", "UTF-8");

    private Object content;

    private String redirectURL;

    public IncludedContent(String contentType, Object content) {
        if (contentType != null) {
            this.contentType = contentType;
        }
        this.content = content;
    }

    public IncludedContent(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    /**
     * Returns the URL of a redirect if a redirect was issue in the Include
     * otherwise it returns null if there was no redirect.
     *
     * @return The redirect URL
     */
    public String getRedirectURL() {
        return this.redirectURL;
    }

    /**
     * Returns the included content type (default is text/html;charset=UTF=8)
     * @return The content type
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the included content
     * @return The content
     */
    public Object getContent() {
        return this.content;
    }

    public Writer writeTo(Writer target) throws IOException {
        if (this.content == null) {
            return target;
        }

        if (this.content instanceof StreamCharBuffer) {
            ((StreamCharBuffer) this.content).writeTo(target);
        }
        else if (this.content instanceof String) {
            target.write((String) this.content);
        }
        else {
            target.write(String.valueOf(this.content));
        }
        return target;
    }

    public char[] getContentAsCharArray() {
        if (this.content == null) {
            return new char[0];
        }

        if (this.content instanceof StreamCharBuffer) {
            return ((StreamCharBuffer) this.content).toCharArray();
        }

        if (this.content instanceof String) {
            return ((String) this.content).toCharArray();
        }

        return String.valueOf(this.content).toCharArray();
    }

}
