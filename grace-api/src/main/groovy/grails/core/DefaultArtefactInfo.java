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
package grails.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Mutable holder of artefact info.
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 */
public class DefaultArtefactInfo implements ArtefactInfo {

    private final LinkedList<GrailsClass> grailsClasses = new LinkedList<>();

    private Class<?>[] classes;

    private Map<String, GrailsClass> grailsClassesByName = new LinkedHashMap<>();

    private Map<String, Class<?>> classesByName = new LinkedHashMap<>();

    private final Map<String, GrailsClass> logicalPropertyNameToClassMap = new HashMap<>();

    private GrailsClass[] grailsClassesArray;

    /**
     * <p>Call to add a new class to this info object.</p>
     * <p>You <b>must</b> call refresh() later to update the arrays</p>
     * @param artefactClass
     */
    public synchronized void addGrailsClass(GrailsClass artefactClass) {
        addGrailsClassInternal(artefactClass, false);
    }

    private void addGrailsClassInternal(GrailsClass artefactClass, boolean atStart) {
        this.grailsClassesByName = new LinkedHashMap<>(this.grailsClassesByName);
        this.classesByName = new LinkedHashMap<>(this.classesByName);

        Class<?> actualClass = artefactClass.getClazz();
        boolean addToGrailsClasses = true;
        if (artefactClass instanceof InjectableGrailsClass) {
            addToGrailsClasses = ((InjectableGrailsClass) artefactClass).getAvailable();
        }
        if (addToGrailsClasses) {
            GrailsClass oldVersion = this.grailsClassesByName.put(actualClass.getName(), artefactClass);
            this.grailsClasses.remove(oldVersion);
        }
        this.classesByName.put(actualClass.getName(), actualClass);
        this.logicalPropertyNameToClassMap.put(artefactClass.getLogicalPropertyName(), artefactClass);

        if (!this.grailsClasses.contains(artefactClass)) {
            if (atStart) {
                this.grailsClasses.addFirst(artefactClass);
            }
            else {
                this.grailsClasses.addLast(artefactClass);
            }
        }
    }

    /**
     * Refresh the arrays generated from the maps.
     */
    public synchronized void updateComplete() {
        this.grailsClassesByName = Collections.unmodifiableMap(this.grailsClassesByName);
        this.classesByName = Collections.unmodifiableMap(this.classesByName);

        this.grailsClassesArray = this.grailsClasses.toArray(new GrailsClass[0]);
        // Make classes array
        this.classes = this.classesByName.values().toArray(new Class[0]);
    }

    public Class<?>[] getClasses() {
        return this.classes;
    }

    public GrailsClass[] getGrailsClasses() {
        return this.grailsClassesArray;
    }

    public Map<String, Class<?>> getClassesByName() {
        return this.classesByName;
    }

    public Map<String, GrailsClass> getGrailsClassesByName() {
        return this.grailsClassesByName;
    }

    public GrailsClass getGrailsClass(String name) {
        return this.grailsClassesByName.get(name);
    }

    public GrailsClass getGrailsClassByLogicalPropertyName(String logicalName) {
        return this.logicalPropertyNameToClassMap.get(logicalName);
    }

    public void addOverridableGrailsClass(GrailsClass artefactGrailsClass) {
        addGrailsClassInternal(artefactGrailsClass, true);
    }

}
