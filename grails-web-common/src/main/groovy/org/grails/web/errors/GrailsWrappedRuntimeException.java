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
package org.grails.web.errors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import grails.core.GrailsApplication;
import grails.util.GrailsStringUtils;

import org.grails.buffer.FastStringPrintWriter;
import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.core.artefact.ServiceArtefactHandler;
import org.grails.core.exceptions.GrailsException;
import org.grails.core.io.support.GrailsFactoriesLoader;
import org.grails.exceptions.reporting.SourceCodeAware;
import org.grails.gsp.ResourceAwareTemplateEngine;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.GrailsApplicationAttributes;

/**
 * Wraps a Grails RuntimeException and attempts to extract more relevent diagnostic messages
 * from the stack trace.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class GrailsWrappedRuntimeException extends GrailsException {

    private static final Class<? extends GrailsApplicationAttributes> grailsApplicationAttributesClass = GrailsFactoriesLoader.loadFactoryClasses(
            GrailsApplicationAttributes.class, GrailsWebRequest.class.getClassLoader()).get(0);

    private static final Constructor<? extends GrailsApplicationAttributes> grailsApplicationAttributesConstructor =
            ClassUtils.getConstructorIfAvailable(grailsApplicationAttributesClass, ServletContext.class);

    private static final long serialVersionUID = 7284065617154554366L;

    private static final Pattern ANY_GSP_DETAILS = Pattern.compile("_gsp.run");

    private static final Pattern PARSE_DETAILS_STEP1 = Pattern.compile("\\((\\w+)\\.groovy:(\\d+)\\)");

    private static final Pattern PARSE_DETAILS_STEP2 = Pattern.compile("at\\s{1}(\\w+)\\$_closure\\d+\\.doCall\\(\\1:(\\d+)\\)");

    private static final Pattern PARSE_GSP_DETAILS_STEP1 = Pattern.compile("_gsp\\.run\\(((\\w+?)_.*?):(\\d+)\\)");

    public static final String URL_PREFIX = "/WEB-INF/grails-app/";

    private static final Log LOG = LogFactory.getLog(GrailsWrappedRuntimeException.class);

    private String className = UNKNOWN;

    private int lineNumber = -1;

    private String stackTrace;

    private String[] codeSnippet = new String[0];

    private String gspFile;

    private Throwable cause;

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private String[] stackTraceLines;

    private static final String UNKNOWN = "Unknown";

    private String fileName;

    /**
     * @param servletContext The ServletContext instance
     * @param t The exception that was thrown
     */
    public GrailsWrappedRuntimeException(ServletContext servletContext, Throwable t) {
        super(t.getMessage(), t);
        this.cause = t;
        Throwable cause = t;

        FastStringPrintWriter pw = FastStringPrintWriter.newInstance();
        cause.printStackTrace(pw);
        this.stackTrace = pw.toString();

        while (cause.getCause() != cause) {
            if (cause.getCause() == null) {
                break;
            }
            cause = cause.getCause();
        }

        this.stackTraceLines = this.stackTrace.split("\\n");

        if (cause instanceof MultipleCompilationErrorsException) {
            MultipleCompilationErrorsException mcee = (MultipleCompilationErrorsException) cause;
            Object message = mcee.getErrorCollector().getErrors().iterator().next();
            if (message instanceof SyntaxErrorMessage) {
                SyntaxErrorMessage sem = (SyntaxErrorMessage) message;
                this.lineNumber = sem.getCause().getLine();
                this.className = sem.getCause().getSourceLocator();
                sem.write(pw);
            }
        }
        else {
            Matcher m1 = PARSE_DETAILS_STEP1.matcher(this.stackTrace);
            Matcher m2 = PARSE_DETAILS_STEP2.matcher(this.stackTrace);
            Matcher gsp = PARSE_GSP_DETAILS_STEP1.matcher(this.stackTrace);
            try {
                if (ANY_GSP_DETAILS.matcher(this.stackTrace).find() && gsp.find()) {
                    System.out.println(gsp.group(1) + " " + gsp.group(2) + " " + gsp.group(3));
                    this.className = gsp.group(1);
                    this.lineNumber = Integer.parseInt(gsp.group(3));
                    this.gspFile = URL_PREFIX + "views/" + gsp.group(2) + '/' + this.className;
                }
                else {
                    if (m1.find()) {
                        do {
                            this.className = m1.group(1);
                            this.lineNumber = Integer.parseInt(m1.group(2));
                        }
                        while (m1.find());
                    }
                    else {
                        while (m2.find()) {
                            this.className = m2.group(1);
                            this.lineNumber = Integer.parseInt(m2.group(2));
                        }
                    }
                }
            }
            catch (NumberFormatException ignored) {
            }
        }

        LineNumberReader reader = null;
        try {
            checkIfSourceCodeAware(t);
            checkIfSourceCodeAware(cause);

            if (getLineNumber() > -1) {
                String fileLocation;
                String url = null;

                if (this.fileName != null) {
                    fileLocation = this.fileName;
                }
                else {
                    String urlPrefix = "";
                    if (this.gspFile == null) {
                        this.fileName = this.className.replace('.', '/') + ".groovy";

                        GrailsApplication application = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
                                .getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
                        // @todo Refactor this to get the urlPrefix from the ArtefactHandler
                        if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, this.className)) {
                            urlPrefix += "/controllers/";
                        }
                        else if (application.isArtefactOfType("TagLib", this.className)) {
                            urlPrefix += "/taglib/";
                        }
                        else if (application.isArtefactOfType(ServiceArtefactHandler.TYPE, this.className)) {
                            urlPrefix += "/services/";
                        }
                        url = URL_PREFIX + urlPrefix + this.fileName;
                    }
                    else {
                        url = this.gspFile;
                        GrailsApplicationAttributes attrs = null;
                        try {
                            attrs = grailsApplicationAttributesConstructor.newInstance(servletContext);
                        }
                        catch (Exception e) {
                            ReflectionUtils.rethrowRuntimeException(e);
                        }
                        ResourceAwareTemplateEngine engine = attrs.getPagesTemplateEngine();
                        this.lineNumber = engine.mapStackLineNumber(url, this.lineNumber);
                    }
                    fileLocation = "grails-app" + urlPrefix + this.fileName;
                }

                InputStream in = null;
                if (!GrailsStringUtils.isBlank(url)) {
                    in = servletContext.getResourceAsStream(url);
                    LOG.debug("Attempting to display code snippet found in url " + url);
                }
                if (in == null) {
                    Resource r = null;
                    try {
                        r = this.resolver.getResource(fileLocation);
                        in = r.getInputStream();
                    }
                    catch (Throwable e) {
                        r = this.resolver.getResource("file:" + fileLocation);
                        if (r.exists()) {
                            try {
                                in = r.getInputStream();
                            }
                            catch (IOException ignored) {
                            }
                        }
                    }
                }

                if (in != null) {
                    reader = new LineNumberReader(new InputStreamReader(in, "UTF-8"));
                    String currentLine = reader.readLine();
                    StringBuilder buf = new StringBuilder();
                    while (currentLine != null) {
                        int currentLineNumber = reader.getLineNumber();
                        if ((this.lineNumber > 0 && currentLineNumber == this.lineNumber - 1) ||
                                (currentLineNumber == this.lineNumber)) {
                            buf.append(currentLineNumber)
                                    .append(": ")
                                    .append(currentLine)
                                    .append("\n");
                        }
                        else if (currentLineNumber == this.lineNumber + 1) {
                            buf.append(currentLineNumber)
                                    .append(": ")
                                    .append(currentLine);
                            break;
                        }
                        currentLine = reader.readLine();
                    }
                    this.codeSnippet = buf.toString().split("\n");
                }
            }
        }
        catch (IOException e) {
            LOG.warn("[GrailsWrappedRuntimeException] I/O error reading line diagnostics: " + e.getMessage(), e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ignored) {
                }
            }
        }
    }

    private void checkIfSourceCodeAware(Throwable t) {
        if (!(t instanceof SourceCodeAware)) {
            return;
        }

        final SourceCodeAware codeAware = (SourceCodeAware) t;
        if (codeAware.getFileName() != null) {
            this.fileName = codeAware.getFileName();
            if (this.className == null || UNKNOWN.equals(this.className)) {
                this.className = codeAware.getFileName();
            }
        }
        if (codeAware.getLineNumber() > -1) {
            this.lineNumber = codeAware.getLineNumber();
        }
    }

    /**
     * @return Returns the line.
     */
    public String[] getCodeSnippet() {
        return this.codeSnippet;
    }

    /**
     * @return Returns the className.
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * @return Returns the lineNumber.
     */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /**
     * @return Returns the stackTrace.
     */
    public String getStackTraceText() {
        return this.stackTrace;
    }

    /**
     * @return Returns the stackTrace lines
     */
    public String[] getStackTraceLines() {
        return this.stackTraceLines;
    }

    /* (non-Javadoc)
     * @see groovy.lang.GroovyRuntimeException#getMessage()
     */
    @Override
    public String getMessage() {
        return this.cause.getMessage();
    }

}
