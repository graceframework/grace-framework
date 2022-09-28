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
package org.grails.web.taglib.jsp;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.DynamicAttributes;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;

import grails.core.GrailsApplication;
import grails.core.gsp.GrailsTagLibClass;
import grails.util.Holders;

import org.grails.buffer.FastStringPrintWriter;
import org.grails.core.artefact.TagLibArtefactHandler;
import org.grails.gsp.GroovyPage;
import org.grails.taglib.GrailsTagException;

/**
 * A tag that invokes a tag defined in a the Grails dynamic tag library. Authors of Grails tags
 * who want their tags to work in JSP should sub-class this class and call "setTagName" to set
 * the tagName of the tag within the Grails taglib
 *
 * @author Graeme Rocher
 * @since 16-Jan-2006
 * @deprecated
 */
@Deprecated
public class JspInvokeGrailsTagLibTag extends BodyTagSupport implements DynamicAttributes {

    private static final long serialVersionUID = 4688821761801666631L;

    private static final String ZERO_ARGUMENTS = "zeroArgumentsFlag";

    private static final String GROOVY_DEFAULT_ARGUMENT = "it";

    private static final String NAME_ATTRIBUTE = "tagName";

    private static final Pattern ATTRIBUTE_MAP = Pattern.compile("(\\s*(\\S+)\\s*:\\s*(\\S+?)(,|$){1}){1}");

    private String tagName;

    private int invocationCount;

    private List<Object> invocationArgs = new ArrayList<>();

    private List<String> invocationBodyContent = new ArrayList<>();

    private BeanWrapper bean;

    protected Map<String, Object> attributes = new HashMap<>();

    private FastStringPrintWriter sw;

    private PrintWriter out;

    private JspWriter jspWriter;

    private GrailsApplication application;

    private ApplicationContext appContext;

    private static final String TAG_LIBS_ATTRIBUTE = "org.codehaus.groovy.grails.TAG_LIBS";

    private static final String OUT_PROPERTY = "out";

    private String tagContent;

    private boolean bodyInvokation;

    public JspInvokeGrailsTagLibTag() {
        this.bean = new BeanWrapperImpl(this);
    }

    @Override
    public final int doStartTag() throws JspException {
        for (PropertyDescriptor pd : this.bean.getPropertyDescriptors()) {
            if (pd.getPropertyType() == String.class &&
                    !pd.getName().equals(NAME_ATTRIBUTE) &&
                    this.bean.isWritableProperty(pd.getName()) &&
                    this.bean.isReadableProperty(pd.getName())) {

                String propertyValue = (String) this.bean.getPropertyValue(pd.getName());

                if (propertyValue != null) {
                    String trimmed = propertyValue.trim();
                    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        trimmed = trimmed.substring(1, trimmed.length() - 1);
                        Matcher m = ATTRIBUTE_MAP.matcher(trimmed);
                        Map<String, Object> attributeMap = new HashMap<>();
                        while (m.find()) {
                            String attributeName = m.group(1);
                            String attributeValue = m.group(2);
                            attributeMap.put(attributeName, attributeValue);
                        }
                        this.attributes.put(pd.getName(), attributeMap);
                    }
                    else {
                        this.attributes.put(pd.getName(), propertyValue);
                    }
                }
            }
        }
        return doStartTagInternal();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private GroovyObject getTagLib(String name) {
        if (this.application == null) {
            initPageState();
        }

        Map tagLibs = (Map) this.pageContext.getAttribute(TAG_LIBS_ATTRIBUTE);
        if (tagLibs == null) {
            tagLibs = new HashMap();
            this.pageContext.setAttribute(TAG_LIBS_ATTRIBUTE, tagLibs);
        }
        GrailsTagLibClass tagLibClass = (GrailsTagLibClass) this.application.getArtefactForFeature(
                TagLibArtefactHandler.TYPE, GroovyPage.DEFAULT_NAMESPACE + ':' + name);

        GroovyObject tagLib;
        if (tagLibs.containsKey(tagLibClass.getFullName())) {
            tagLib = (GroovyObject) tagLibs.get(tagLibClass.getFullName());
        }
        else {
            tagLib = (GroovyObject) this.appContext.getBean(tagLibClass.getFullName());
            tagLibs.put(tagLibClass.getFullName(), tagLib);
        }
        return tagLib;
    }

    private void initPageState() {
        if (this.application == null) {
            this.application = Holders.getGrailsApplication();
            this.appContext = this.application != null ? this.application.getMainContext() : null;
        }
    }

    @SuppressWarnings("rawtypes")
    protected int doStartTagInternal() {
        GroovyObject tagLib = getTagLib(getTagName());
        if (tagLib == null) {
            throw new GrailsTagException("Tag [" + getTagName() + "] does not exist. No tag library found.");
        }

        this.sw = FastStringPrintWriter.newInstance();
        this.out = this.sw;
        tagLib.setProperty(OUT_PROPERTY, this.out);
        Object tagLibProp;
        final Map tagLibProperties = DefaultGroovyMethods.getProperties(tagLib);
        if (tagLibProperties.containsKey(getTagName())) {
            tagLibProp = tagLibProperties.get(getTagName());
        }
        else {
            throw new GrailsTagException("Tag [" + getTagName() + "] does not exist in tag library [" +
                    tagLib.getClass().getName() + "]");
        }

        if (!(tagLibProp instanceof Closure)) {
            throw new GrailsTagException("Tag [" + getTagName() + "] does not exist in tag library [" +
                    tagLib.getClass().getName() + "]");
        }

        Closure body = new Closure(this) {
            private static final long serialVersionUID = 1861498565854341886L;

            @SuppressWarnings("unused")
            public Object doCall() {
                return call();
            }

            @SuppressWarnings("unused")
            public Object doCall(Object o) {
                return call(new Object[] { o });
            }

            @SuppressWarnings("unused")
            public Object doCall(Object[] args) {
                return call(args);
            }

            @Override
            public Object call(Object... args) {
                JspInvokeGrailsTagLibTag.this.invocationCount++;
                if (args.length > 0) {
                    JspInvokeGrailsTagLibTag.this.invocationArgs.add(args[0]);
                }
                else {
                    JspInvokeGrailsTagLibTag.this.invocationArgs.add(ZERO_ARGUMENTS);
                }
                JspInvokeGrailsTagLibTag.this.out.print("<jsp-body-gen" + JspInvokeGrailsTagLibTag.this.invocationCount + ">");
                return "";
            }
        };
        Closure tag = (Closure) tagLibProp;
        if (tag.getParameterTypes().length == 1) {
            tag.call(new Object[] { this.attributes });
            if (body != null) {
                body.call();
            }
        }
        if (tag.getParameterTypes().length == 2) {
            tag.call(new Object[] { this.attributes, body });
        }

        Collections.reverse(this.invocationArgs);
        setCurrentArgument();
        return EVAL_BODY_BUFFERED;
    }

    private void setCurrentArgument() {
        if (this.invocationCount == 0) {
            return;
        }

        Object arg = this.invocationArgs.get(this.invocationCount - 1);
        if (arg.equals(ZERO_ARGUMENTS)) {
            this.pageContext.setAttribute(GROOVY_DEFAULT_ARGUMENT, null);
        }
        else {
            this.pageContext.setAttribute(GROOVY_DEFAULT_ARGUMENT, arg);
        }
    }

    @Override
    public int doAfterBody() throws JspException {
        BodyContent b = getBodyContent();
        if (this.invocationCount > 0) {
            if (b != null) {
                this.jspWriter = b.getEnclosingWriter();
                this.invocationBodyContent.add(b.getString());
                this.bodyInvokation = true;
                b.clearBody();
            }
        }

        this.invocationCount--;
        setCurrentArgument();
        if (this.invocationCount < 1) {
            this.tagContent = this.sw.toString();
            int i = 1;
            StringBuilder buf = new StringBuilder();
            for (String body : this.invocationBodyContent) {
                String replaceFlag = "<jsp-body-gen" + i + ">";
                int j = this.tagContent.indexOf(replaceFlag);
                if (j > -1) {
                    buf.append(this.tagContent.substring(0, j))
                            .append(body)
                            .append(this.tagContent.substring(j + replaceFlag.length(), this.tagContent.length()));
                    this.tagContent = buf.toString();
                    if (this.tagContent != null) {
                        try {
                            this.jspWriter.write(this.tagContent);
                            this.out.close();
                        }
                        catch (IOException e) {
                            throw new JspTagException("I/O error writing tag contents [" + this.tagContent +
                                    "] to response out");
                        }
                    }
                    buf.delete(0, buf.length());
                }
            }
            return SKIP_BODY;
        }

        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        if (!this.bodyInvokation) {
            if (this.tagContent == null) {
                this.tagContent = this.sw.toString();
            }

            if (this.tagContent != null) {
                try {
                    this.jspWriter = this.pageContext.getOut();
                    this.jspWriter.write(this.tagContent);
                    this.out.close();
                }
                catch (IOException e) {
                    throw new JspTagException("I/O error writing tag contents [" + this.tagContent +
                            "] to response out");
                }
            }
        }
        return SKIP_BODY;
    }

    public String getTagName() {
        return this.tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public final void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
        if (value instanceof String) {
            String stringValue = (String) value;
            String trimmed = stringValue.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
                Matcher m = ATTRIBUTE_MAP.matcher(trimmed);
                Map<String, Object> attributeMap = new HashMap<>();
                while (m.find()) {
                    String attributeName = m.group(1);
                    String attributeValue = m.group(2);
                    attributeMap.put(attributeName, attributeValue);
                }
                this.attributes.put(localName, attributeMap);
            }
            else {
                this.attributes.put(localName, value);
            }
        }
        else {
            this.attributes.put(localName, value);
        }
    }

}
