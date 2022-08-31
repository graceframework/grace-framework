/*
 * Copyright 2009-2022 the original author or authors.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Iterator;
import java.util.LinkedList;

import grails.util.GrailsArrayUtils;

/**
 * An in-memory buffer that provides OutputStream and InputStream interfaces.
 *
 * This is more efficient than using ByteArrayOutputStream/ByteArrayInputStream
 *
 * This is not thread-safe, it is intended to be used by a single Thread.
 *
 * @author Lari Hotari, Sagire Software Oy
 */
public class StreamByteBuffer {

    private static final int DEFAULT_CHUNK_SIZE = 8192;

    private LinkedList<StreamByteBufferChunk> chunks = new LinkedList<>();

    private StreamByteBufferChunk currentWriteChunk;

    private StreamByteBufferChunk currentReadChunk = null;

    private int chunkSize;

    private StreamByteBufferOutputStream output;

    private StreamByteBufferInputStream input;

    private int totalBytesUnreadInList = 0;

    private int totalBytesUnreadInIterator = 0;

    private ReadMode readMode;

    private Iterator<StreamByteBufferChunk> readIterator;

    public enum ReadMode {
        REMOVE_AFTER_READING,
        RETAIN_AFTER_READING
    }

    public StreamByteBuffer() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public StreamByteBuffer(int chunkSize) {
        this(chunkSize, ReadMode.REMOVE_AFTER_READING);
    }

    public StreamByteBuffer(int chunkSize, ReadMode readMode) {
        this.chunkSize = chunkSize;
        this.readMode = readMode;
        this.currentWriteChunk = new StreamByteBufferChunk(chunkSize);
        this.output = new StreamByteBufferOutputStream();
        this.input = new StreamByteBufferInputStream();
    }

    public OutputStream getOutputStream() {
        return this.output;
    }

    public InputStream getInputStream() {
        return this.input;
    }

    public void writeTo(OutputStream target) throws IOException {
        while (prepareRead() != -1) {
            this.currentReadChunk.writeTo(target);
        }
    }

    public byte[] readAsByteArray() {
        byte[] buf = new byte[totalBytesUnread()];
        this.input.readImpl(buf, 0, buf.length);
        return buf;
    }

    public String readAsString(String encoding) throws CharacterCodingException {
        Charset charset = Charset.forName(encoding);
        return readAsString(charset);
    }

    public String readAsString(Charset charset) throws CharacterCodingException {
        int unreadSize = totalBytesUnread();
        if (unreadSize > 0) {
            CharsetDecoder decoder = charset.newDecoder().onMalformedInput(
                    CodingErrorAction.REPLACE).onUnmappableCharacter(
                    CodingErrorAction.REPLACE);
            CharBuffer charbuffer = CharBuffer.allocate(unreadSize);
            ByteBuffer buf = null;
            while (prepareRead() != -1) {
                buf = this.currentReadChunk.readToNioBuffer();
                boolean endOfInput = (prepareRead() == -1);
                CoderResult result = decoder.decode(buf, charbuffer, endOfInput);
                if (endOfInput) {
                    if (!result.isUnderflow()) {
                        result.throwException();
                    }
                }
            }
            CoderResult result = decoder.flush(charbuffer);
            if (buf.hasRemaining()) {
                throw new IllegalStateException("There's a bug here, buffer wasn't read fully.");
            }
            if (!result.isUnderflow()) {
                result.throwException();
            }
            charbuffer.flip();
            String str;
            if (charbuffer.hasArray()) {
                int len = charbuffer.remaining();
                char[] ch = charbuffer.array();
                if (len != ch.length) {
                    ch = (char[]) GrailsArrayUtils.subarray(ch, 0, len);
                }
                str = StringCharArrayAccessor.createString(ch);
            }
            else {
                str = charbuffer.toString();
            }
            return str;
        }
        return null;
    }

    public int totalBytesUnread() {
        int total = 0;
        if (this.readMode == ReadMode.REMOVE_AFTER_READING) {
            total = this.totalBytesUnreadInList;
        }
        else if (this.readMode == ReadMode.RETAIN_AFTER_READING) {
            prepareRetainAfterReading();
            total = this.totalBytesUnreadInIterator;
        }
        if (this.currentReadChunk != null) {
            total += this.currentReadChunk.bytesUnread();
        }
        if (this.currentWriteChunk != null && this.currentWriteChunk != this.currentReadChunk) {
            if (this.readMode == ReadMode.REMOVE_AFTER_READING) {
                total += this.currentWriteChunk.bytesUnread();
            }
            else if (this.readMode == ReadMode.RETAIN_AFTER_READING) {
                total += this.currentWriteChunk.bytesUsed();
            }
        }
        return total;
    }

    protected int allocateSpace() {
        int spaceLeft = this.currentWriteChunk.spaceLeft();
        if (spaceLeft == 0) {
            this.chunks.add(this.currentWriteChunk);
            this.totalBytesUnreadInList += this.currentWriteChunk.bytesUnread();
            this.currentWriteChunk = new StreamByteBufferChunk(this.chunkSize);
            spaceLeft = this.currentWriteChunk.spaceLeft();
        }
        return spaceLeft;
    }

    protected int prepareRead() {
        prepareRetainAfterReading();
        int bytesUnread = (this.currentReadChunk != null) ? this.currentReadChunk.bytesUnread() : 0;
        if (bytesUnread == 0) {
            if (this.readMode == ReadMode.REMOVE_AFTER_READING && !this.chunks.isEmpty()) {
                this.currentReadChunk = this.chunks.removeFirst();
                bytesUnread = this.currentReadChunk.bytesUnread();
                this.totalBytesUnreadInList -= bytesUnread;
            }
            else if (this.readMode == ReadMode.RETAIN_AFTER_READING && this.readIterator.hasNext()) {
                this.currentReadChunk = this.readIterator.next();
                this.currentReadChunk.reset();
                bytesUnread = this.currentReadChunk.bytesUnread();
                this.totalBytesUnreadInIterator -= bytesUnread;
            }
            else if (this.currentReadChunk != this.currentWriteChunk) {
                this.currentReadChunk = this.currentWriteChunk;
                bytesUnread = this.currentReadChunk.bytesUnread();
            }
            else {
                bytesUnread = -1;
            }
        }
        return bytesUnread;
    }

    public void reset() {
        if (this.readMode == ReadMode.RETAIN_AFTER_READING) {
            this.readIterator = null;
            prepareRetainAfterReading();
            if (this.currentWriteChunk != null) {
                this.currentWriteChunk.reset();
            }
        }
    }

    private void prepareRetainAfterReading() {
        if (this.readMode == ReadMode.RETAIN_AFTER_READING && this.readIterator == null) {
            this.readIterator = this.chunks.iterator();
            this.totalBytesUnreadInIterator = this.totalBytesUnreadInList;
            this.currentReadChunk = null;
        }
    }

    public ReadMode getReadMode() {
        return this.readMode;
    }

    public void setReadMode(ReadMode readMode) {
        this.readMode = readMode;
    }

    public void retainAfterReadingMode() {
        setReadMode(ReadMode.RETAIN_AFTER_READING);
    }

    class StreamByteBufferChunk {

        private int pointer = 0;

        private byte[] buffer;

        private int size;

        private int used = 0;

        public StreamByteBufferChunk(int size) {
            this.size = size;
            this.buffer = new byte[size];
        }

        public ByteBuffer readToNioBuffer() {
            if (this.pointer < this.used) {
                ByteBuffer result;
                if (this.pointer > 0 || this.used < this.size) {
                    result = ByteBuffer.wrap(this.buffer, this.pointer, this.used - this.pointer);
                }
                else {
                    result = ByteBuffer.wrap(this.buffer);
                }
                this.pointer = this.used;
                return result;
            }

            return null;
        }

        public boolean write(byte b) {
            if (this.used < this.size) {
                this.buffer[this.used++] = b;
                return true;
            }

            return false;
        }

        public void write(byte[] b, int off, int len) {
            System.arraycopy(b, off, this.buffer, this.used, len);
            this.used = this.used + len;
        }

        public void read(byte[] b, int off, int len) {
            System.arraycopy(this.buffer, this.pointer, b, off, len);
            this.pointer = this.pointer + len;
        }

        public void writeTo(OutputStream target) throws IOException {
            if (this.pointer < this.used) {
                target.write(this.buffer, this.pointer, this.used - this.pointer);
                this.pointer = this.used;
            }
        }

        public void reset() {
            this.pointer = 0;
        }

        public int bytesUsed() {
            return this.used;
        }

        public int bytesUnread() {
            return this.used - this.pointer;
        }

        public int read() {
            if (this.pointer < this.used) {
                return this.buffer[this.pointer++] & 0xff;
            }

            return -1;
        }

        public int spaceLeft() {
            return this.size - this.used;
        }

    }

    class StreamByteBufferOutputStream extends OutputStream {

        private boolean closed = false;

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }

            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return;
            }

            int bytesLeft = len;
            int currentOffset = off;
            while (bytesLeft > 0) {
                int spaceLeft = allocateSpace();
                int writeBytes = Math.min(spaceLeft, bytesLeft);
                StreamByteBuffer.this.currentWriteChunk.write(b, currentOffset, writeBytes);
                bytesLeft -= writeBytes;
                currentOffset += writeBytes;
            }
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
        }

        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void write(int b) throws IOException {
            allocateSpace();
            StreamByteBuffer.this.currentWriteChunk.write((byte) b);
        }

        public StreamByteBuffer getBuffer() {
            return StreamByteBuffer.this;
        }

    }

    class StreamByteBufferInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            prepareRead();
            return StreamByteBuffer.this.currentReadChunk.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return readImpl(b, off, len);
        }

        int readImpl(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }

            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return 0;
            }

            int bytesLeft = len;
            int currentOffset = off;
            int bytesUnread = prepareRead();
            int totalBytesRead = 0;
            while (bytesLeft > 0 && bytesUnread != -1) {
                int readBytes = Math.min(bytesUnread, bytesLeft);
                StreamByteBuffer.this.currentReadChunk.read(b, currentOffset, readBytes);
                bytesLeft -= readBytes;
                currentOffset += readBytes;
                totalBytesRead += readBytes;
                bytesUnread = prepareRead();
            }
            if (totalBytesRead > 0) {
                return totalBytesRead;
            }

            return -1;
        }

        @Override
        public synchronized void reset() throws IOException {
            if (StreamByteBuffer.this.readMode == ReadMode.RETAIN_AFTER_READING) {
                StreamByteBuffer.this.reset();
            }
            else {
                // reset isn't supported in ReadMode.REMOVE_AFTER_READING
                super.reset();
            }
        }

        @Override
        public int available() throws IOException {
            return totalBytesUnread();
        }

        public StreamByteBuffer getBuffer() {
            return StreamByteBuffer.this;
        }

    }

    public void clear() {
        this.chunks.clear();
        this.currentReadChunk = null;
        this.totalBytesUnreadInList = 0;
        this.totalBytesUnreadInIterator = 0;
        this.currentWriteChunk = new StreamByteBufferChunk(this.chunkSize);
        this.readIterator = null;
    }

}
