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
package org.grails.plugins.web.taglib;

import java.util.Map;

import grails.util.CollectionUtils;

import org.grails.taglib.TagInvocationContext;
import org.grails.taglib.TagInvocationContextCustomizer;
import org.grails.taglib.TagOutput;

/**
 * Customizer for {@link ApplicationTagLib}, support new namespace {@code link}
 *
 * @author Michael Yan
 * @since 2022.0.0
 * @see TagInvocationContextCustomizer
 */
public class ApplicationTagInvocationContextCustomizer implements TagInvocationContextCustomizer {

    private static final String LINK_NAMESPACE = "link";

    @Override
    @SuppressWarnings({"rawtypes", "unckecked"})
    public void customize(TagInvocationContext tagInvocationContext) {
        if (tagInvocationContext.getNamespace().equals(LINK_NAMESPACE)) {
            String tagName = "link";
            String tmpTagName = tagInvocationContext.getTagName();
            Map tmpAttrs = tagInvocationContext.getAttrs();
            Map attrs = CollectionUtils.newMap("mapping", tmpTagName);
            if (!tmpAttrs.isEmpty()) {
                attrs.put("params", tmpAttrs);
            }
            tagInvocationContext.setNamespace(TagOutput.DEFAULT_NAMESPACE);
            tagInvocationContext.setTagName(tagName);
            tagInvocationContext.setAttrs(attrs);
        }
    }

}
