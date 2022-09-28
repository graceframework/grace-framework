/*
 * Copyright 2013-2022 the original author or authors.
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

import java.util.Stack;

/**
 * Parses an expression in a GSP.
 *
 * Used by GroovyPageScanner and GroovyPageParser to search for the end of an expression.
 *
 * @author Lari Hotari
 */
class GroovyPageExpressionParser {

    private enum ParsingState {
        NORMAL, EXPRESSION, QUOTEDVALUE_SINGLE, QUOTEDVALUE_DOUBLE, TRIPLEQUOTED_SINGLE, TRIPLEQUOTED_DOUBLE;
    }

    String scriptTokens;

    int startPos;

    char terminationChar;

    char nextTerminationChar;

    Stack<ParsingState> parsingStateStack = new Stack<>();

    boolean containsGstrings = false;

    int terminationCharPos = -1;

    int relativeCharIndex = 0;

    GroovyPageExpressionParser(String scriptTokens, int startPos, char terminationChar,
            char nextTerminationChar, boolean startInExpression) {
        this.scriptTokens = scriptTokens;
        this.startPos = startPos;
        this.terminationChar = terminationChar;
        this.nextTerminationChar = nextTerminationChar;
        if (startInExpression) {
            this.parsingStateStack.push(ParsingState.EXPRESSION);
        }
        else {
            this.parsingStateStack.push(ParsingState.NORMAL);
        }
    }

    /**
     * Finds the ending position of an expression.
     *
     * @return end position of expression
     */
    int parse() {
        int currentPos = this.startPos;
        char previousChar = 0;
        char previousPreviousChar = 0;

        while (currentPos < this.scriptTokens.length() && this.terminationCharPos == -1) {
            ParsingState parsingState = this.parsingStateStack.peek();
            char ch = this.scriptTokens.charAt(currentPos++);
            char nextChar = (currentPos < this.scriptTokens.length()) ? this.scriptTokens.charAt(currentPos) : 0;

            if (this.parsingStateStack.size() == 1 && ch == this.terminationChar
                    && (this.nextTerminationChar == 0 || this.nextTerminationChar == nextChar)) {
                this.terminationCharPos = currentPos - 1;
            }
            else if (parsingState == ParsingState.EXPRESSION || parsingState == ParsingState.NORMAL) {
                switch (ch) {
                    case '{':
                        if (previousChar == '$' && parsingState == ParsingState.EXPRESSION) {
                            // invalid expression, starting new ${} expression inside expression
                            return -1;
                        }
                        if (previousChar == '$' || parsingState == ParsingState.EXPRESSION) {
                            changeState(ParsingState.EXPRESSION);
                        }
                        break;
                    case '[':
                        if (this.relativeCharIndex == 0 || parsingState == ParsingState.EXPRESSION) {
                            changeState(ParsingState.EXPRESSION);
                        }
                        break;
                    case '}':
                    case ']':
                        if (parsingState == ParsingState.EXPRESSION) {
                            this.parsingStateStack.pop();
                        }
                        break;
                    case '\'':
                    case '"':
                        if (parsingState == ParsingState.EXPRESSION) {
                            if (nextChar != ch && previousChar != ch) {
                                changeState(ch == '"' ? ParsingState.QUOTEDVALUE_DOUBLE : ParsingState.QUOTEDVALUE_SINGLE);
                            }
                            else if (previousChar == ch && previousPreviousChar == ch) {
                                changeState(ch == '"' ? ParsingState.TRIPLEQUOTED_DOUBLE : ParsingState.TRIPLEQUOTED_SINGLE);
                            }
                        }
                        break;
                }
            }
            else if (ch == '"' || ch == '\'') {
                if (nextChar != ch && (previousChar != ch || previousPreviousChar == '\\')
                        && (previousChar != '\\' || (previousChar == '\\' && previousPreviousChar == '\\'))
                        && ((parsingState == ParsingState.QUOTEDVALUE_DOUBLE && ch == '"')
                        || (parsingState == ParsingState.QUOTEDVALUE_SINGLE && ch == '\''))) {
                    this.parsingStateStack.pop();
                }
                else if ((previousChar == ch && previousPreviousChar == ch)
                        && ((parsingState == ParsingState.TRIPLEQUOTED_DOUBLE && ch == '"')
                        || (parsingState == ParsingState.TRIPLEQUOTED_SINGLE && ch == '\''))) {
                    this.parsingStateStack.pop();
                }
            }
            previousPreviousChar = previousChar;
            previousChar = ch;
            this.relativeCharIndex++;
        }
        return this.terminationCharPos;
    }

    private void changeState(ParsingState newState) {
        ParsingState currentState = this.parsingStateStack.peek();
        // check if expression contains GStrings
        if (this.relativeCharIndex > 1 && newState == ParsingState.EXPRESSION
                && (currentState == ParsingState.QUOTEDVALUE_DOUBLE
                || currentState == ParsingState.TRIPLEQUOTED_DOUBLE || currentState == ParsingState.NORMAL)) {
            this.containsGstrings = true;
        }
        this.parsingStateStack.push(newState);
    }

    public boolean isContainsGstrings() {
        return this.containsGstrings;
    }

    public int getTerminationCharPos() {
        return this.terminationCharPos;
    }

}
