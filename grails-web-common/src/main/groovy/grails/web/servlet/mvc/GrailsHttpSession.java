/*
 * Copyright 2004-2023 the original author or authors.
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
package grails.web.servlet.mvc;

import java.util.Enumeration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * An adapter class that takes a regular HttpSession and allows you to access it like a Groovy map.
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class GrailsHttpSession implements HttpSession {

    private HttpSession adaptee;

    private final HttpServletRequest request;

    public GrailsHttpSession(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Object getAttribute(String name) {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getAttribute(name);
        }
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getAttributeNames();
        }
    }

    @Override
    public long getCreationTime() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getCreationTime();
        }
    }

    @Override
    public String getId() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getId();
        }
    }

    @Override
    public long getLastAccessedTime() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getLastAccessedTime();
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getMaxInactiveInterval();
        }
    }

    @Override
    public ServletContext getServletContext() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.getServletContext();
        }
    }

    @Override
    public void invalidate() {
        createSessionIfNecessary();
        synchronized (this) {
            this.adaptee.invalidate();
        }
    }

    @Override
    public boolean isNew() {
        createSessionIfNecessary();
        synchronized (this) {
            return this.adaptee.isNew();
        }
    }

    @Override
    public void removeAttribute(String name) {
        createSessionIfNecessary();
        synchronized (this) {
            this.adaptee.removeAttribute(name);
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        createSessionIfNecessary();
        synchronized (this) {
            this.adaptee.setAttribute(name, value);
        }
    }

    @Override
    public void setMaxInactiveInterval(int arg0) {
        createSessionIfNecessary();
        synchronized (this) {
            this.adaptee.setMaxInactiveInterval(arg0);
        }
    }

    @Override
    public String toString() {
        createSessionIfNecessary();
        StringBuilder sb = new StringBuilder("Session Content:\n");
        Enumeration<String> e = this.adaptee.getAttributeNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            sb.append("  ");
            sb.append(name);
            sb.append(" = ");
            sb.append(this.adaptee.getAttribute(name));
            sb.append('\n');
        }
        return sb.toString();
    }

    private void createSessionIfNecessary() {
        if (this.adaptee == null) {
            this.adaptee = this.request.getSession(true);
        }
    }

}
