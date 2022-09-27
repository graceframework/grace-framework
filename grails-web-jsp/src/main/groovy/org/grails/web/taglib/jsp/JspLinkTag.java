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

/**
 * A JSP facade that delegates to the Grails taglib link tag.
 *
 * @author Graeme Rocher
 */
public class JspLinkTag extends JspInvokeGrailsTagLibTag {

    private static final long serialVersionUID = 5302909740441701754L;

    private static final String TAG_NAME = "link";

    private String controller;

    private String action;

    private String id;

    private String url;

    public JspLinkTag() {
        super.setTagName(TAG_NAME);
    }

    public String getController() {
        return this.controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
