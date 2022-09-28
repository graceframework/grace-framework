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
package org.grails.core.gsp;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaProperty;

import grails.core.gsp.GrailsTagLibClass;

import org.grails.core.artefact.gsp.TagLibArtefactHandler;

/**
 * Default implementation of a tag lib class.
 *
 * @author Graeme Rocher
 *
 */
public class DefaultGrailsTagLibClass extends org.grails.core.DefaultGrailsTagLibClass implements GrailsTagLibClass {

    protected static final String TAG_LIB = TagLibArtefactHandler.TYPE;

    private Set<String> tags = new HashSet<>();

    private String namespace = GrailsTagLibClass.DEFAULT_NAMESPACE;

    private Set<String> returnObjectForTagsSet = new HashSet<>();

    private Object defaultEncodeAs = null;

    private Map<String, Object> encodeAsForTags = new HashMap<>();

    /**
     * Default contructor.
     *
     * @param clazz        the tag library class
     */
    @SuppressWarnings("rawtypes")
    public DefaultGrailsTagLibClass(Class<?> clazz) {
        super(clazz);

        for (MetaProperty prop : GroovySystem.getMetaClassRegistry().getMetaClass(clazz).getProperties()) {
            int modifiers = prop.getModifiers();
            if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                continue;
            }

            if (Closure.class.isAssignableFrom(prop.getType())) {
                this.tags.add(prop.getName());
            }
        }

        String ns = getStaticPropertyValue(NAMESPACE_FIELD_NAME, String.class);
        if (ns != null && !"".equals(ns.trim())) {
            this.namespace = ns.trim();
        }

        List returnObjectForTagsList = getStaticPropertyValue(RETURN_OBJECT_FOR_TAGS_FIELD_NAME, List.class);
        if (returnObjectForTagsList != null) {
            for (Object tagName : returnObjectForTagsList) {
                this.returnObjectForTagsSet.add(String.valueOf(tagName));
            }
        }

        this.defaultEncodeAs = getStaticPropertyValue(DEFAULT_ENCODE_AS_FIELD_NAME, Object.class);

        Map encodeAsForTagsMap = getStaticPropertyValue(ENCODE_AS_FOR_TAGS_FIELD_NAME, Map.class);
        if (encodeAsForTagsMap != null) {
            for (@SuppressWarnings("unchecked")
                 Iterator<Map.Entry> it = encodeAsForTagsMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = it.next();
                this.encodeAsForTags.put(entry.getKey().toString(), entry.getValue());
            }
        }
    }

    public boolean hasTag(String tagName) {
        return this.tags.contains(tagName);
    }

    public Set<String> getTagNames() {
        return this.tags;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public Set<String> getTagNamesThatReturnObject() {
        return this.returnObjectForTagsSet;
    }

    public Object getEncodeAsForTag(String tagName) {
        return this.encodeAsForTags.get(tagName);
    }

    public Object getDefaultEncodeAs() {
        return this.defaultEncodeAs;
    }

}
