/*
 * Copyright 2022-2023 the original author or authors.
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
package org.grails.taglib;

import java.util.HashMap;
import java.util.Map;


/**
 * The context for the {@link TagInvocationContextCustomizer to customize}.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@SuppressWarnings("rawtypes")
public class TagInvocationContext {

    private String namespace;

    private String tagName;

    private Map attrs = new HashMap();

    public TagInvocationContext(String namespace, String tagName) {
        this.namespace = namespace;
        this.tagName = tagName;
    }

    public TagInvocationContext(String namespace, String tagName, Map attrs) {
        this.namespace = namespace;
        this.tagName = tagName;
        this.attrs = attrs;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Map getAttrs() {
        return attrs;
    }

    public void setAttrs(Map attrs) {
        this.attrs = attrs;
    }

}
