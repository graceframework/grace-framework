/*
 * Copyright 2004-2025 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;

import grails.artefact.Artefact;
import grails.util.GrailsNameUtils;

import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.exceptions.GrailsRuntimeException;

/**
 * Adapter for the {@link grails.core.ArtefactHandler} interface
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 1.0
 */
public class ArtefactHandlerAdapter implements ArtefactHandler {

    protected final String type;

    protected final Class<?> grailsClassType;

    protected final Class<?> grailsClassImpl;

    protected final boolean allowAbstract;

    protected final String artefactPath;

    protected final String artefactSuffix;

    /**
     * Adapter for the {@link grails.core.ArtefactHandler} interface
     *
     * @param type The type of Artefact, e.g. Application, Controller
     * @param grailsClassType The Class Interface
     * @param grailsClassImpl The Class that GrailsClass to be implements
     * @param artefactSuffix The suffix of Artefact
     */
    public ArtefactHandlerAdapter(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl,
                                  String artefactSuffix) {
        this(type, grailsClassType, grailsClassImpl, artefactSuffix, null, false);
    }

    /**
     * Adapter for the {@link grails.core.ArtefactHandler} interface
     *
     * @param type The type of Artefact, e.g. Application, Controller
     * @param grailsClassType The Class Interface
     * @param grailsClassImpl The Class that GrailsClass to be implements
     * @param artefactSuffix The suffix of Artefact
     * @param artefactPath The path of Artefact within
     */
    public ArtefactHandlerAdapter(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl,
                                  String artefactSuffix, String artefactPath) {
        this(type, grailsClassType, grailsClassImpl, artefactSuffix, artefactPath, false);
    }

    /**
     * Adapter for the {@link grails.core.ArtefactHandler} interface
     *
     * @param type The type of Artefact, e.g. Application, Controller
     * @param grailsClassType The Class Interface
     * @param grailsClassImpl The Class that GrailsClass to be implements
     * @param artefactSuffix The suffix of Artefact
     * @param allowAbstract weather allow abstract or not
     * @deprecated as 2024.0.0 in favor of {@link ArtefactHandlerAdapter(String, Class, Class, String, String, boolean)}
     */
    @Deprecated(since = "2024.0.0", forRemoval = true)
    public ArtefactHandlerAdapter(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl,
                                  String artefactSuffix, boolean allowAbstract) {
        this(type, grailsClassType, grailsClassImpl, artefactSuffix, null, allowAbstract);
    }

    /**
     * Adapter for the {@link grails.core.ArtefactHandler} interface
     *
     * @param type The type of Artefact, e.g. Application, Controller
     * @param grailsClassType The Class Interface
     * @param grailsClassImpl The Class that GrailsClass to be implements
     * @param artefactSuffix The suffix of Artefact
     * @param artefactPath The path of Artefact within
     * @param allowAbstract weather allow abstract or not
     */
    public ArtefactHandlerAdapter(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl,
                                  String artefactSuffix, String artefactPath, boolean allowAbstract) {
        this.type = type;
        this.grailsClassType = grailsClassType;
        this.grailsClassImpl = grailsClassImpl;
        this.artefactSuffix = artefactSuffix;
        this.artefactPath = artefactPath;
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
     * which returns true if the ClassNode is Grails resource
     * and the name of the ClassNode ends with the {@link #artefactSuffix}
     *
     * @param classNode The ClassNode instance
     * @return True if the ClassNode is an artefact of this type
     */
    @Override
    public boolean isArtefact(ClassNode classNode) {
        if (classNode == null) {
            return false;
        }

        if (!isArtefactClass(classNode)) {
            return false;
        }

        int modifiers = classNode.getModifiers();
        String name = classNode.getName();
        if (isValidArtefactClassNode(classNode, modifiers)) {
            return this.artefactSuffix == null || (name != null && name.endsWith(this.artefactSuffix));
        }
        return false;
    }

    protected boolean isValidArtefactClassNode(ClassNode classNode, int modifiers) {
        return !classNode.isEnum() && !classNode.isInterface()
                && !(classNode instanceof InnerClassNode)
                && (this.allowAbstract || !classNode.isAbstract());
    }

    /**
     * Subclasses can override to narrow down whether the given resource is an artefact of this type.
     * The default is to consider all files under "grails-app" to be a resource
     *
     * @param classNode The ClassNode to check
     * @return True if it is a Grails artefact
     */
    protected boolean isArtefactClass(ClassNode classNode) {
        if (classNode == null) {
            return false;
        }

        if (hasArtefactAnnotation(classNode, this.type)) {
            return true;
        }

        if (this.artefactPath != null) {
            return GrailsASTUtils.isGrailsSource(classNode, this.artefactPath);
        }
        return GrailsASTUtils.isGrailsSource(classNode);
    }

    public boolean isArtefact(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (!isArtefactClass(clazz)) {
            return false;
        }

        return  (this.artefactSuffix == null || clazz.getName().endsWith(this.artefactSuffix))
                && !Closure.class.isAssignableFrom(clazz)
                && (this.allowAbstract || !Modifier.isAbstract(clazz.getModifiers()));
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

        return hasArtefactAnnotation(clazz, this.type);
    }

    protected boolean hasArtefactAnnotation(Class<?> clazz) {
        Artefact annotation = clazz.getAnnotation(Artefact.class);
        return annotation != null;
    }

    protected boolean hasArtefactAnnotation(Class<?> clazz, String value) {
        Artefact annotation = clazz.getAnnotation(Artefact.class);
        return annotation != null && annotation.value().equals(value);
    }

    protected boolean hasArtefactAnnotation(ClassNode classNode) {
        List<AnnotationNode> annotationNodes = classNode.getAnnotations(new ClassNode(Artefact.class));

        return annotationNodes != null && annotationNodes.size() > 0;
    }

    protected boolean hasArtefactAnnotation(ClassNode classNode, String value) {
        List<AnnotationNode> annotationNodes = classNode.getAnnotations(new ClassNode(Artefact.class));

        for (AnnotationNode node : annotationNodes) {
            Expression artefactValue = node.getMember("value");
            if (artefactValue instanceof ConstantExpression) {
                Object artefactType = ((ConstantExpression) artefactValue).getValue();
                if (artefactType != null && artefactType.equals(value)) {
                    return true;
                }
            }
        }
        return false;
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
     * the initial creation to avoid looping.
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
