/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.core.artefact;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import grails.artefact.Artefact;
import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsClass;
import grails.core.GrailsDomainClass;

import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.DefaultGrailsDomainClass;
import org.grails.datastore.mapping.model.MappingContext;

/**
 * Evaluates the conventions that define a domain class in Grails.
 *
 * @author Graeme Rocher
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Michael Yan
 * @since 0.5
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class DomainClassArtefactHandler extends ArtefactHandlerAdapter implements Ordered {

    public static final String TYPE = "Domain";

    public static final String PATH = "domain";

    public static final String PLUGIN_NAME = "domainClass";

    private static final Set<Class<? extends Annotation>> ENTITY_ANNOTATIONS = new HashSet<>();

    static {
        ENTITY_ANNOTATIONS.add(grails.persistence.Entity.class);
        ClassLoader classLoader = DomainClassArtefactHandler.class.getClassLoader();
        if (ClassUtils.isPresent("org.grails.datastore.gorm.AbstractDatastoreApi", classLoader)) {
            try {
                ENTITY_ANNOTATIONS.add((Class<? extends Annotation>) classLoader.loadClass("grails.gorm.annotation.Entity"));
            }
            catch (ClassNotFoundException ignored) {
            }
        }
    }

    public DomainClassArtefactHandler() {
        super(TYPE, GrailsDomainClass.class, DefaultGrailsDomainClass.class, null, PATH, true);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public GrailsClass newArtefactClass(Class<?> artefactClass) {
        return new DefaultGrailsDomainClass(artefactClass);
    }

    public GrailsClass newArtefactClass(Class<?> artefactClass, MappingContext mappingContext) {
        return new DefaultGrailsDomainClass(artefactClass, mappingContext);
    }

    @Override
    protected boolean isValidArtefactClassNode(ClassNode classNode, int modifiers) {
        return !classNode.isEnum() && !(classNode instanceof InnerClassNode);
    }

    @Override
    public boolean isArtefactClass(ClassNode classNode) {
        if (GrailsASTUtils.hasAnyAnnotations(classNode, ENTITY_ANNOTATIONS.toArray(new Class[0]))) {
            return true;
        }
        return !GrailsASTUtils.isJpaEntityClass(classNode) && super.isArtefactClass(classNode);
    }

    @Override
    public boolean isArtefactClass(Class<?> clazz) {
        return isDomainClass(clazz);
    }

    public static boolean isDomainClass(Class<?> clazz, boolean allowProxyClass) {
        boolean retval = isDomainClass(clazz);
        if (!retval && allowProxyClass && clazz != null && clazz.getSimpleName().contains("$")) {
            retval = isDomainClass(clazz.getSuperclass());
        }
        return retval;
    }

    public static boolean isDomainClass(Class<?> clazz) {
        return clazz != null && doIsDomainClassCheck(clazz);
    }

    private static boolean doIsDomainClassCheck(Class<?> clazz) {
        if (Closure.class.isAssignableFrom(clazz) || clazz.isEnum()) {
            return false;
        }

        try {
            Artefact artefactAnn = clazz.getAnnotation(Artefact.class);
            if (artefactAnn != null && artefactAnn.value().equals(DomainClassArtefactHandler.TYPE)) {
                return true;
            }
        }
        catch (Exception ignored) {
        }

        try {
            Annotation[] annotations = clazz.getAnnotations();
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annType = annotation.annotationType();
                if (ENTITY_ANNOTATIONS.contains(annType)) {
                    return true;
                }
            }
        }
        catch (Exception ignored) {
        }

        return false;
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
