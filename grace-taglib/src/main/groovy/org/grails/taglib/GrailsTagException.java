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
package org.grails.taglib;

import org.grails.core.exceptions.GrailsException;
import org.grails.exceptions.reporting.SourceCodeAware;
import org.grails.io.support.GrailsResourceUtils;

/**
 * @author Graeme Rocher
 */
public class GrailsTagException extends GrailsException implements SourceCodeAware {

    private static final long serialVersionUID = -2340187595590923592L;

    private String fileName;

    private int lineNumber;

    public GrailsTagException(String message) {
        super(message);
    }

    public GrailsTagException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrailsTagException(Throwable cause) {
        super(cause);
    }

    public GrailsTagException(String message, String pageName, int lineNumber) {
        this(message, null, pageName, lineNumber);
    }

    public GrailsTagException(String message, Throwable cause, String fileName, int lineNumber) {
        super(message, cause);
        String path = GrailsResourceUtils.getPathFromBaseDir(fileName);
        this.fileName = path != null ? path : fileName;
        this.lineNumber = lineNumber;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public String getMessage() {
        String fn = getFileName();
        int ln = getLineNumber();
        if (fn != null && ln > 0) {
            return "[" + fn + ":" + ln + "] " + super.getMessage();
        }
        else {
            return super.getMessage();
        }
    }

}
