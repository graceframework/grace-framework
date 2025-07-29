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
package org.grails.taglib.encoder;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.grails.buffer.CodecPrintWriter;
import org.grails.buffer.GrailsLazyProxyPrintWriter;
import org.grails.buffer.GrailsLazyProxyPrintWriter.DestinationFactory;
import org.grails.buffer.GrailsWrappedWriter;
import org.grails.encoder.EncodedAppender;
import org.grails.encoder.EncodedAppenderFactory;
import org.grails.encoder.EncodedAppenderWriterFactory;
import org.grails.encoder.Encoder;
import org.grails.encoder.EncoderAware;
import org.grails.encoder.EncodingStateRegistry;
import org.grails.encoder.StreamingEncoder;
import org.grails.encoder.StreamingEncoderWriter;

public final class OutputEncodingStack {

    public static final Log log = LogFactory.getLog(OutputEncodingStack.class);

    private static final String ATTRIBUTE_NAME_OUTPUT_STACK = "org.grails.taglib.encoder.OUTPUT_ENCODING_STACK";

    private final OutputContext outputContext;

    private Stack<StackEntry> stack = new Stack<StackEntry>();

    private OutputProxyWriter taglibWriter;

    private OutputProxyWriter outWriter;

    private OutputProxyWriter staticWriter;

    private OutputProxyWriter expressionWriter;

    private boolean autoSync;

    private EncodingStateRegistry encodingStateRegistry;

    private OutputProxyWriterGroup writerGroup = new OutputProxyWriterGroup();

    private OutputEncodingStack(OutputEncodingStackAttributes attributes) {
        this.outWriter = new OutputProxyWriter(this.writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = OutputEncodingStack.this.stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.outEncoder, OutputEncodingStack.this.encodingStateRegistry,
                        OutputEncodingSettings.OUT_CODEC_NAME);
            }
        });
        this.staticWriter = new OutputProxyWriter(this.writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = OutputEncodingStack.this.stack.peek();
                if (stackEntry.staticEncoder == null) {
                    return stackEntry.unwrappedTarget;
                }
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.staticEncoder, OutputEncodingStack.this.encodingStateRegistry,
                        OutputEncodingSettings.STATIC_CODEC_NAME);
            }
        });
        this.expressionWriter = new OutputProxyWriter(this.writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = OutputEncodingStack.this.stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget, stackEntry.expressionEncoder, OutputEncodingStack.this.encodingStateRegistry,
                        OutputEncodingSettings.EXPRESSION_CODEC_NAME);
            }
        });
        this.taglibWriter = new OutputProxyWriter(this.writerGroup, new DestinationFactory() {
            public Writer activateDestination() throws IOException {
                StackEntry stackEntry = OutputEncodingStack.this.stack.peek();
                return createEncodingWriter(stackEntry.unwrappedTarget,
                        stackEntry.taglibEncoder != null ? stackEntry.taglibEncoder : stackEntry.defaultTaglibEncoder,
                        OutputEncodingStack.this.encodingStateRegistry, OutputEncodingSettings.TAGLIB_CODEC_NAME);
            }
        });
        this.autoSync = attributes.isAutoSync();
        push(attributes, false);
        if (!this.autoSync) {
            applyWriterThreadLocals(this.outWriter);
        }
        this.encodingStateRegistry = attributes.getOutputContext().getEncodingStateRegistry();
        this.outputContext = attributes.getOutputContext() != null ? attributes.getOutputContext() : OutputContextLookupHelper.lookupOutputContext();
    }

    public static OutputEncodingStack currentStack() {
        return currentStack(true);
    }

    public static OutputEncodingStack currentStack(OutputContext outputContext) {
        return currentStack(outputContext, true);
    }

    public static OutputEncodingStack currentStack(boolean allowCreate) {
        return currentStack(OutputContextLookupHelper.lookupOutputContext(), allowCreate);
    }

    public static OutputEncodingStack currentStack(OutputContext outputContext, boolean allowCreate) {
        OutputEncodingStack outputStack = lookupStack(outputContext);
        if (outputStack == null && allowCreate) {
            outputStack = currentStack(outputContext, allowCreate, null, allowCreate, false);
        }
        return outputStack;
    }

    public static OutputEncodingStack currentStack(boolean allowCreate, Writer topWriter, boolean autoSync, boolean pushTop) {
        return currentStack(OutputContextLookupHelper.lookupOutputContext(), allowCreate, topWriter, autoSync, pushTop);
    }

    public static OutputEncodingStack currentStack(OutputContext outputContext,
            boolean allowCreate, Writer topWriter, boolean autoSync, boolean pushTop) {
        return currentStack(new OutputEncodingStackAttributes.Builder()
                .outputContext(outputContext).allowCreate(allowCreate).topWriter(topWriter).autoSync(autoSync).pushTop(pushTop).build());
    }

    public static OutputEncodingStack currentStack(OutputEncodingStackAttributes attributes) {
        OutputEncodingStack outputStack = lookupStack(attributes.getOutputContext());
        if (outputStack != null) {
            if (attributes.isPushTop()) {
                outputStack.push(attributes, false);
            }
            return outputStack;
        }

        if (attributes.isAllowCreate()) {
            return createNew(attributes);
        }

        return null;
    }

    private static OutputEncodingStack createNew(OutputEncodingStackAttributes attributes) {
        if (attributes.getTopWriter() == null) {
            attributes = new OutputEncodingStackAttributes.Builder(attributes).topWriter(lookupCurrentWriter(attributes.getOutputContext())).build();
        }
        OutputEncodingStack instance = new OutputEncodingStack(attributes);
        attributes.getOutputContext().setCurrentOutputEncodingStack(instance);
        return instance;
    }

    private static OutputEncodingStack lookupStack(OutputContext outputContext) {
        OutputEncodingStack outputStack = outputContext.getCurrentOutputEncodingStack();
        return outputStack;
    }

    public static Writer currentWriter() {
        OutputEncodingStack outputStack = currentStack(false);
        if (outputStack != null) {
            return outputStack.getOutWriter();
        }

        return lookupCurrentWriter();
    }

    private static Writer lookupCurrentWriter() {
        OutputContext outputContext = OutputContextLookupHelper.lookupOutputContext();
        return lookupCurrentWriter(outputContext);
    }

    private static Writer lookupCurrentWriter(OutputContext outputContext) {
        if (outputContext != null) {
            return outputContext.getCurrentWriter();
        }
        return null;
    }

    private Writer unwrapTargetWriter(Writer targetWriter) {
        if (targetWriter instanceof GrailsWrappedWriter && ((GrailsWrappedWriter) targetWriter).isAllowUnwrappingOut()) {
            return ((GrailsWrappedWriter) targetWriter).unwrap();
        }
        return targetWriter;
    }

    public void push(final Writer newWriter) {
        push(newWriter, false);
    }

    public void push(final Writer newWriter, final boolean checkExisting) {
        OutputEncodingStackAttributes.Builder attributesBuilder = new OutputEncodingStackAttributes.Builder();
        attributesBuilder.inheritPreviousEncoders(true);
        attributesBuilder.topWriter(newWriter);
        push(attributesBuilder.build(), checkExisting);
    }

    public void push(final OutputEncodingStackAttributes attributes) {
        push(attributes, false);
    }

    public void push(final OutputEncodingStackAttributes attributes, final boolean checkExisting) {
        this.writerGroup.reset();

        if (checkExisting) {
            checkExistingStack(attributes.getTopWriter());
        }

        StackEntry previousStackEntry = null;
        if (this.stack.size() > 0) {
            previousStackEntry = this.stack.peek();
        }

        Writer topWriter = attributes.getTopWriter();
        Writer unwrappedWriter = null;
        if (topWriter != null) {
            if (topWriter instanceof OutputProxyWriter) {
                topWriter = ((OutputProxyWriter) topWriter).getOut();
            }
            unwrappedWriter = unwrapTargetWriter(topWriter);
        }
        else if (previousStackEntry != null) {
            topWriter = previousStackEntry.originalTarget;
            unwrappedWriter = previousStackEntry.unwrappedTarget;
        }
        else {
            throw new NullPointerException("attributes.getTopWriter() is null and there is no previous stack item");
        }

        StackEntry stackEntry = new StackEntry(topWriter, unwrappedWriter);
        stackEntry.outEncoder = applyEncoder(attributes.getOutEncoder(),
                previousStackEntry != null ? previousStackEntry.outEncoder : null,
                attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.staticEncoder = applyEncoder(attributes.getStaticEncoder(),
                previousStackEntry != null ? previousStackEntry.staticEncoder : null,
                attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.expressionEncoder = applyEncoder(attributes.getExpressionEncoder(),
                previousStackEntry != null ? previousStackEntry.expressionEncoder : null,
                attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.taglibEncoder = applyEncoder(attributes.getTaglibEncoder(),
                previousStackEntry != null ? previousStackEntry.taglibEncoder : null,
                attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());
        stackEntry.defaultTaglibEncoder = applyEncoder(attributes.getDefaultTaglibEncoder(),
                previousStackEntry != null ? previousStackEntry.defaultTaglibEncoder : null,
                attributes.isInheritPreviousEncoders(), attributes.isReplaceOnly());

        this.stack.push(stackEntry);

        resetWriters();

        if (this.autoSync) {
            applyWriterThreadLocals(attributes.getTopWriter());
        }
    }

    private Encoder applyEncoder(Encoder newEncoder, Encoder previousEncoder, boolean allowInheriting, boolean replaceOnly) {
        if (newEncoder != null && (!replaceOnly || previousEncoder == null || replaceOnly && previousEncoder.isSafe())) {
            return newEncoder;
        }
        if (allowInheriting) {
            return previousEncoder;
        }
        return null;
    }

    private void checkExistingStack(final Writer topWriter) {
        if (topWriter != null) {
            for (StackEntry item : this.stack) {
                if (item.originalTarget == topWriter) {
                    log.warn("Pushed a writer to stack a second time. Writer type " +
                            topWriter.getClass().getName(), new Exception());
                }
            }
        }
    }

    private void resetWriters() {
        this.outWriter.setDestinationActivated(false);
        this.staticWriter.setDestinationActivated(false);
        this.expressionWriter.setDestinationActivated(false);
        this.taglibWriter.setDestinationActivated(false);
    }

    private Writer createEncodingWriter(Writer out, Encoder encoder, EncodingStateRegistry encodingStateRegistry, String codecWriterName) {
        Writer encodingWriter;
        if (out instanceof EncodedAppenderWriterFactory) {
            encodingWriter = ((EncodedAppenderWriterFactory) out).getWriterForEncoder(encoder, encodingStateRegistry);
        }
        else if (encoder instanceof StreamingEncoder) {
            encodingWriter = new StreamingEncoderWriter(out, (StreamingEncoder) encoder, encodingStateRegistry);
        }
        else {
            encodingWriter = new CodecPrintWriter(out, encoder, encodingStateRegistry);
        }
        return encodingWriter;
    }

    public void pop() {
        pop(this.autoSync);
    }

    public void pop(boolean forceSync) {
        this.writerGroup.reset();
        this.stack.pop();
        resetWriters();
        if (this.stack.size() > 0) {
            StackEntry stackEntry = this.stack.peek();
            if (forceSync) {
                applyWriterThreadLocals(stackEntry.originalTarget);
            }
        }
    }

    public OutputProxyWriter getOutWriter() {
        return this.outWriter;
    }

    public OutputProxyWriter getStaticWriter() {
        return this.staticWriter;
    }

    public OutputProxyWriter getExpressionWriter() {
        return this.expressionWriter;
    }

    public OutputProxyWriter getTaglibWriter() {
        return this.taglibWriter;
    }

    public Encoder getOutEncoder() {
        return this.stack.size() > 0 ? this.stack.peek().outEncoder : null;
    }

    public Encoder getStaticEncoder() {
        return this.stack.size() > 0 ? this.stack.peek().staticEncoder : null;
    }

    public Encoder getExpressionEncoder() {
        return this.stack.size() > 0 ? this.stack.peek().expressionEncoder : null;
    }

    public Encoder getTaglibEncoder() {
        return this.stack.size() > 0 ? this.stack.peek().taglibEncoder : null;
    }

    public Encoder getDefaultTaglibEncoder() {
        return this.stack.size() > 0 ? this.stack.peek().defaultTaglibEncoder : null;
    }

    public Writer getCurrentOriginalWriter() {
        return this.stack.peek().originalTarget;
    }

    public void restoreThreadLocalsToOriginals() {
        Writer originalTopWriter = this.stack.firstElement().originalTarget;
        applyWriterThreadLocals(originalTopWriter);
    }

    private void applyWriterThreadLocals(Writer writer) {
        if (this.outputContext != null) {
            this.outputContext.setCurrentWriter(writer);
        }
    }

    public void flushActiveWriter() {
        this.writerGroup.flushActive();
    }

    public OutputContext getOutputContext() {
        return this.outputContext;
    }

    private static class StackEntry implements Cloneable {

        Writer originalTarget;

        Writer unwrappedTarget;

        Encoder staticEncoder;

        Encoder taglibEncoder;

        Encoder defaultTaglibEncoder;

        Encoder outEncoder;

        Encoder expressionEncoder;

        StackEntry(Writer originalTarget, Writer unwrappedTarget) {
            this.originalTarget = originalTarget;
            this.unwrappedTarget = unwrappedTarget;
        }

        @Override
        public StackEntry clone() {
            StackEntry newEntry = new StackEntry(this.originalTarget, this.unwrappedTarget);
            newEntry.staticEncoder = this.staticEncoder;
            newEntry.outEncoder = this.outEncoder;
            newEntry.taglibEncoder = this.taglibEncoder;
            newEntry.defaultTaglibEncoder = this.defaultTaglibEncoder;
            newEntry.expressionEncoder = this.expressionEncoder;
            return newEntry;
        }

    }

    static class OutputProxyWriterGroup {

        OutputProxyWriter activeWriter;

        void reset() {
            activateWriter(null);
        }

        void activateWriter(OutputProxyWriter newWriter) {
            if (newWriter != this.activeWriter) {
                flushActive();
                this.activeWriter = newWriter;
            }
        }

        void flushActive() {
            if (this.activeWriter != null) {
                this.activeWriter.flush();
            }
        }

    }

    public class OutputProxyWriter extends GrailsLazyProxyPrintWriter implements EncodedAppenderFactory, EncoderAware {

        OutputProxyWriterGroup writerGroup;

        OutputProxyWriter(OutputProxyWriterGroup writerGroup, DestinationFactory factory) {
            super(factory);
            this.writerGroup = writerGroup;
        }

        public OutputEncodingStack getOutputStack() {
            return OutputEncodingStack.this;
        }

        @Override
        public Writer getOut() {
            this.writerGroup.activateWriter(this);
            return super.getOut();
        }

        @Override
        public EncodedAppender getEncodedAppender() {
            Writer out = getOut();
            if (out instanceof EncodedAppenderFactory) {
                return ((EncodedAppenderFactory) out).getEncodedAppender();
            }
            else if (out instanceof EncodedAppender) {
                return (EncodedAppender) getOut();
            }
            else {
                return null;
            }
        }

        @Override
        public Encoder getEncoder() {
            Writer out = getOut();
            if (out instanceof EncoderAware) {
                return ((EncoderAware) out).getEncoder();
            }
            return null;
        }

    }

}
