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
package org.grails.web.json;

import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO Proof of concept
 * Should capture the JSON Path to the current element
 *
 * @author Siegfried Puchbauer
 */
public class PathCapturingJSONWriterWrapper extends JSONWriter {

    private static final Log log = LogFactory.getLog(PathCapturingJSONWriterWrapper.class);

    private final boolean debugCurrentStack = true;

    private JSONWriter delegate;

    private Stack<PathElement> pathStack = new Stack<>();

    public PathCapturingJSONWriterWrapper(JSONWriter delegate) {
        super(null);
        this.delegate = delegate;
    }

    @Override
    public JSONWriter append(String s) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("append(%s)", s));
        }
        this.delegate.append(s);
        return this;
    }

    @Override
    public void comma() {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug("comma()");
        }
        this.delegate.comma();
    }

    @Override
    public JSONWriter array() {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("array()"));
        }
        this.pathStack.push(new IndexElement(-1));
        this.delegate.array();
        return this;
    }

    @Override
    public JSONWriter end(Mode m, char c) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("end(%s, %s)", m, c));
        }
        this.delegate.end(m, c);
        return this;
    }

    @Override
    public JSONWriter endArray() {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("endArray()"));
        }
        this.pathStack.pop();
        this.delegate.endArray();
        if (this.delegate.mode == Mode.KEY) {
            this.pathStack.pop();
        }
        return this;
    }

    @Override
    public JSONWriter endObject() {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("endObject()"));
        }
        this.delegate.endObject();
        if (this.delegate.mode != Mode.ARRAY && this.pathStack.size() > 0) {
            this.pathStack.pop();
        }
        return this;
    }

    @Override
    public JSONWriter key(String s) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("key(%s)", s));
        }
        this.pathStack.push(new PropertyElement(s));
        this.delegate.key(s);
        return this;
    }

    @Override
    public JSONWriter object() {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("object()"));
        }
        if (this.delegate.mode == Mode.ARRAY) {
            pushNextIndex();
        }
        this.delegate.object();
        return this;
    }

    @Override
    public void pop(Mode c) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("pop(%s)", c));
        }
        this.delegate.pop(c);
    }

    @Override
    public void push(Mode c) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("push(%s)", c));
        }
        this.delegate.push(c);
    }

    private void pushNextIndex() {
        int x = nextIndex();
        this.pathStack.pop();
        this.pathStack.push(new IndexElement(x));
    }

    private int nextIndex() {
        int x = ((IndexElement) this.pathStack.peek()).index + 1;
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("Next index: " + x));
        }
        return x;
    }

    @Override
    public JSONWriter value(boolean b) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("value(boolean %b)", b));
        }
        if (this.delegate.mode == Mode.ARRAY) {
            pushNextIndex();
        }
        else {
            this.pathStack.pop();
        }
        this.delegate.value(b);
        return this;
    }

    @Override
    public JSONWriter value(double d) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("value(double %s)", d));
        }
        if (this.delegate.mode == Mode.ARRAY) {
            pushNextIndex();
        }
        else {
            this.pathStack.pop();
        }
        this.delegate.value(d);
        return this;
    }

    @Override
    public JSONWriter value(long l) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("value(long %s)", l));
        }
        if (this.delegate.mode == Mode.ARRAY) {
            pushNextIndex();
        }
        else {
            this.pathStack.pop();
        }
        this.delegate.value(l);
        return this;
    }

    @Override
    public JSONWriter value(Object o) {
        if (log.isDebugEnabled()) {
            if (this.debugCurrentStack) {
                log.debug(this.delegate.mode.name() + " > " + String.format(">> " + getCurrentStrackReference()));
            }
            log.debug(this.delegate.mode.name() + " > " + String.format("value(Object %s)", o));
        }

        if (this.delegate.mode == Mode.ARRAY) {
            pushNextIndex();
        }
        else {
            this.pathStack.pop();
        }
        this.delegate.value(o);
        return this;
    }

    private class PathElement {
        // ??
    }

    private final class PropertyElement extends PathElement {

        private String property;

        private PropertyElement(String property) {
            this.property = property;
        }

        @Override
        public String toString() {
            return "." + this.property;
        }

    }

    private final class IndexElement extends PathElement {

        private int index;

        private IndexElement(int index) {
            this.index = index;
        }

        @Override
        public String toString() {
            return "[" + this.index + "]";
        }

    }

    public String getStackReference(int depth) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            PathElement el = this.pathStack.get(i);
            out.append(el.toString());
        }
        return out.toString();
    }

    public String getCurrentStrackReference() {
        StringBuilder out = new StringBuilder();
        for (PathElement el : this.pathStack) {
            out.append(el.toString());
        }
        return out.toString();
    }

}
