/*
 * Copyright 2004-2025 the original author or authors.
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
package org.grails.web.errors;

import jakarta.servlet.ServletContext;

import org.grails.core.exceptions.GrailsException;

/**
 * Wraps a Grails RuntimeException and attempts to extract more relevant diagnostic messages
 * from the stack trace.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.1
 */
public class GrailsWrappedRuntimeException extends GrailsException {

    private static final long serialVersionUID = 7284065617154554366L;

    private final Throwable cause;

    /**
     * @param servletContext The ServletContext instance
     * @param t The exception that was thrown
     */
    public GrailsWrappedRuntimeException(ServletContext servletContext, Throwable t) {
        super(t.getMessage(), t);
        this.cause = t;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }

    @Override
    public String getMessage() {
        return this.cause.getMessage();
    }

}
