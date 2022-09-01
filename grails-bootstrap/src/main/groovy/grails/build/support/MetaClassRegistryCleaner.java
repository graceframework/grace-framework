/*
 * Copyright 2011-2022 the original author or authors.
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
package grails.build.support;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaClassRegistryChangeEvent;
import groovy.lang.MetaClassRegistryChangeEventListener;
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl;

/**
 * Allows clean-up of changes made to the MetaClassRegistry.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public final class MetaClassRegistryCleaner implements MetaClassRegistryChangeEventListener {

    private Map<Class, Object> alteredClasses = new ConcurrentHashMap<>();

    private Map<IdentityWeakReference, Object> alteredInstances = new ConcurrentHashMap<>();

    private static final Object NO_CUSTOM_METACLASS = new Object();

    private static boolean cleaning;

    private static final MetaClassRegistryCleaner INSTANCE = new MetaClassRegistryCleaner();

    private MetaClassRegistryCleaner() {
    }

    public static MetaClassRegistryCleaner createAndRegister() {

        MetaClassRegistry metaClassRegistry = GroovySystem.getMetaClassRegistry();
        MetaClassRegistryChangeEventListener[] listeners = metaClassRegistry.getMetaClassRegistryChangeEventListeners();
        boolean registered = false;
        for (MetaClassRegistryChangeEventListener listener : listeners) {
            if (listener == INSTANCE) {
                registered = true;
                break;
            }
        }
        if (!registered) {
            GroovySystem.getMetaClassRegistry().addMetaClassRegistryChangeEventListener(INSTANCE);
        }
        return INSTANCE;
    }

    public static void cleanAndRemove(MetaClassRegistryCleaner cleaner) {
        cleaner.clean();
        GroovySystem.getMetaClassRegistry().removeMetaClassRegistryChangeEventListener(cleaner);
    }

    public static void addAlteredMetaClass(Class cls, MetaClass altered) {
        INSTANCE.alteredClasses.put(cls, altered);
    }

    public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
        if (!cleaning) {
            MetaClass oldMetaClass = cmcu.getOldMetaClass();
            Class classToUpdate = cmcu.getClassToUpdate();
            Object instanceToUpdate = cmcu.getInstance();
            if (instanceToUpdate == null && (cmcu.getNewMetaClass() instanceof ExpandoMetaClass)) {
                updateMetaClassOfClass(oldMetaClass, classToUpdate);
            }
            else if (instanceToUpdate != null) {
                updateMetaClassOfInstance(oldMetaClass, instanceToUpdate);
            }
        }
    }

    private void updateMetaClassOfInstance(MetaClass oldMetaClass, Object instanceToUpdate) {
        IdentityWeakReference key = new IdentityWeakReference(instanceToUpdate);
        if (oldMetaClass != null) {
            Object current = this.alteredInstances.get(key);
            if (current == null || current == NO_CUSTOM_METACLASS) {
                this.alteredInstances.put(key, oldMetaClass);
            }
        }
        else {
            this.alteredInstances.put(key, NO_CUSTOM_METACLASS);
        }
    }

    private void updateMetaClassOfClass(MetaClass oldMetaClass, Class classToUpdate) {
        if (oldMetaClass != null && !(oldMetaClass.getClass().getName().equals("groovy.mock.interceptor.MockProxyMetaClass"))) {
            Object current = this.alteredClasses.get(classToUpdate);
            if (current == null) {
                this.alteredClasses.put(classToUpdate, oldMetaClass);
            }
        }
        else {
            this.alteredClasses.put(classToUpdate, NO_CUSTOM_METACLASS);
        }
    }

    public synchronized void clean() {
        try {
            cleaning = true;
            MetaClassRegistryImpl registry = (MetaClassRegistryImpl) GroovySystem.getMetaClassRegistry();
            cleanMetaClassOfClass(registry);
            cleanMetaClassOfInstance(registry);
        }
        finally {
            cleaning = false;
        }
    }

    private void cleanMetaClassOfInstance(MetaClassRegistryImpl registry) {
        List<IdentityWeakReference> keys = new ArrayList<>(this.alteredInstances.keySet());
        for (IdentityWeakReference key : keys) {
            Object instance = key.get();
            if (instance != null) {
                Object alteredMetaClass = this.alteredInstances.get(key);
                if (alteredMetaClass == NO_CUSTOM_METACLASS) {
                    alteredMetaClass = null;
                }
                registry.setMetaClass(instance, (MetaClass) alteredMetaClass);
            }
        }
        this.alteredInstances.clear();
    }

    private void cleanMetaClassOfClass(MetaClassRegistryImpl registry) {
        Set<Class> classes = new HashSet<>(this.alteredClasses.keySet());
        for (Class aClass : classes) {
            Object alteredMetaClass = this.alteredClasses.get(aClass);
            if (alteredMetaClass == NO_CUSTOM_METACLASS) {
                registry.removeMetaClass(aClass);
            }
            else {
                registry.setMetaClass(aClass, (MetaClass) alteredMetaClass);
            }
        }
        this.alteredClasses.clear();
    }

    private static final class IdentityWeakReference extends WeakReference<Object> {

        private int hash;

        IdentityWeakReference(Object referent) {
            super(referent);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return get() == ((IdentityWeakReference) obj).get();
        }
    }
}
