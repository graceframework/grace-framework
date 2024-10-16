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
package grails.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.control.SourceUnit;

import grails.util.GrailsNameUtils;

import org.grails.core.exceptions.GrailsRuntimeException;
import org.grails.io.support.FileSystemResource;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.io.support.Resource;
import org.grails.io.support.UrlResource;

/**
 * Adapter for the {@link grails.core.ArtefactHandler} interface
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 * @since 1.0
 */
public class ArtefactHandlerAdapter implements ArtefactHandler {

    protected final String type;

    protected final Class<?> grailsClassType;

    protected final Class<?> grailsClassImpl;

    protected boolean allowAbstract;

    protected final String artefactSuffix;

    public ArtefactHandlerAdapter(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl, String artefactSuffix) {
        this.artefactSuffix = artefactSuffix;
        this.type = type;
        this.grailsClassType = grailsClassType;
        this.grailsClassImpl = grailsClassImpl;
    }

    public ArtefactHandlerAdapter(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl,
            String artefactSuffix, boolean allowAbstract) {
        this.artefactSuffix = artefactSuffix;
        this.type = type;
        this.grailsClassType = grailsClassType;
        this.grailsClassImpl = grailsClassImpl;
        this.allowAbstract = allowAbstract;
    }

    public String getPluginName() {
        return GrailsNameUtils.getPropertyName(this.type);
    }

    public String getType() {
        return this.type;
    }

    /**
     * Default implementation of {@link grails.core.ArtefactHandler#isArtefact(org.codehaus.groovy.ast.ClassNode)}
     * which returns true if the ClassNode passes the {@link #isArtefactResource(org.grails.io.support.Resource)} method
     * and the name of the ClassNode ends with the {@link #artefactSuffix}
     *
     * @param classNode The ClassNode instance
     * @return True if the ClassNode is an artefact of this type
     */
    @Override
    public boolean isArtefact(ClassNode classNode) {
        SourceUnit source = classNode.getModule().getContext();
        String filename = source.getName();
        if (filename == null) {
            return false;
        }

        URL url = null;
        Resource resource = new FileSystemResource(filename);
        if (resource.exists()) {
            try {
                url = resource.getURL();
            }
            catch (IOException ignored) {
            }
        }

        if (url == null) {
            return false;
        }

        try {
            UrlResource urlResource = new UrlResource(url);
            if (!isArtefactResource(urlResource)) {
                return false;
            }
        }
        catch (IOException e) {
            return false;
        }

        int modifiers = classNode.getModifiers();
        String name = classNode.getName();
        if (isValidArtefactClassNode(classNode, modifiers)) {
            return name != null && this.artefactSuffix != null && name.endsWith(this.artefactSuffix);
        }
        return false;
    }

    protected boolean isValidArtefactClassNode(ClassNode classNode, int modifiers) {
        return !classNode.isEnum() && !classNode.isInterface() && !(classNode instanceof InnerClassNode);
    }

    /**
     * Subclasses can override to narrow down whether the given resource is an artefact of this type.
     * The default is to consider all files under "grails-app" to be a resource
     *
     * @param resource The resource
     * @return True if it is a Grails artefact
     */
    protected boolean isArtefactResource(Resource resource) throws IOException {
        return GrailsResourceUtils.isGrailsResource(resource);
    }

    public final boolean isArtefact(Class<?> aClass) {
        if (aClass == null) {
            return false;
        }

        try {
            if (isArtefactClass(aClass)) {
                return true;
            }
        }
        catch (Throwable t) {
            throw new GrailsRuntimeException("Failed to introspect class: " + aClass, t);
        }

        return false;
    }

    /**
     * <p>Checks that class's name ends in the suffix specified for this handler.</p>
     * <p>Override for more complex criteria</p>
     * @param clazz The class to check
     * @return true if it is an artefact of this type
     */
    public boolean isArtefactClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        boolean ok = clazz.getName().endsWith(this.artefactSuffix) && !Closure.class.isAssignableFrom(clazz);
        if (ok && !this.allowAbstract) {
            ok = !Modifier.isAbstract(clazz.getModifiers());
        }
        return ok;
    }

    /**
     * <p>Creates new GrailsClass derived object using the type supplied in constructor. May not perform
     * optimally but is a convenience.</p>
     * @param artefactClass Creates a new artefact for the given class
     * @return An instance of the GrailsClass interface representing the artefact
     */
    public GrailsClass newArtefactClass(Class<?> artefactClass) {
        try {
            Constructor<?> c = this.grailsClassImpl.getDeclaredConstructor(Class.class);
            // TODO GRAILS-720 plugin class instance created here first
            return (GrailsClass) c.newInstance(new Object[] {artefactClass});
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new GrailsRuntimeException("Unable to locate constructor with Class parameter for " + artefactClass, e);
        }
        catch (InvocationTargetException | InstantiationException e) {
            throw new GrailsRuntimeException("Error instantiated artefact class [" + artefactClass + "] of type [" + this.grailsClassImpl + "]: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
    }

    /**
     * Sets up the relationships between the domain classes, this has to be done after
     * the intial creation to avoid looping.
     */
    public void initialize(ArtefactInfo artefacts) {
        // do nothing
    }

    public GrailsClass getArtefactForFeature(Object feature) {
        return null;
    }

    public boolean isArtefactGrailsClass(GrailsClass artefactGrailsClass) {
        return this.grailsClassType.isAssignableFrom(artefactGrailsClass.getClass());
    }

}
