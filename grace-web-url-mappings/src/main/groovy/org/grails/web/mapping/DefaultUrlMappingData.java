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
package org.grails.web.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

import grails.web.mapping.UrlMapping;
import grails.web.mapping.UrlMappingData;

/**
 * Default implementating of the UrlMappingData interface.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class DefaultUrlMappingData implements UrlMappingData {

    private static final String CAPTURED_WILDCARD = "(*)";

    private static final String CAPTURED_DOUBLE_WILDCARD = "(**)";

    private static final String QUESTION_MARK = "?";

    private static final String SLASH = "/";

    private final String urlPattern;

    private final String[] logicalUrls;

    private final String[] tokens;

    private List<Boolean> optionalTokens = new ArrayList<>();

    private boolean hasOptionalExtension;

    public DefaultUrlMappingData(String urlPattern) {
        Assert.hasLength(urlPattern, "Argument [urlPattern] cannot be null or blank");
        Assert.isTrue(urlPattern.startsWith(SLASH), "Argument [urlPattern] is not a valid URL. It must start with '/' !");

        String configuredPattern = configureUrlPattern(urlPattern);
        this.urlPattern = configuredPattern;
        this.tokens = tokenizeUrlPattern(configuredPattern);
        List<String> urls = new ArrayList<>();
        parseUrls(urls, this.tokens, this.optionalTokens);

        this.logicalUrls = urls.toArray(new String[0]);
    }

    @Override
    public boolean hasOptionalExtension() {
        return this.hasOptionalExtension;
    }

    private String[] tokenizeUrlPattern(String urlPattern) {
        // remove starting / and split
        return urlPattern.substring(1).split(SLASH);
    }

    private String configureUrlPattern(String urlPattern) {
        return urlPattern.replace("(*)**", CAPTURED_DOUBLE_WILDCARD);
    }

    private DefaultUrlMappingData(String urlPattern, String[] logicalUrls, String[] tokens, List<Boolean> optionalTokens) {
        this.urlPattern = urlPattern;
        this.logicalUrls = logicalUrls;
        this.tokens = tokens;
        this.optionalTokens = optionalTokens;
    }

    private void parseUrls(List<String> urls, String[] tokens, List<Boolean> optionalTokens) {
        StringBuilder buf = new StringBuilder();

        String optionalExtensionPattern = UrlMapping.OPTIONAL_EXTENSION_WILDCARD + '?';
        String optionalExtension = null;

        if (tokens.length > 0) {
            String lastToken = tokens[tokens.length - 1];
            this.hasOptionalExtension = lastToken.endsWith(optionalExtensionPattern);
            if (this.hasOptionalExtension) {
                int i = lastToken.indexOf(optionalExtensionPattern);
                optionalExtension = lastToken.substring(i, lastToken.length());
                tokens[tokens.length - 1] = lastToken.substring(0, i);
            }

        }

        for (String s : tokens) {
            String token = s.trim();
            if (token.equals(SLASH)) {
                continue;
            }

            boolean isOptional = false;
            if (token.endsWith(QUESTION_MARK)) {
                if (optionalExtension != null) {
                    urls.add(buf + optionalExtension);
                }
                else {
                    urls.add(buf.toString());
                }
                buf.append(SLASH).append(token);
                isOptional = true;
            }
            else {
                buf.append(SLASH).append(token);
            }
            if (CAPTURED_WILDCARD.equals(token)) {
                if (isOptional) {
                    optionalTokens.add(Boolean.TRUE);
                }
                else {
                    optionalTokens.add(Boolean.FALSE);
                }
            }
            if (CAPTURED_DOUBLE_WILDCARD.equals(token)) {
                optionalTokens.add(Boolean.TRUE);
            }
        }
        if (optionalExtension != null) {
            urls.add(buf + optionalExtension);
        }
        else {
            urls.add(buf.toString());
        }

        Collections.reverse(urls);
    }

    public String[] getTokens() {
        return this.tokens;
    }

    public String[] getLogicalUrls() {
        return this.logicalUrls;
    }

    public String getUrlPattern() {
        return this.urlPattern;
    }

    public boolean isOptional(int index) {
        if (index >= this.optionalTokens.size()) {
            return true;
        }
        return this.optionalTokens.get(index).equals(Boolean.TRUE);
    }

    @Override
    public UrlMappingData createRelative(String path) {
        Assert.hasLength(path, "Argument [path] cannot be null or blank");

        String newPattern = this.urlPattern + configureUrlPattern(path);

        String[] tokens = tokenizeUrlPattern(newPattern);
        List<String> urls = new ArrayList<>();
        List<Boolean> optionalTokens = new ArrayList<>();
        parseUrls(urls, tokens, optionalTokens);
        String[] logicalUrls = urls.toArray(new String[0]);

        return new DefaultUrlMappingData(newPattern, logicalUrls, tokens, optionalTokens);
    }

}
