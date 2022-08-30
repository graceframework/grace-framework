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
package org.grails.buffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import groovy.lang.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

/**
 * @author Lari Hotari
 * @since 2.0
 */
public class GrailsPrintWriterAdapter extends PrintWriter implements GrailsWrappedWriter {

    private static final Logger LOG = LoggerFactory.getLogger(GrailsPrintWriterAdapter.class);

    protected GrailsPrintWriter target;

    private static ObjectInstantiator instantiator;

    static {
        try {
            instantiator = new ObjenesisStd(false).getInstantiatorOf(GrailsPrintWriterAdapter.class);
        }
        catch (Exception e) {
            LOG.debug("Couldn't get direct performance optimized instantiator for GrailsPrintWriterAdapter. Using default instantiation.", e);
        }
    }

    public GrailsPrintWriterAdapter(Writer wrapped) {
        super(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                // no-op
            }

            @Override
            public void flush() throws IOException {
                // no-op
            }

            @Override
            public void close() throws IOException {
                // no-op
            }
        });
        setTarget(wrapped);
    }

    public static GrailsPrintWriterAdapter newInstance(Writer wrapped) {
        if (instantiator != null) {
            GrailsPrintWriterAdapter instance = (GrailsPrintWriterAdapter) instantiator.newInstance();
            instance.setTarget(wrapped);
            return instance;
        }
        return new GrailsPrintWriterAdapter(wrapped);
    }

    public void setTarget(Writer wrapped) {
        if (wrapped instanceof GrailsPrintWriter) {
            this.target = ((GrailsPrintWriter) wrapped);
        }
        else {
            this.target = new GrailsPrintWriter(wrapped);
        }
        this.out = this.target;
        this.lock = this.out != null ? this.out : this;
    }

    public boolean isAllowUnwrappingOut() {
        return true;
    }

    public GrailsPrintWriter getTarget() {
        return this.target;
    }

    public Writer getOut() {
        return this.target.getOut();
    }

    public Writer unwrap() {
        return this.target.unwrap();
    }

    public GrailsPrintWriter leftShift(Object value) throws IOException {
        return this.target.leftShift(value);
    }

    public GrailsPrintWriter plus(Object value) throws IOException {
        return this.target.plus(value);
    }

    @Override
    public boolean checkError() {
        return this.target.checkError();
    }

    @Override
    public void setError() {
        this.target.setError();
    }

    @Override
    public void flush() {
        this.target.flush();
    }

    @Override
    public void print(Object obj) {
        this.target.print(obj);
    }

    @Override
    public void print(String s) {
        this.target.print(s);
    }

    @Override
    public void write(String s) {
        this.target.write(s);
    }

    @Override
    public void write(int c) {
        this.target.write(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        this.target.write(buf, off, len);
    }

    @Override
    public void write(String s, int off, int len) {
        this.target.write(s, off, len);
    }

    @Override
    public void write(char[] buf) {
        this.target.write(buf);
    }

    @Override
    public void print(boolean b) {
        this.target.print(b);
    }

    @Override
    public void print(char c) {
        this.target.print(c);
    }

    @Override
    public void print(int i) {
        this.target.print(i);
    }

    @Override
    public void print(long l) {
        this.target.print(l);
    }

    @Override
    public void print(float f) {
        this.target.print(f);
    }

    @Override
    public void print(double d) {
        this.target.print(d);
    }

    @Override
    public void print(char[] s) {
        this.target.print(s);
    }

    @Override
    public void println() {
        this.target.println();
    }

    @Override
    public void println(boolean b) {
        this.target.println(b);
    }

    @Override
    public void println(char c) {
        this.target.println(c);
    }

    @Override
    public void println(int i) {
        this.target.println(i);
    }

    @Override
    public void println(long l) {
        this.target.println(l);
    }

    @Override
    public void println(float f) {
        this.target.println(f);
    }

    @Override
    public void println(double d) {
        this.target.println(d);
    }

    @Override
    public void println(char[] c) {
        this.target.println(c);
    }

    @Override
    public void println(String s) {
        this.target.println(s);
    }

    @Override
    public void println(Object o) {
        this.target.println(o);
    }

    @Override
    public PrintWriter append(char c) {
        this.target.append(c);
        return this;
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        this.target.append(csq, start, end);
        return this;
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        this.target.append(csq);
        return this;
    }

    public PrintWriter append(Object obj) {
        this.target.append(obj);
        return this;
    }

    public void write(StreamCharBuffer otherBuffer) {
        this.target.write(otherBuffer);
    }

    public void print(StreamCharBuffer otherBuffer) {
        this.target.print(otherBuffer);
    }

    public void append(StreamCharBuffer otherBuffer) {
        this.target.append(otherBuffer);
    }

    public void println(StreamCharBuffer otherBuffer) {
        this.target.println(otherBuffer);
    }

    public GrailsPrintWriter leftShift(StreamCharBuffer otherBuffer) {
        return this.target.leftShift(otherBuffer);
    }

    public void write(Writable writable) {
        this.target.write(writable);
    }

    public void print(Writable writable) {
        this.target.print(writable);
    }

    public GrailsPrintWriter leftShift(Writable writable) {
        return this.target.leftShift(writable);
    }

    public boolean isUsed() {
        return this.target.isUsed();
    }

    public void setUsed(boolean newUsed) {
        this.target.setUsed(newUsed);
    }

    public boolean resetUsed() {
        return this.target.resetUsed();
    }

    @Override
    public void close() {
        this.target.close();
    }

    public void markUsed() {
        this.target.markUsed();
    }

    protected boolean isTrouble() {
        return this.target.isTrouble();
    }

    protected void handleIOException(IOException e) {
        this.target.handleIOException(e);
    }

}
