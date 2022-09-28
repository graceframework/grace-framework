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
package org.grails.web.pages;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletResponse;

import com.opensymphony.module.sitemesh.RequestConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

import org.grails.buffer.StreamCharBuffer;
import org.grails.buffer.StreamCharBuffer.LazyInitializingWriter;
import org.grails.buffer.StreamCharBuffer.StreamCharBufferWriter;
import org.grails.encoder.EncodedAppender;
import org.grails.encoder.EncodedAppenderFactory;
import org.grails.encoder.Encoder;
import org.grails.encoder.EncoderAware;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.sitemesh.GrailsContentBufferingResponse;
import org.grails.web.sitemesh.GrailsRoutablePrintWriter;
import org.grails.web.util.BoundedCharsAsEncodedBytesCounter;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * A buffered writer that won't commit the response until the buffer has reached the high
 * water mark, or until flush() or close() is called.
 *
 * Performance optimizations by Lari Hotari, 13.03.2009
 *
 * Calculating the Content-Length has been disabled by default since Jetty ignores it (uses Chunked mode anyways).
 * Content-Length mode can be enabled with -DGSPResponseWriter.enableContentLength=true system property.
 *
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 * @author Lari Hotari, Sagire Software Oy
 *
 * Date: Jan 10, 2004
 */
public final class GSPResponseWriter extends GrailsRoutablePrintWriter implements EncoderAware, EncodedAppenderFactory {

    protected static final Log logger = LogFactory.getLog(GSPResponseWriter.class);

    private ServletResponse response;

    private BoundedCharsAsEncodedBytesCounter bytesCounter;

    public static final boolean CONTENT_LENGTH_COUNTING_ENABLED = Boolean.getBoolean("GSPResponseWriter.enableContentLength");

    public static final boolean BUFFERING_ENABLED = Boolean.valueOf(System.getProperty("GSPResponseWriter.enableBuffering", "true"));

    public static final boolean AUTOFLUSH_ENABLED = Boolean.getBoolean("GSPResponseWriter.enableAutoFlush");

    private static final int BUFFER_SIZE = Integer.getInteger("GSPResponseWriter.bufferSize", 8042);

    private Encoder encoder;

    private StreamCharBuffer buffer;

    private static ObjectInstantiator instantiator = null;

    static {
        try {
            instantiator = new ObjenesisStd(false).getInstantiatorOf(GSPResponseWriter.class);
        }
        catch (Exception e) {
            logger.debug("Couldn't get direct performance optimized instantiator for GSPResponseWriter. Using default instantiation.", e);
        }
    }

    /**
     * Private constructor.  Use getInstance() instead.
     * @param activeWriter buffered writer
     */
    private GSPResponseWriter(final Writer activeWriter) {
        super(null);
        initialize(activeWriter);
    }

    /**
     * Private constructor.  Use getInstance() instead.
     * @param buffer buffered writer
     * @param response The servlet response
     * @param bytesCounter    Keeps count of encoded bytes count
     */
    private GSPResponseWriter(final StreamCharBuffer buffer, final ServletResponse response, BoundedCharsAsEncodedBytesCounter bytesCounter) {
        super(null);

        initialize(buffer, response, bytesCounter);
    }

    public static GSPResponseWriter getInstance(final ServletResponse response) {
        return getInstance(response, BUFFER_SIZE);
    }

    /**
     * Static factory methdirectWritingod to create the writer.
     * @param response
     * @param max
     * @return A GSPResponseWriter instance
     */
    private static GSPResponseWriter getInstance(final ServletResponse response, final int max) {
        final BoundedCharsAsEncodedBytesCounter bytesCounter = new BoundedCharsAsEncodedBytesCounter();

        final StreamCharBuffer streamBuffer = new StreamCharBuffer(max, 0, max);
        streamBuffer.setChunkMinSize(max / 2);
        streamBuffer.setNotifyParentBuffersEnabled(false);

        final StreamCharBuffer.LazyInitializingWriter lazyResponseWriter = new StreamCharBuffer.LazyInitializingWriter() {
            public Writer getWriter() throws IOException {
                return response.getWriter();
            }
        };

        if (!(response instanceof GrailsContentBufferingResponse)) {
            streamBuffer.connectTo(new StreamCharBuffer.LazyInitializingMultipleWriter() {
                public Writer getWriter() throws IOException {
                    return null;
                }

                public LazyInitializingWriter[] initializeMultiple(StreamCharBuffer buffer, boolean autoFlush) throws IOException {
                    final StreamCharBuffer.LazyInitializingWriter[] lazyWriters;
                    if (CONTENT_LENGTH_COUNTING_ENABLED) {
                        lazyWriters = new StreamCharBuffer.LazyInitializingWriter[] { new StreamCharBuffer.LazyInitializingWriter() {
                            public Writer getWriter() throws IOException {
                                bytesCounter.setCapacity(max * 2);
                                bytesCounter.setEncoding(response.getCharacterEncoding());
                                return bytesCounter.getCountingWriter();
                            }
                        }, lazyResponseWriter };
                    }
                    else {
                        lazyWriters = new StreamCharBuffer.LazyInitializingWriter[] { lazyResponseWriter };
                    }
                    return lazyWriters;
                }
            }, AUTOFLUSH_ENABLED);
        }
        else {
            streamBuffer.connectTo(lazyResponseWriter);
        }

        if (instantiator != null) {
            GSPResponseWriter instance = (GSPResponseWriter) instantiator.newInstance();
            instance.initialize(streamBuffer, response, bytesCounter);
            return instance;
        }
        else {
            return new GSPResponseWriter(streamBuffer, response, bytesCounter);
        }
    }

    /**
     * Static factory method to create the writer.
     *
     * TODO: this can be removed?
     *
     * @param target The target writer to write too
     * @param max
     * @return A GSPResponseWriter instance
     */
    @SuppressWarnings("unused")
    private static GSPResponseWriter getInstance(Writer target, int max) {
        if (BUFFERING_ENABLED && !(target instanceof GrailsRoutablePrintWriter) && !(target instanceof StreamCharBufferWriter)) {
            StreamCharBuffer streamBuffer = new StreamCharBuffer(max, 0, max);
            streamBuffer.connectTo(target, false);
            target = streamBuffer.getWriter();
        }

        if (instantiator == null) {
            return new GSPResponseWriter(target);
        }

        GSPResponseWriter instance = (GSPResponseWriter) instantiator.newInstance();
        instance.initialize(target);
        return instance;
    }

    void initialize(final StreamCharBuffer buffer, final ServletResponse response,
            BoundedCharsAsEncodedBytesCounter bytesCounter) {
        DestinationFactory lazyTargetFactory = new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                final GrailsWebRequest webRequest = GrailsWebRequest.lookup();
                GSPResponseWriter.this.encoder = webRequest != null ? webRequest.lookupFilteringEncoder() : null;
                if (GSPResponseWriter.this.encoder != null) {
                    return buffer.getWriterForEncoder(GSPResponseWriter.this.encoder, webRequest.getEncodingStateRegistry());
                }
                return buffer.getWriter();
            }
        };

        updateDestination(lazyTargetFactory);
        this.response = response;
        this.bytesCounter = bytesCounter;
        setBlockClose(true);
        setBlockFlush(false);
    }

    void initialize(final Writer activeWriter) {
        updateDestination(new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                return activeWriter;
            }
        });
        setBlockClose(true);
        setBlockFlush(false);
    }

    /**
     * Close the stream.
     * @see #checkError()
     */
    @Override
    public void close() {
        flush();
        if (canFlushContentLengthAwareResponse()) {
            int size = this.bytesCounter.size();
            if (size > 0) {
                this.response.setContentLength(size);
            }
            flushResponse();
        }
        else if (!isTrouble()) {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            if (webRequest != null && webRequest.getCurrentRequest().getAttribute(RequestConstants.PAGE) != null) {
                // flush the response if its a layout
                flushResponse();
            }
        }
    }

    private boolean canFlushContentLengthAwareResponse() {
        return CONTENT_LENGTH_COUNTING_ENABLED && isDestinationActivated()
                && this.bytesCounter != null && this.bytesCounter.isWriterReferenced()
                && this.response != null && !this.response.isCommitted() && !isTrouble();
    }

    private void flushResponse() {
        try {
            if (isDestinationActivated()) {
                this.response.getWriter().flush();
            }
        }
        catch (IOException e) {
            handleIOException(e);
        }
    }

    @Override
    public boolean isAllowUnwrappingOut() {
        return false;
    }

    @Override
    public Writer unwrap() {
        return this;
    }

    public EncodedAppender getEncodedAppender() {
        if (this.buffer != null) {
            return ((EncodedAppenderFactory) this.buffer.getWriter()).getEncodedAppender();
        }

        activateDestination();
        Writer target = getTarget().unwrap();
        if (target != this && target instanceof EncodedAppenderFactory) {
            return ((EncodedAppenderFactory) target).getEncodedAppender();
        }

        return null;
    }

    public Encoder getEncoder() {
        return this.encoder;
    }

}
