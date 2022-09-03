/*
 * Copyright 2015-2022 the original author or authors.
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
package org.grails.io.support

import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.ClassReader
import groovyjarjarasm.asm.ClassVisitor
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type

import grails.util.BuildSettings

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class MainClassFinder {

    private static final Type STRING_ARRAY_TYPE = Type.getType(String[].class)

    private static final Type MAIN_METHOD_TYPE = Type.getMethodType(Type.VOID_TYPE, STRING_ARRAY_TYPE)

    private static final String MAIN_METHOD_NAME = 'main'

    static final Map<String, String> MAIN_CLASSES = new ConcurrentHashMap<>()

    /**
     * Searches for the main class relative to the give path that is within the project tree
     *
     * @param path The path as a URI
     * @return The name of the main class
     */
    static String searchMainClass(URI path) {
        if (!path) {
            return null
        }

        def pathStr = path.toString()
        if (MAIN_CLASSES.containsKey(pathStr)) {
            return MAIN_CLASSES.get(pathStr)
        }

        try {
            File file = path ? Paths.get(path).toFile() : null
            def rootDir = findRootDirectory(file)

            def classesDir = BuildSettings.CLASSES_DIR
            Collection<File> searchDirs
            if (classesDir == null) {
                searchDirs = []
            }
            else {
                searchDirs = [classesDir]
            }

            if (rootDir) {
                def rootClassesDir = new File(rootDir, BuildSettings.BUILD_CLASSES_PATH)
                if (rootClassesDir.exists()) {
                    searchDirs << rootClassesDir
                }

                rootClassesDir = new File(rootDir, 'build/classes/groovy/main')
                if (rootClassesDir.exists()) {
                    searchDirs << rootClassesDir
                }
            }

            String mainClass = null

            for (File dir in searchDirs) {
                mainClass = findMainClass(dir)
                if (mainClass) {
                    break
                }
            }
            if (mainClass != null) {
                MAIN_CLASSES.put(pathStr, mainClass)
            }
            return mainClass
        }
        catch (Throwable e) {
            return null
        }
    }

    private static File findRootDirectory(File file) {
        if (file) {
            def parent = file.parentFile

            while (parent != null) {
                if (new File(parent, 'build.gradle').exists() || new File(parent, 'grails-app').exists()) {
                    return parent
                }
                parent = parent.parentFile
            }
        }
        null
    }

    static String findMainClass(File rootFolder = BuildSettings.CLASSES_DIR) {
        if (rootFolder == null) {
            // try current directory
            rootFolder = new File('build/classes/main')
        }

        def rootFolderPath = rootFolder.canonicalPath
        if (MAIN_CLASSES.containsKey(rootFolderPath)) {
            return MAIN_CLASSES.get(rootFolderPath)
        }

        if (!rootFolder.exists()) {
            return null // nothing to do
        }
        if (!rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid root folder '$rootFolder'")
        }
        String prefix = "${rootFolderPath}/"
        def stack = new ArrayDeque<File>()
        stack.push rootFolder

        while (!stack.empty) {
            File file = stack.pop()
            if (file.isFile()) {
                InputStream inputStream = file.newInputStream()
                try {
                    def classReader = new ClassReader(inputStream)

                    if (isMainClass(classReader)) {
                        def mainClassName = classReader.getClassName().replace('/', '.').replace('\\', '.')
                        MAIN_CLASSES.put(rootFolderPath, mainClassName)
                        return mainClassName
                    }
                }
                finally {
                    inputStream?.close()
                }
            }
            if (file.isDirectory()) {
                def files = file.listFiles()?.findAll { File f ->
                    (f.isDirectory() && !f.name.startsWith('.') && !f.hidden) ||
                            (f.isFile() && f.name.endsWith(GrailsResourceUtils.CLASS_EXTENSION))
                }

                if (files) {
                    for (File sub in files) {
                        stack.push(sub)
                    }
                }
            }
        }
        null
    }

    protected static boolean isMainClass(ClassReader classReader) {
        if (classReader.superName?.startsWith('grails/boot/config/')) {
            def mainMethodFinder = new MainMethodFinder()
            classReader.accept(mainMethodFinder, ClassReader.SKIP_CODE)
            return mainMethodFinder.found
        }
        false
    }

    @CompileStatic
    static class MainMethodFinder extends ClassVisitor {

        boolean found = false

        MainMethodFinder() {
            super(Opcodes.ASM4)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (!found) {
                if (isAccess(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC)
                        && MAIN_METHOD_NAME.equals(name)
                        && MAIN_METHOD_TYPE.getDescriptor().equals(desc)) {
                    this.found = true
                }
            }
            null
        }

        private boolean isAccess(int access, int... requiredOpsCodes) {
            !requiredOpsCodes.any { int requiredOpsCode -> (access & requiredOpsCode) == 0 }
        }

    }

}
