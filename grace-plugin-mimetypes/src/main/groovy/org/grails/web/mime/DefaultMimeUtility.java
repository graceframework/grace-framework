/*
 * Copyright 2011-2023 the original author or authors.
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
package org.grails.web.mime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import grails.web.mime.MimeType;
import grails.web.mime.MimeUtility;

/**
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultMimeUtility implements MimeUtility {

    private final List<MimeType> mimeTypes;

    private final Map<String, MimeType> extensionToMimeMap = new HashMap<>();

    public DefaultMimeUtility(MimeType[] mimeTypes) {
        this(Arrays.asList(mimeTypes));
    }

    public DefaultMimeUtility(List<MimeType> mimeTypes) {
        this.mimeTypes = mimeTypes;
        for (MimeType mimeType : mimeTypes) {
            String ext = mimeType.getExtension();
            if (!this.extensionToMimeMap.containsKey(ext)) {
                this.extensionToMimeMap.put(ext, mimeType);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MimeType> getKnownMimeTypes() {
        return this.mimeTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MimeType getMimeTypeForExtension(String extension) {
        if (extension == null) {
            return null;
        }
        return this.extensionToMimeMap.get(extension);
    }

    public MimeType getMimeTypeForURI(String uri) {
        if (uri == null) {
            return null;
        }

        int i = uri.lastIndexOf('.');
        int length = uri.length();
        if (i > -1) {
            String extension = uri.substring(i + 1, length);
            return getMimeTypeForExtension(extension);
        }

        return null;
    }

}
