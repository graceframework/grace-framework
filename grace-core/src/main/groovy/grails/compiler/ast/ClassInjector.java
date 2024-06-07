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
package grails.compiler.ast;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.grails.io.support.FileSystemResource;
import org.grails.io.support.Resource;

/**
 * When implemented allows additional properties to be injected into Grails
 * classes at compile time (ie when they are loaded by the GroovyClassLoader).
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.2
 */
public interface ClassInjector {

    int PRIVATE_STATIC_MODIFIER = Modifier.PRIVATE | Modifier.STATIC;

    /**
     * Handles injection of properties, methods etc. into a class.
     *
     * @param source The source unit
     * @param context The generator context
     * @param classNode The ClassNode instance
     */
    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode);

    /**
     * Handles injection of properties, methods etc. into a class.
     *
     * @param source The source unit
     * @param classNode The ClassNode instance
     */
    void performInjection(SourceUnit source, ClassNode classNode);

    /**
     * Handles injection of properties, methods etc. into a class.
     *
     * @param source The source unit
     * @param classNode The ClassNode instance
     */
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode);

    /**
     * Returns whether this injector should inject
     *
     * @param url The URL of the source file
     * @return true if injection should occur
     * @deprecated since 2022.3.0, in favor of {@link #shouldInject(ClassNode)}
     */
    @Deprecated(forRemoval = true, since = "2023.0.0")
    default boolean shouldInject(URL url) {
        return true;
    }

    /**
     * Returns whether this injector should inject
     *
     * @param classNode The classNode of the Groovy source
     * @return true if injection should occur
     */
    default boolean shouldInject(ClassNode classNode) {
        String filename = classNode.getModule().getContext().getName();
        if (filename == null) {
            return false;
        }
        Resource resource = new FileSystemResource(filename);
        if (resource.exists()) {
            try {
                URL url = resource.getURL();
                return shouldInject(url);
            }
            catch (IOException ignored) {
            }
        }
        return false;
    }

}
