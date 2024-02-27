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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

/**
 * Detects annotated domain classes for EJB3 style mappings.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class AnnotationDomainClassArtefactHandler extends DomainClassArtefactHandler {

    private static final String JPA_MAPPING_STRATEGY = "JPA";

    private final Set<String> jpaClassNames = new HashSet<>();

    public Set<String> getJpaClassNames() {
        return this.jpaClassNames;
    }

    @Override
    public boolean isArtefactClass(Class<?> clazz) {
        boolean isJpaDomainClass = isJPADomainClass(clazz);
        if (isJpaDomainClass) {
            this.jpaClassNames.add(clazz.getName());
        }
        return super.isArtefactClass(clazz);
    }

    public static boolean isJPADomainClass(Class<?> clazz) {
        return clazz != null && clazz.getAnnotation(Entity.class) != null;
    }

}
