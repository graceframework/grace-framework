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
package org.grails.gsp.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.grails.taglib.GrailsTagException;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Lexer for GroovyPagesServlet.
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 */
class GroovyPageScanner implements Tokens {

    private static final int DEFAULT_MAX_HTML_LENGTH = 64000;

    private String text;

    private int end1;

    private int begin1;

    private int end2;

    private int begin2;

    private int state = HTML;

    private int len;

    private String lastNamespace;

    private List<Integer> lineNumberPositions;

    private int lastLineNumberIndex = -1;

    private String pageName = "Unknown";

    private int maxHtmlLength = DEFAULT_MAX_HTML_LENGTH;

    private Pattern isTagNameSpacePattern = Pattern.compile("^\\p{Alpha}\\w*$");

    GroovyPageScanner(String text) {
        Strip strip = new Strip(text);
        strip.strip(0);
        this.text = strip.toString();
        this.len = this.text.length();
        resolveLineNumberPositions();
    }

    GroovyPageScanner(String gspSource, String uri) {
        this(gspSource);
        this.pageName = uri;
    }

    // add line starting positions to array
    private void resolveLineNumberPositions() {
        this.lineNumberPositions = new ArrayList<>();
        // first line starts at 0
        this.lineNumberPositions.add(0);
        for (int i = 0; i < this.len; i++) {
            if (this.text.charAt(i) == '\n') {
                // next line starts after LF
                this.lineNumberPositions.add(i + 1);
            }
        }
    }

    private int found(int newState, int skip) {
        this.begin2 = this.begin1;
        this.end2 = --this.end1;
        this.begin1 = this.end1 += skip;
        int lastState = this.state;
        this.state = newState;
        return lastState;
    }

    private int foundStartOrEndTag(int newState, int skip, String namespace) {
        this.begin2 = this.begin1;
        this.end2 = --this.end1;
        this.begin1 = this.end1 += skip;
        int lastState = this.state;
        this.state = newState;
        this.lastNamespace = namespace;
        return lastState;
    }

    String getToken() {
        return this.text.substring(this.begin2, this.end2);
    }

    int getLineNumberForToken() {
        for (int i = this.lastLineNumberIndex + 1; i < this.lineNumberPositions.size(); i++) {
            if (this.lineNumberPositions.get(i) > this.begin2) {
                this.lastLineNumberIndex = i - 1;
                return i;
            }
        }
        // unknown
        return 1;
    }

    String getNamespace() {
        return this.lastNamespace;
    }

    int nextToken() {
        for (; ; ) {
            int left = this.len - this.end1;
            if (left == 0) {
                this.end1++; // in order to include the last letter
                return found(EOF, 0);
            }
            char c = this.text.charAt(this.end1++);
            char c1 = left > 1 ? this.text.charAt(this.end1) : 0;
            char c2 = left > 2 ? this.text.charAt(this.end1 + 1) : 0;
            int tokenLength = this.end1 - this.begin1;

            switch (this.state) {
                case HTML:
                    if (isPotentialScriptletOrTag(c, left)) {
                        if (c1 == '%') {
                            if (c2 == '=') {
                                return found(JEXPR, 3);
                            }
                            if (c2 == '@') {
                                return found(JDIRECT, 3);
                            }
                            if (c2 == '!') {
                                return found(JDECLAR, 3);
                            }
                            if (isStartComment(c1, c2, left)) {
                                if (skipJComment()) {
                                    continue;
                                }
                            }
                            return found(JSCRIPT, 2);
                        }

                        boolean bStartTag = !isClosingTag(c1);

                        String tagNameSpace = getTagNamespace(bStartTag ? this.end1 : this.end1 + 1);
                        if (isTagDefinition(tagNameSpace)) {
                            if (bStartTag) {
                                return foundStartOrEndTag(GSTART_TAG, tagNameSpace.length() + 2, tagNameSpace);
                            }

                            return foundStartOrEndTag(GEND_TAG, tagNameSpace.length() + 3, tagNameSpace);
                        }
                    }
                    else if (isStartOfGExpression(c, c1)) {
                        return found(GEXPR, 2);
                    }

                    if (c == '%' && c1 == '{') {
                        if (c2 == '-' && left > 3 && this.text.charAt(this.end1 + 2) == '-') {
                            if (skipGComment()) {
                                continue;
                            }
                        }
                        return found(GSCRIPT, 2);
                    }

                    if (c == '!' && c1 == '{') {
                        return found(GDECLAR, 2);
                    }

                    if (c == '@' && c1 == '{') {
                        return found(GDIRECT, 2);
                    }

                    if (tokenLength > this.maxHtmlLength) {
                        return found(HTML, 0);
                    }

                    break;
                case JEXPR:
                case JSCRIPT:
                case JDIRECT:
                case JDECLAR:
                    if (c == '%' && c1 == '>') {
                        return found(HTML, 2);
                    }
                    break;
                case GSTART_TAG:
                    if (isStartOfGExpression(c, c1)) {
                        return found(GTAG_EXPR, 2);
                    }
                    if (c == '>') {
                        return found(HTML, 1);
                    }
                    else if (isClosingTag(c) && c1 == '>') {
                        return found(GEND_EMPTY_TAG, 1);
                    }
                    break;
                case GEND_TAG:
                case GEND_EMPTY_TAG:
                    if (c == '>') {
                        return found(HTML, 1);
                    }
                    break;
                case GTAG_EXPR:
                case GEXPR:
                    return parseExpression();
                case GDIRECT:
                    if (c == '}') {
                        return found(HTML, 1);
                    }
                    break;
                case GSCRIPT:
                    if (c == '}' && c1 == '%') {
                        return found(HTML, 2);
                    }
                    break;
                case GDECLAR:
                    if (c == '}' && (c1 == '!' || c1 == '%')) {
                        return found(HTML, 2);
                    }
                    break;
            }
        }
    }

    protected int parseExpression() {
        char terminationChar = '}';
        char nextTerminationChar = 0;
        boolean startInExpression = true;
        GroovyPageExpressionParser expressionParser =
                new GroovyPageExpressionParser(this.text, this.end1 - 1, terminationChar, nextTerminationChar, startInExpression);
        int endpos = expressionParser.parse();
        if (endpos != -1) {
            this.end1 = endpos + 1;
            int expressionEndState = HTML;
            if (this.state == GTAG_EXPR) {
                expressionEndState = GSTART_TAG;
            }
            return found(expressionEndState, nextTerminationChar == 0 ? 1 : 2);
        }

        throw new GrailsTagException("Unclosed GSP expression", this.pageName, getLineNumberForToken());
    }

    private boolean isClosingTag(char c1) {
        return c1 == '/';
    }

    private boolean isTagDefinition(String tagNameSpace) {
        return tagNameSpace != null && this.isTagNameSpacePattern.matcher(tagNameSpace).matches();
    }

    private String getTagNamespace(int fromIndex) {
        int foundColonIdx = this.text.indexOf(":", fromIndex);
        String tagNameSpace = null;
        if (foundColonIdx > -1) {
            tagNameSpace = this.text.substring(fromIndex, foundColonIdx);
        }
        return tagNameSpace;
    }

    private boolean isPotentialScriptletOrTag(char c, int left) {
        return c == '<' && left > 3;
    }

    private boolean isStartComment(char c1, char c2, int left) {
        return c1 == '%' && c2 == '-' && left > 3 && this.text.charAt(this.end1 + 2) == '-';
    }

    private boolean isStartOfGExpression(char c, char c1) {
        return c == '$' && c1 == '{';
    }

    private boolean skipComment(char c3, char c4) {
        int ix = this.end1 + 3;
        for (int ixz = this.len - 4; ; ix++) {
            if (ix > ixz) {
                return false;
            }
            if (this.text.charAt(ix) == '-' && this.text.charAt(ix + 1) == '-' && this.text.charAt(ix + 2) == c3 &&
                    this.text.charAt(ix + 3) == c4) {
                break;
            }
        }
        this.text = this.text.substring(0, --this.end1) + this.text.substring(ix + 4);
        this.len = this.text.length();
        return true;
    }

    private boolean skipGComment() {
        return skipComment('}', '%');
    }

    private boolean skipJComment() {
        return skipComment('%', '>');
    }

    void reset() {
        this.end1 = this.begin1 = this.end2 = this.begin2 = 0;
        this.state = HTML;
        this.lastNamespace = null;
        this.lastLineNumberIndex = -1;
    }

    public int getMaxHtmlLength() {
        return this.maxHtmlLength;
    }

    public void setMaxHtmlLength(int maxHtmlLength) {
        this.maxHtmlLength = maxHtmlLength;
    }

}
