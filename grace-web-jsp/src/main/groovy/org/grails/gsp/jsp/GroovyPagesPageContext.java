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
package org.grails.gsp.jsp;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import jakarta.el.ELContext;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspApplicationContext;
import jakarta.servlet.jsp.JspContext;
import jakarta.servlet.jsp.JspFactory;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.el.ELException;
import jakarta.servlet.jsp.el.ExpressionEvaluator;
import jakarta.servlet.jsp.el.VariableResolver;
import jakarta.servlet.jsp.tagext.BodyContent;

import groovy.lang.Binding;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.RequestContextHolder;

import org.grails.gsp.GroovyPage;
import org.grails.web.servlet.mvc.GrailsWebRequest;

/**
 * A JSP PageContext implementation for use with GSP.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class GroovyPagesPageContext extends PageContext {

    private final ServletContext servletContext;

    private final Servlet servlet;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final ServletConfig servletconfig;

    private final Binding pageScope;

    private final GrailsWebRequest webRequest;

    private JspWriter jspOut;

    private final Deque<JspWriter> outStack = new ArrayDeque<>();

    private final List<Object> tags = new ArrayList<>();

    private final HttpSession session;

    public GroovyPagesPageContext(Servlet pagesServlet, Binding pageScope) {
        Assert.notNull(pagesServlet, "GroovyPagesPageContext class requires a reference to the GSP servlet");
        this.webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();

        this.servletContext = this.webRequest.getServletContext();
        this.request = this.webRequest.getCurrentRequest();
        this.response = this.webRequest.getCurrentResponse();
        this.servlet = pagesServlet;
        this.servletconfig = pagesServlet.getServletConfig();
        this.pageScope = pageScope;
        this.session = this.request.getSession(false);
        // setup initial writer
        pushWriter(new JspWriterDelegate(this.webRequest.getOut()));
        // Register page attributes as per JSP spec
        setAttribute(REQUEST, this.request);
        setAttribute(RESPONSE, this.response);
        if (this.session != null) {
            setAttribute(SESSION, this.session);
        }
        setAttribute(PAGE, this.servlet);
        setAttribute(CONFIG, this.servlet.getServletConfig());
        setAttribute(PAGECONTEXT, this);
        setAttribute(APPLICATION, this.servletContext);
    }

    void popWriter() {
        this.outStack.pop();
        this.jspOut = this.outStack.peek();
        setCurrentOut();
    }

    void pushWriter(JspWriter out) {
        this.outStack.push(out);
        this.jspOut = out;
        setCurrentOut();
    }

    private void setCurrentOut() {
        setAttribute(OUT, this.jspOut);
        setAttribute(GroovyPage.OUT, this.jspOut);
        this.webRequest.setOut(this.jspOut);
    }

    Object peekTopTag(Class<?> tagClass) {
        for (ListIterator<?> iter = this.tags.listIterator(this.tags.size()); iter.hasPrevious(); ) {
            Object tag = iter.previous();
            if (tagClass.isInstance(tag)) {
                return tag;
            }
        }
        return null;
    }

    void popTopTag() {
        this.tags.remove(this.tags.size() - 1);
    }

    void pushTopTag(Object tag) {
        this.tags.add(tag);
    }

    @Override
    public BodyContent pushBody() {
        BodyContent bc = new BodyContentImpl(getOut(), true);
        pushWriter(bc);
        return bc;
    }

    @Override
    public JspWriter popBody() {
        popWriter();
        return (JspWriter) getAttribute(OUT);
    }

    public GroovyPagesPageContext(Binding pageScope) {
        this(new GenericServlet() {
            @Override
            public ServletConfig getServletConfig() {
                return this;
            }

            @Override
            public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
                // do nothing;
            }
        }, pageScope != null ? pageScope : new Binding());
    }

    public GroovyPagesPageContext() {
        this(new Binding());
    }

    @Override
    public void initialize(Servlet s, ServletRequest servletRequest, ServletResponse servletResponse,
            String errorPageURL, boolean needSession, int bufferSize, boolean autoFlush) {
        // do nothing, not constructed for container
    }

    @Override
    public void release() {
        // do nothing, not released by container
    }

    @Override
    public HttpSession getSession() {
        return this.request.getSession(false);
    }

    @Override
    public Object getPage() {
        return this.servlet;
    }

    @Override
    public ServletRequest getRequest() {
        return this.request;
    }

    @Override
    public ServletResponse getResponse() {
        return this.response;
    }

    @Override
    public Exception getException() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.servletconfig;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void forward(String url) throws ServletException, IOException {
        this.request.getRequestDispatcher(url).forward(this.request, this.response);
    }

    @Override
    public void include(String url) throws ServletException, IOException {
        include(url, false);
    }

    @Override
    public void include(String url, boolean flush) throws ServletException, IOException {
        this.request.getRequestDispatcher(url).include(this.request, this.response);
    }

    @Override
    public void handlePageException(Exception e) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handlePageException(Throwable throwable) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "Attribute name cannot be null");
        this.pageScope.setVariable(name, value);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        Assert.notNull(name, "Attribute name cannot be null");

        switch (scope) {
            case PAGE_SCOPE:
                setAttribute(name, value);
                break;
            case REQUEST_SCOPE:
                this.request.setAttribute(name, value);
                break;
            case SESSION_SCOPE:
                this.request.getSession(true).setAttribute(name, value);
                break;
            case APPLICATION_SCOPE:
                this.servletContext.setAttribute(name, value);
                break;
            default:
                setAttribute(name, value);
        }
    }

    @Override
    public Object getAttribute(String name) {
        Assert.notNull(name, "Attribute name cannot be null");

        if (this.pageScope.getVariables().containsKey(name)) {
            return this.pageScope.getVariable(name);
        }

        return null;
    }

    @Override
    public Object getAttribute(String name, int scope) {
        Assert.notNull(name, "Attribute name cannot be null");

        switch (scope) {
            case PAGE_SCOPE:
                return getAttribute(name);
            case REQUEST_SCOPE:
                return this.request.getAttribute(name);
            case SESSION_SCOPE:
                return this.request.getSession(true).getAttribute(name);
            case APPLICATION_SCOPE:
                return this.servletContext.getAttribute(name);
            default:
                return getAttribute(name);
        }
    }

    @Override
    public Object findAttribute(String name) {
        Assert.notNull(name, "Attribute name cannot be null");

        int scope = getAttributesScope(name);
        if (scope > 0) {
            return getAttribute(name, scope);
        }

        return null;
    }

    @Override
    public void removeAttribute(String name) {
        Assert.notNull(name, "Attribute name cannot be null");
        this.pageScope.getVariables().remove(name);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        Assert.notNull(name, "Attribute name cannot be null");

        switch (scope) {
            case PAGE_SCOPE:
                removeAttribute(name);
                break;
            case REQUEST_SCOPE:
                this.request.removeAttribute(name);
                break;
            case SESSION_SCOPE:
                HttpSession httpSession = this.request.getSession(false);
                if (httpSession != null) {
                    httpSession.removeAttribute(name);
                }
                break;
            case APPLICATION_SCOPE:
                this.servletContext.removeAttribute(name);
                break;
            default:
                removeAttribute(name);
        }
    }

    @Override
    public int getAttributesScope(String name) {
        Assert.notNull(name, "Attribute name cannot be null");

        if (this.pageScope.getVariables().containsKey(name)) {
            return PAGE_SCOPE;
        }

        if (this.request.getAttribute(name) != null) {
            return REQUEST_SCOPE;
        }

        HttpSession httpSession = this.request.getSession(false);
        if (httpSession != null && httpSession.getAttribute(name) != null) {
            return SESSION_SCOPE;
        }

        if (this.servletContext.getAttribute(name) != null) {
            return APPLICATION_SCOPE;
        }

        return 0;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        switch (scope) {
            case PAGE_SCOPE:
                final Iterator i = this.pageScope.getVariables().keySet().iterator();
                return new Enumeration() {
                    @Override
                    public boolean hasMoreElements() {
                        return i.hasNext();
                    }

                    @Override
                    public Object nextElement() {
                        return i.next();
                    }
                };
            case REQUEST_SCOPE:
                return this.request.getAttributeNames();
            case SESSION_SCOPE:
                HttpSession httpSession = this.request.getSession(false);
                if (httpSession != null) {
                    return httpSession.getAttributeNames();
                }
                else {
                    return Collections.emptyEnumeration();
                }
            case APPLICATION_SCOPE:
                return this.servletContext.getAttributeNames();
        }
        return Collections.emptyEnumeration();
    }

    @Override
    public JspWriter getOut() {
        Writer out = this.webRequest.getOut();
        if (out instanceof JspWriter) {
            return (JspWriter) out;
        }

        out = new JspWriterDelegate(out);
        this.webRequest.setOut(out);
        return (JspWriter) out;
    }

    @Override
    @Deprecated
    public ExpressionEvaluator getExpressionEvaluator() {
        try {
            Class<?> type = ClassUtils.forName("org.apache.commons.el.ExpressionEvaluatorImpl", getClass().getClassLoader());
            return (ExpressionEvaluator) ReflectionUtils.accessibleConstructor(type).newInstance();
        }
        catch (Exception e) {
            throw new UnsupportedOperationException("In order for the getExpressionEvaluator() " +
                    "method to work, you must have downloaded the apache commons-el jar and " +
                    "made it available in the classpath.");
        }
    }

    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        final PageContext ctx = this;
        return new VariableResolver() {
            public Object resolveVariable(String name) throws ELException {
                return ctx.findAttribute(name);
            }
        };
    }

    static {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new GroovyPagesJspFactory());
        }
    }

    private ELContext elContext;

    @Override
    public ELContext getELContext() {
        if (this.elContext == null) {
            JspApplicationContext jspContext = JspFactory.getDefaultFactory().getJspApplicationContext(getServletContext());
            if (jspContext instanceof GroovyPagesJspApplicationContext) {
                this.elContext = ((GroovyPagesJspApplicationContext) jspContext).createELContext(this);
                this.elContext.putContext(JspContext.class, this);
            }
            else {
                throw new IllegalStateException("Unable to create ELContext for a JspApplicationContext. "
                        + "It must be an instance of [GroovyPagesJspApplicationContext] do not override JspFactory.setDefaultFactory()!");
            }
        }
        return this.elContext;
    }

}
