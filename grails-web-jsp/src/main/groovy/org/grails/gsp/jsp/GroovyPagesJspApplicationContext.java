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
package org.grails.gsp.jsp;

import java.util.Iterator;
import java.util.LinkedList;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.el.ImplicitObjectELResolver;
import javax.servlet.jsp.el.ScopedAttributeELResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class GroovyPagesJspApplicationContext implements JspApplicationContext {

    private static final Log logger = LogFactory.getLog(GroovyPagesJspApplicationContext.class);

    private static final ExpressionFactory expressionFactoryImpl = findExpressionFactoryImplementation();

    private final LinkedList<ELContextListener> listeners = new LinkedList<>();

    private final CompositeELResolver elResolver = new CompositeELResolver();

    private final CompositeELResolver additionalResolvers = new CompositeELResolver();

    public GroovyPagesJspApplicationContext() {
        this.elResolver.add(new ImplicitObjectELResolver());
        this.elResolver.add(this.additionalResolvers);
        this.elResolver.add(new MapELResolver());
        this.elResolver.add(new ResourceBundleELResolver());
        this.elResolver.add(new ListELResolver());
        this.elResolver.add(new ArrayELResolver());
        this.elResolver.add(new BeanELResolver());
        this.elResolver.add(new ScopedAttributeELResolver());
    }

    private static ExpressionFactory findExpressionFactoryImplementation() {
        ExpressionFactory ef = tryExpressionFactoryImplementation("com.sun");
        if (ef == null) {
            ef = tryExpressionFactoryImplementation("org.apache");
            if (ef == null) {
                logger.warn("Could not find any implementation for " +
                        ExpressionFactory.class.getName());
            }
        }
        return ef;
    }

    private static ExpressionFactory tryExpressionFactoryImplementation(String packagePrefix) {
        String className = packagePrefix + ".el.ExpressionFactoryImpl";
        try {
            Class<?> cl = ClassUtils.forName(className, null);
            if (ExpressionFactory.class.isAssignableFrom(cl)) {
                logger.info("Using " + className + " as implementation of " +
                        ExpressionFactory.class.getName());
                return (ExpressionFactory) cl.newInstance();
            }
            logger.warn("Class " + className + " does not implement " +
                    ExpressionFactory.class.getName());
        }
        catch (ClassNotFoundException e) {
            // ignored
        }
        catch (Exception e) {
            logger.error("Failed to instantiate " + className, e);
        }
        return null;
    }

    public void addELResolver(ELResolver resolver) {
        this.additionalResolvers.add(resolver);
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactoryImpl;
    }

    public void addELContextListener(ELContextListener elContextListener) {
        synchronized (this.listeners) {
            this.listeners.addLast(elContextListener);
        }
    }

    ELContext createELContext(GroovyPagesPageContext pageCtx) {
        ELContext ctx = new GroovyPagesELContext(pageCtx);
        ELContextEvent event = new ELContextEvent(ctx);
        synchronized (this.listeners) {
            for (Iterator<ELContextListener> iter = this.listeners.iterator(); iter.hasNext(); ) {
                iter.next().contextCreated(event);
            }
        }
        return ctx;
    }

    private class GroovyPagesELContext extends ELContext {

        private GroovyPagesPageContext pageCtx;

        GroovyPagesELContext(GroovyPagesPageContext pageCtx) {
            this.pageCtx = pageCtx;
        }

        @Override
        public ELResolver getELResolver() {
            return GroovyPagesJspApplicationContext.this.elResolver;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return null;
        }

        @Override
        public VariableMapper getVariableMapper() {
            return new VariableMapper() {

                @Override
                public ValueExpression resolveVariable(String name) {
                    Object o = GroovyPagesELContext.this.pageCtx.findAttribute(name);
                    if (o == null) {
                        return null;
                    }
                    return expressionFactoryImpl.createValueExpression(o, o.getClass());
                }

                @Override
                public ValueExpression setVariable(String name, ValueExpression valueExpression) {
                    ValueExpression previous = resolveVariable(name);
                    GroovyPagesELContext.this.pageCtx.setAttribute(name, valueExpression.getValue(GroovyPagesELContext.this));
                    return previous;
                }
            };
        }

    }

}
