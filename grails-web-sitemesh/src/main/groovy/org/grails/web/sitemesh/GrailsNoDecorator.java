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
package org.grails.web.sitemesh;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import com.opensymphony.sitemesh.webapp.decorator.BaseWebAppDecorator;

/**
 * Grails version of Sitemesh's NoDecorator
 *
 * original version always calls response.setContentLength which would require the calculation of
 * resulting bytes. Calculation would be extra overhead.
 *
 * bug exists for OutputStream / byte version: http://jira.opensymphony.com/browse/SIM-196
 * skip setting ContentLength because of that bug.
 *
 * @author Lari Hotari, Sagire Software Oy
 */
public class GrailsNoDecorator extends BaseWebAppDecorator implements Decorator {

    @Override
    protected void render(Content content, HttpServletRequest request, HttpServletResponse response,
            ServletContext servletContext, SiteMeshWebAppContext webAppContext)
            throws IOException, ServletException {

        if (webAppContext.isUsingStream()) {
            // http://jira.opensymphony.com/browse/SIM-196 , skip setting setContentLength
            //response.setContentLength(content.originalLength());
            OutputStream output = response.getOutputStream();
            PrintWriter writer = new PrintWriter(output);
            content.writeOriginal(writer);
            writer.flush();
        }
        else {
            PrintWriter writer = response.getWriter();
            content.writeOriginal(writer);
            writer.flush();
        }
    }

    public String getPage() {
        return null;
    }

    public String getName() {
        return null;
    }

    public String getURIPath() {
        return null;
    }

    public String getRole() {
        return null;
    }

    public String getInitParameter(String paramName) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Iterator getInitParameterNames() {
        return null;
    }

}
