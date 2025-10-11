/*
 * Copyright 2021-2025 the original author or authors.
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
package org.grails.plugins;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.plugins.exceptions.PluginException;
import grails.plugins.metadata.GrailsPlugin;
import grails.util.GrailsMetaClassUtils;
import grails.util.GrailsNameUtils;

import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * Wrapper Grails class for plugins.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsPluginClass implements GrailsClass {

    public static final String GRAILS_PLUGIN = "GrailsPlugin";

    private final Class<?> clazz;

    private final String fullName;

    private final String name;

    private final String packageName;

    private final String naturalName;

    private final String shortName;

    private final String propertyName;

    private final String logicalPropertyName;

    private ClassPropertyFetcher classPropertyFetcher;

    protected GrailsApplication grailsApplication;

    private final boolean isAbstract;

    public GrailsPluginClass(Class<?> clazz) {
        Assert.notNull(clazz, "Clazz parameter should not be null");

        this.clazz = clazz;
        this.fullName = clazz.getName();
        this.packageName = ClassUtils.getPackageName(clazz);
        this.naturalName = GrailsNameUtils.getNaturalName(clazz.getName());
        this.shortName = ClassUtils.getShortName(clazz);
        this.name = GrailsNameUtils.getLogicalName(clazz, GRAILS_PLUGIN);
        this.propertyName = GrailsNameUtils.getPropertyNameRepresentation(this.shortName);
        if (!StringUtils.hasText(this.name)) {
            this.logicalPropertyName = this.propertyName;
        }
        else {
            this.logicalPropertyName = GrailsNameUtils.getPropertyNameRepresentation(this.name);
        }
        this.isAbstract = Modifier.isAbstract(clazz.getModifiers());
    }

    @Override
    public String getPluginName() {
        GrailsPlugin ann = getClazz().getAnnotation(GrailsPlugin.class);
        return ann != null ? ann.name() : null;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    public GrailsApplication getApplication() {
        return this.grailsApplication;
    }

    public String getShortName() {
        return this.shortName;
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    @SuppressWarnings("deprecation")
    public Object newInstance() {
        try {
            Constructor<?> defaultConstructor = getClazz().getDeclaredConstructor();
            if (!defaultConstructor.isAccessible()) {
                defaultConstructor.setAccessible(true);
            }
            return defaultConstructor.newInstance();
        }
        catch (Exception e) {
            Throwable targetException;
            if (e instanceof InvocationTargetException) {
                targetException = ((InvocationTargetException) e).getTargetException();
            }
            else {
                targetException = e;
            }
            throw new PluginException("Could not create a new instance of class [" +
                    getClazz().getName() + "]!", targetException);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getNaturalName() {
        return this.naturalName;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public String getLogicalPropertyName() {
        return this.logicalPropertyName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public Object getReferenceInstance() {
        Object obj = BeanUtils.instantiateClass(this.clazz);
        if (obj instanceof GroovyObject) {
            ((GroovyObject) obj).setMetaClass(getMetaClass());
        }
        return obj;
    }

    public <T> T getPropertyValue(String propName, Class<T> type) {
        return ClassPropertyFetcher.getStaticPropertyValue(getClazz(), propName, type);
    }

    public Object getPropertyValue(String propName) {
        return ClassPropertyFetcher.getStaticPropertyValue(getClazz(), name, Object.class);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public boolean hasProperty(String propName) {
        return ClassPropertyFetcher.getPropertyType(getClazz(), propName) != null;
    }

    public MetaClass getMetaClass() {
        return GrailsMetaClassUtils.getExpandoMetaClass(getClazz());
    }

    @Override
    public String toString() {
        return "GrailsPlugin > " + getName();
    }

}
