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
package org.grails.gsp;

import java.util.Map;

import groovy.lang.Writable;
import groovy.text.Template;

import org.grails.taglib.encoder.OutputContextLookup;
import org.grails.taglib.encoder.OutputContextLookupHelper;

/**
 * Knows how to make in instance of GroovyPageWritable.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class GroovyPageTemplate implements Template, Cloneable {

    private final OutputContextLookup outputContextLookup;

    private GroovyPageMetaInfo metaInfo;

    private boolean allowSettingContentType = false;

    public GroovyPageTemplate(GroovyPageMetaInfo metaInfo) {
        this(metaInfo, OutputContextLookupHelper.getOutputContextLookup());
    }

    public GroovyPageTemplate(GroovyPageMetaInfo metaInfo, OutputContextLookup outputContextLookup) {
        this.metaInfo = metaInfo;
        this.outputContextLookup = outputContextLookup;
    }

    public Writable make() {
        return new GroovyPageWritable(this.metaInfo, this.outputContextLookup, this.allowSettingContentType);
    }

    @SuppressWarnings("rawtypes")
    public GroovyPageWritable make(Map binding) {
        GroovyPageWritable gptw = new GroovyPageWritable(this.metaInfo, this.outputContextLookup, this.allowSettingContentType);
        gptw.setBinding(binding);
        return gptw;
    }

    public GroovyPageMetaInfo getMetaInfo() {
        return this.metaInfo;
    }

    public boolean isAllowSettingContentType() {
        return this.allowSettingContentType;
    }

    public void setAllowSettingContentType(boolean allowSettingContentType) {
        this.allowSettingContentType = allowSettingContentType;
    }

    @Override
    public Object clone() {
        GroovyPageTemplate cloned = new GroovyPageTemplate(this.metaInfo);
        cloned.setAllowSettingContentType(this.allowSettingContentType);
        return cloned;
    }

}
