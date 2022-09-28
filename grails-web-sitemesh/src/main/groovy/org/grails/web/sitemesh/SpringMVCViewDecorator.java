/*
 * Copyright 2011-2022 the original author or authors.
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
package org.grails.web.sitemesh;

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.module.sitemesh.mapper.DefaultDecorator;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.SiteMeshContext;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import groovy.text.Template;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import org.grails.web.servlet.view.AbstractGrailsView;
import org.grails.web.util.GrailsApplicationAttributes;

/**
 * Encapsulates the logic for rendering a layout.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class SpringMVCViewDecorator extends DefaultDecorator implements com.opensymphony.sitemesh.Decorator {

    private View view;

    public SpringMVCViewDecorator(String name, View view) {
        super(name, (view instanceof AbstractUrlBasedView) ? ((AbstractUrlBasedView) view).getUrl() : view.toString(), Collections.emptyMap());
        this.view = view;
    }

    public void render(Content content, SiteMeshContext context) {
        SiteMeshWebAppContext ctx = (SiteMeshWebAppContext) context;
        render(content, Collections.emptyMap(), ctx.getRequest(), ctx.getResponse(), ctx.getServletContext());
    }

    public void render(Content content, Map<String, ?> model, HttpServletRequest request,
            HttpServletResponse response, ServletContext servletContext) {
        HTMLPage htmlPage = GSPSitemeshPage.content2htmlPage(content);
        request.setAttribute(RequestConstants.PAGE, htmlPage);

        // get the dispatcher for the decorator
        if (!response.isCommitted()) {
            boolean dispatched = false;
            try {
                request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, new GSPSitemeshPage(true));
                try {
                    this.view.render(model, request, response);
                    dispatched = true;
                    if (!response.isCommitted()) {
                        response.getWriter().flush();
                    }
                }
                catch (Exception e) {
                    cleanRequestAttributes(request);
                    String message = "Error applying layout : " + getName();
                    if (this.view instanceof AbstractGrailsView) {
                        ((AbstractGrailsView) this.view).rethrowRenderException(e, message);
                    }
                    else {
                        throw new RuntimeException(message, e);
                    }
                }
            }
            finally {
                if (!dispatched) {
                    cleanRequestAttributes(request);
                }
            }
        }

        request.removeAttribute(RequestConstants.PAGE);
        request.removeAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE);
    }

    private void cleanRequestAttributes(HttpServletRequest request) {
        request.removeAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
        request.removeAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE);
    }

    public View getView() {
        return this.view;
    }

    public Template getTemplate() {
        if (this.view instanceof AbstractGrailsView) {
            return ((AbstractGrailsView) this.view).getTemplate();
        }
        return null;
    }

}
