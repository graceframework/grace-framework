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

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.PageParser;
import com.opensymphony.module.sitemesh.PageParserSelector;
import com.opensymphony.module.sitemesh.filter.HttpContentType;
import com.opensymphony.module.sitemesh.filter.RoutableServletOutputStream;
import com.opensymphony.module.sitemesh.filter.TextEncoder;

import org.grails.buffer.GrailsPrintWriterAdapter;
import org.grails.buffer.StreamByteBuffer;
import org.grails.buffer.StreamCharBuffer;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.WebUtils;

/**
 * @author Graeme Rocher
 * @since 1.0.4
 */
public class GrailsPageResponseWrapper extends HttpServletResponseWrapper {

    private final GrailsRoutablePrintWriter routablePrintWriter;

    private final RoutableServletOutputStream routableServletOutputStream;

    private final PageParserSelector parserSelector;

    private final HttpServletRequest request;

    private GrailsBuffer buffer;

    private boolean parseablePage = false;

    private GSPSitemeshPage gspSitemeshPage;

    public GrailsPageResponseWrapper(final HttpServletRequest request, final HttpServletResponse response,
            PageParserSelector parserSelector) {
        super(response);
        this.parserSelector = parserSelector;

        this.routablePrintWriter = GrailsRoutablePrintWriter.newInstance(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() throws IOException {
                return response.getWriter();
            }
        });
        this.routableServletOutputStream = new RoutableServletOutputStream(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                return response.getOutputStream();
            }
        });

        this.request = request;

        this.gspSitemeshPage = (GSPSitemeshPage) request.getAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE);

        applyContentType(response.getContentType());
    }

    @Override
    public void sendError(int sc) throws IOException {
        clearBuffer();
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        try {
            super.sendError(sc);
        }
        finally {
            if (webRequest != null) {
                WebUtils.storeGrailsWebRequest(webRequest);
            }
        }
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        clearBuffer();
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        try {
            super.sendError(sc, msg);
        }
        finally {
            if (webRequest != null) {
                WebUtils.storeGrailsWebRequest(webRequest);
            }
        }
    }

    /**
     * Set the content-type of the request and store it so it can
     * be passed to the {@link com.opensymphony.module.sitemesh.PageParser}.
     */
    @Override
    public void setContentType(String type) {
        super.setContentType(type);

        applyContentType(type);
    }

    protected void applyContentType(String type) {
        if (type == null) {
            return;
        }

        HttpContentType httpContentType = new HttpContentType(type);

        if (this.parserSelector.shouldParsePage(httpContentType.getType())) {
            activateSiteMesh(httpContentType.getType(), httpContentType.getEncoding());
        }
        else {
            deactivateSiteMesh();
        }
    }

    public void activateSiteMesh(String contentType, String encoding) {
        if (this.parseablePage) {
            return; // already activated
        }

        this.buffer = new GrailsBuffer(this.parserSelector, contentType, encoding, this.gspSitemeshPage);
        this.routablePrintWriter.updateDestination(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() {
                return GrailsPageResponseWrapper.this.buffer.getWriter();
            }
        });
        this.routablePrintWriter.blockFlushAndClose();
        this.routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() {
                return GrailsPageResponseWrapper.this.buffer.getOutputStream();
            }
        });
        this.parseablePage = true;
    }

    public void deactivateSiteMesh() {
        this.parseablePage = false;
        this.buffer = null;
        if (this.gspSitemeshPage != null) {
            this.gspSitemeshPage.reset();
        }
        this.routablePrintWriter.updateDestination(new GrailsRoutablePrintWriter.DestinationFactory() {
            public PrintWriter activateDestination() throws IOException {
                return getResponse().getWriter();
            }
        });
        this.routablePrintWriter.unBlockFlushAndClose();
        this.routableServletOutputStream.updateDestination(new RoutableServletOutputStream.DestinationFactory() {
            public ServletOutputStream create() throws IOException {
                return getResponse().getOutputStream();
            }
        });
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    @Override
    public void setContentLength(int contentLength) {
        if (!this.parseablePage) {
            super.setContentLength(contentLength);
        }
    }

    /**
     * Prevent buffer from being flushed if this is a page being parsed.
     */
    @Override
    public void flushBuffer() throws IOException {
        if (!this.parseablePage) {
            super.flushBuffer();
        }
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    @Override
    public void setHeader(String name, String value) {
        if (name.toLowerCase().equals("content-type")) { // ensure ContentType is always set through setContentType()
            setContentType(value);
        }
        else if (!this.parseablePage || !name.toLowerCase().equals("content-length")) {
            super.setHeader(name, value);
        }
    }

    /**
     * Prevent content-length being set if page is parseable.
     */
    @Override
    public void addHeader(String name, String value) {
        if (name.toLowerCase().equals("content-type")) { // ensure ContentType is always set through setContentType()
            setContentType(value);
        }
        else if (!this.parseablePage || !name.toLowerCase().equals("content-length")) {
            super.addHeader(name, value);
        }
    }

    /**
     * If 'not modified' (304) HTTP status is being sent - then abort parsing, as there shouldn't be any body
     */
    @Override
    public void setStatus(int sc) {
        if (sc == HttpServletResponse.SC_NOT_MODIFIED) {
            // route any content back to the original writer.  There shouldn't be any content, but just to be safe
            deactivateSiteMesh();
        }
        else if (sc >= 400) {
            clearBuffer();
        }
        super.setStatus(sc);
    }

    protected void clearBuffer() {
        if (this.buffer != null) {
            this.buffer.clear();
        }
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return this.routableServletOutputStream;
    }

    @Override
    public PrintWriter getWriter() {
        return this.routablePrintWriter;
    }

    public Page getPage() throws IOException {
        if (isSitemeshNotActive()) {
            return null;
        }

        GSPSitemeshPage page = (GSPSitemeshPage) this.request.getAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE);
        if (page != null && page.isUsed()) {
            return page;
        }

        return this.buffer.parse();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        clearBuffer();
        super.sendRedirect(location);
    }

    public boolean isUsingStream() {
        return this.buffer != null && this.buffer.isUsingStream();
    }

    public char[] getContents() throws IOException {
        if (isSitemeshNotActive()) {
            return null;
        }

        return this.buffer.getContents();
    }

    public boolean isSitemeshActive() {
        return !isSitemeshNotActive();
    }

    public boolean isGspSitemeshActive() {
        return (this.gspSitemeshPage != null && this.gspSitemeshPage.isUsed());
    }

    private boolean isSitemeshNotActive() {
        return !this.parseablePage;
    }

    private static class GrailsBuffer {

        private final PageParserSelector parserSelector;

        private final String contentType;

        private final String encoding;

        private static final TextEncoder TEXT_ENCODER = new TextEncoder();

        private StreamCharBuffer charBuffer;

        private GrailsPrintWriterAdapter exposedWriter;

        private StreamByteBuffer byteBuffer;

        private ServletOutputStream exposedStream;

        private GSPSitemeshPage gspSitemeshPage;

        GrailsBuffer(PageParserSelector parserSelector, String contentType, String encoding, GSPSitemeshPage gspSitemeshPage) {
            this.parserSelector = parserSelector;
            this.contentType = contentType;
            this.encoding = encoding;
            this.gspSitemeshPage = gspSitemeshPage;
        }

        public void clear() {
            if (this.charBuffer != null) {
                this.charBuffer.clear();
            }
            else if (this.byteBuffer != null) {
                this.byteBuffer.clear();
            }
            if (this.gspSitemeshPage != null) {
                this.gspSitemeshPage.reset();
            }
        }

        private char[] getContents() throws IOException {
            if (this.charBuffer != null) {
                if (!this.charBuffer.isEmpty()) {
                    return this.charBuffer.toCharArray();
                }
                else {
                    return null;
                }
            }
            if (this.byteBuffer != null) {
                return TEXT_ENCODER.encode(this.byteBuffer.readAsByteArray(), this.encoding);
            }

            return new char[0];
        }

        public Page parse() throws IOException {
            PageParser pageParser = this.parserSelector.getPageParser(this.contentType);
            return pageParser != null ? pageParser.parse(getContents()) : null;
        }

        public PrintWriter getWriter() {
            if (this.charBuffer == null) {
                if (this.byteBuffer != null) {
                    throw new IllegalStateException("response.getWriter() called after response.getOutputStream()");
                }
                this.charBuffer = new StreamCharBuffer();
                this.charBuffer.setNotifyParentBuffersEnabled(false);
                if (this.gspSitemeshPage != null) {
                    this.gspSitemeshPage.setPageBuffer(this.charBuffer);
                }
                this.exposedWriter = GrailsPrintWriterAdapter.newInstance(this.charBuffer.getWriter());
            }
            return this.exposedWriter;
        }

        public ServletOutputStream getOutputStream() {
            if (this.byteBuffer == null) {
                if (this.charBuffer != null) {
                    throw new IllegalStateException("response.getOutputStream() called after response.getWriter()");
                }
                this.byteBuffer = new StreamByteBuffer();
                final OutputStream out = this.byteBuffer.getOutputStream();
                this.exposedStream = new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        throw new UnsupportedOperationException("Method setWriteListener not supported");
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        out.write(b, off, len);
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        out.write(b);
                    }

                    @Override
                    public void write(int b) throws IOException {
                        out.write(b);
                    }
                };
            }
            return this.exposedStream;
        }

        public boolean isUsingStream() {
            return this.byteBuffer != null;
        }

    }

}
