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
package org.grails.web.pages;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import grails.core.GrailsTagLibClass;
import grails.gsp.TagLib;

import org.grails.core.gsp.DefaultGrailsTagLibClass;
import org.grails.taglib.TagLibraryLookup;

/**
 * GSP TagLibraryLookup class that's used for standalone GSP
 *
 * @author Lari Hotari
 * @since 2.4.0
 */
public final class StandaloneTagLibraryLookup extends TagLibraryLookup implements ApplicationListener<ContextRefreshedEvent> {

    Set<Object> tagLibInstancesSet;

    private StandaloneTagLibraryLookup() {

    }

    public void afterPropertiesSet() {
        registerTagLibraries();
        registerTemplateNamespace();
    }

    protected void registerTagLibraries() {
        if (this.tagLibInstancesSet != null) {
            for (Object tagLibInstance : this.tagLibInstancesSet) {
                registerTagLib(new DefaultGrailsTagLibClass(tagLibInstance.getClass()));
            }
        }
    }

    @Override
    protected void putTagLib(Map<String, Object> tags, String name, GrailsTagLibClass taglib) {
        for (Object tagLibInstance : this.tagLibInstancesSet) {
            if (tagLibInstance.getClass() == taglib.getClazz()) {
                tags.put(name, tagLibInstance);
                break;
            }
        }
    }

    public void setTagLibInstances(List<Object> tagLibInstances) {
        this.tagLibInstancesSet = new LinkedHashSet<>();
        this.tagLibInstancesSet.addAll(tagLibInstances);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        detectAndRegisterTabLibBeans();
    }

    public void detectAndRegisterTabLibBeans() {
        if (this.tagLibInstancesSet == null) {
            this.tagLibInstancesSet = new LinkedHashSet<Object>();
        }
        Collection<Object> detectedInstances = applicationContext.getBeansWithAnnotation(TagLib.class).values();
        for (Object instance : detectedInstances) {
            if (!this.tagLibInstancesSet.contains(instance)) {
                this.tagLibInstancesSet.add(instance);
                registerTagLib(new DefaultGrailsTagLibClass(instance.getClass()));
            }
        }
    }

}
