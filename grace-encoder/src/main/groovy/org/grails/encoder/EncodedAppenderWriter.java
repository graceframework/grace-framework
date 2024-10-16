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
package org.grails.encoder;

import java.io.IOException;
import java.io.Writer;

/**
 * A java.io.Writer implementation that writes to a {@link EncodedAppender} with
 * a certain encoder
 *
 * This class isn't thread-safe.
 *
 * @author Lari Hotari
 * @since 2.3
 */
public class EncodedAppenderWriter extends Writer implements EncodedAppenderWriterFactory, EncodedAppenderFactory, EncoderAware {

    protected final EncodedAppender encodedAppender;

    protected final Encoder encoder;

    protected final EncodingStateRegistry encodingStateRegistry;

    private final char[] singleCharBuffer = new char[1];

    /**
     * Default constructor
     *
     * @param encodedAppender
     *            the EncodedAppender destination
     * @param encoder
     *            the encoder to use
     * @param encodingStateRegistry
     *            the {@link EncodingStateRegistry} to use to lookup encoding
     *            state of CharSequence instances
     */
    public EncodedAppenderWriter(EncodedAppender encodedAppender, Encoder encoder,
            EncodingStateRegistry encodingStateRegistry) {
        this.encodedAppender = encodedAppender;
        this.encoder = encoder;
        this.encodingStateRegistry = encodingStateRegistry;
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        this.encodedAppender.append(this.encoder, null, cbuf, off, len);
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#flush()
     */
    @Override
    public void flush() throws IOException {
        this.encodedAppender.flush();
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#close()
     */
    @Override
    public void close() throws IOException {
        this.encodedAppender.close();
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#write(int)
     */
    @Override
    public void write(int c) throws IOException {
        append((char) c);
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#write(java.lang.String, int, int)
     */
    @Override
    public void write(String str, int off, int len) throws IOException {
        this.encodedAppender.append(this.encoder,
                (this.encodingStateRegistry != null && off == 0 && len == str.length())
                        ? this.encodingStateRegistry.getEncodingStateFor(str)
                        : null,
                str, off, len);
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#append(java.lang.CharSequence)
     */
    @Override
    public Writer append(CharSequence csq) throws IOException {
        this.encodedAppender.append(this.encoder,
                (this.encodingStateRegistry != null) ? this.encodingStateRegistry.getEncodingStateFor(csq) : null,
                csq, 0, csq.length());
        return this;
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#append(java.lang.CharSequence, int, int)
     */
    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        this.encodedAppender.append(this.encoder, null, csq, 0, end - start);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see java.io.Writer#append(char)
     */
    @Override
    public Writer append(char c) throws IOException {
        this.singleCharBuffer[0] = c;
        this.encodedAppender.append(this.encoder, null, this.singleCharBuffer, 0, 1);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see EncodedAppenderFactory#
     * getEncodedAppender()
     */
    public EncodedAppender getEncodedAppender() {
        return this.encodedAppender;
    }

    /*
     * (non-Javadoc)
     * @see
     * EncoderAware#getEncoder()
     */
    public Encoder getEncoder() {
        return this.encoder;
    }

    /*
     * (non-Javadoc)
     * @see
     * EncodedAppenderWriterFactory
     * #getWriterForEncoder(Encoder,
     * EncodingStateRegistry)
     */
    public Writer getWriterForEncoder(Encoder encoder, EncodingStateRegistry encodingStateRegistry) {
        return new EncodedAppenderWriter(this.encodedAppender, encoder, encodingStateRegistry);
    }

}
