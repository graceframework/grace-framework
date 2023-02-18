/*
 * Copyright 2002-2023 the original author or authors.
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
package grails.io

import java.nio.file.Paths

import groovy.io.FileType
import groovy.transform.CompileStatic
import groovy.transform.Memoized

import grails.util.BuildSettings

import org.grails.io.support.Resource
import org.grails.io.support.SpringIOUtils
import org.grails.io.support.UrlResource

/**
 * Utility methods for performing I/O operations.
 *
 * @author Graeme Rocher
 * @since 2.4
 */
@CompileStatic
class IOUtils {

    public static final String RESOURCE_JAR_PREFIX = '.jar!'
    public static final String RESOURCE_WAR_PREFIX = '.war!'

    private static String applicationDirectory

    /**
     * Gracefully opens a stream for a file, throwing exceptions where appropriate. Based off the commons-io method
     *
     * @param file The file
     * @return The stream
     */
    static BufferedInputStream openStream(File file) {
        if (file.exists()) {
            if (file.directory) {
                throw new IOException("File $file exists but is a directory")
            }
            else if (!file.canRead()) {
                throw new IOException("File $file cannot be read")
            }
            else {
                file.newInputStream()
            }
        }
        else {
            throw new FileNotFoundException("File $file does not exist")
        }
    }

    /**
     * Convert a reader to a String, reading the data from the reader
     * @param reader The reader
     * @return The string
     */
    static String toString(Reader reader) {
        StringWriter writer = new StringWriter()
        SpringIOUtils.copy reader, writer
        writer.toString()
    }

    /**
     * Convert a stream to a String, reading the data from the stream
     * @param stream The stream
     * @return The string
     */
    static String toString(InputStream stream, String encoding = null) {
        StringWriter writer = new StringWriter()
        copy stream, writer, encoding
        writer.toString()
    }

    /**
     * Copy an InputStream to the given writer with the given encoding
     * @param input The input
     * @param output The writer
     * @param encoding The encoding
     */
    static void copy(InputStream input, Writer output, String encoding = null) {
        InputStreamReader reader = encoding ? new InputStreamReader(input, encoding) : new InputStreamReader(input)
        SpringIOUtils.copy(reader, output)
    }

    /**
     * Finds a JAR file for the given class
     * @param targetClass The target class
     * @return The JAR file
     */
    static File findJarFile(Class targetClass) {
        URL resource = findClassResource(targetClass)
        findJarFile(resource)
    }

    /**
     * Whether the given URL is within a binary like a JAR or WAR file
     * @param url The URL
     * @return True if it is
     */
    static boolean isWithinBinary(URL url) {
        String protocol = url.protocol
        protocol == null || protocol != 'file'
    }

    /**
     * Finds a JAR for the given resource
     *
     * @param resource The resource
     * @return The JAR file or null if it can't be found
     */
    static File findJarFile(Resource resource) {
        String absolutePath = resource?.getFilename()
        if (absolutePath) {
            String jarPath = absolutePath.substring('file:'.length(), absolutePath.lastIndexOf('!'))
            new File(jarPath)
        }
        null
    }

    /**
     * Finds a JAR for the given resource
     *
     * @param resource The resource
     * @return The JAR file or null if it can't be found
     */
    static File findJarFile(URL resource) {
        if (resource?.protocol == 'jar') {
            String absolutePath = resource?.path
            if (absolutePath) {
                try {
                    return Paths.get(new URL(absolutePath.substring(0, absolutePath.lastIndexOf('!'))).toURI()).toFile()
                }
                catch (MalformedURLException ignored) {
                    return null
                }
            }
        }
        null
    }

    /**
     * Returns the URL resource for the location on disk of the given class or null if it cannot be found
     *
     * @param targetClass The target class
     * @return The URL to class file or null
     */
    static URL findClassResource(Class targetClass) {
        targetClass.getResource('/' + targetClass.name.replace('.', '/') + '.class')
    }

    /**
     * Returns a URL that represents the root classpath resource where the given class was loaded from
     *
     * @param targetClass The target class
     * @return The URL to class file or null
     */
    static URL findRootResource(Class targetClass) {
        String pathToClassFile = '/' + targetClass.name.replace('.', '/') + '.class'
        URL classRes = targetClass.getResource(pathToClassFile)
        if (classRes) {
            String rootPath = classRes.toString() - pathToClassFile
            return new URL("$rootPath/")
        }
        throw new IllegalStateException('Root classpath resource not found! Check your disk permissions')
    }

    /**
     * This method differs from {@link #findRootResource(java.lang.Class)} in that
     * it will find the root URL where to load resources defined in src/main/resources
     *
     * At development time this with be build/main/resources, but in production it will be relative to the class.
     *
     * @param targetClass
     * @param path
     * @return
     */
    static URL findRootResourcesURL(Class targetClass) {
        String pathToClassFile = '/' + targetClass.name.replace('.', '/') + '.class'
        URL classRes = targetClass.getResource(pathToClassFile)
        if (classRes) {
            String rootPath = classRes.toString() - pathToClassFile
            if (rootPath.endsWith(BuildSettings.BUILD_CLASSES_PATH)) {
                rootPath = rootPath.replace('/build/classes/groovy/', '/build/resources/')
            }
            else {
                rootPath = "$rootPath/"
            }
            return new URL(rootPath)
        }
        null
    }

    /**
     * Returns the URL resource for the location on disk of the given class or null if it cannot be found
     *
     * @param targetClass The target class
     * @return The URL to class file or null
     */
    static URL findJarResource(Class targetClass) {
        URL classUrl = findClassResource(targetClass)
        if (classUrl != null) {
            String urlPath = classUrl.toString()
            int bang = urlPath.lastIndexOf('!')

            if (bang > -1) {
                String newPath = urlPath.substring(0, bang)
                return new URL("${newPath}!/")
            }
        }
        null
    }

    /**
     * Finds a URL within a JAR relative (from the root) to the given class
     *
     * @param targetClass
     * @param path
     * @return
     */
    static URL findResourceRelativeToClass(Class targetClass, String path) {
        String pathToClassFile = '/' + targetClass.name.replace('.', '/') + '.class'
        URL classRes = targetClass.getResource(pathToClassFile)
        if (classRes) {
            String rootPath = classRes.toString() - pathToClassFile
            if (rootPath.endsWith(BuildSettings.BUILD_CLASSES_PATH)) {
                rootPath = rootPath.replace('/build/classes/groovy/', '/build/resources/')
            }
            return new URL("$rootPath$path")
        }
        null
    }

    @Memoized
    static File findApplicationDirectoryFile() {
        String directory = findApplicationDirectory()
        if (directory) {
            File f = new File(directory)
            if (f.exists()) {
                return f
            }
        }
        null
    }

    /**
     * Finds the application directory for the given class
     *
     * @param targetClass The target class
     * @return The application directory or null if it can't be found
     */
    static File findApplicationDirectoryFile(Class targetClass) {
        URL rootResource = findRootResource(targetClass)
        if (rootResource != null) {
            try {
                File rootFile = new UrlResource(rootResource).file.canonicalFile
                String rootPath = rootFile.path
                String buildClasspath = BuildSettings.BUILD_CLASSES_PATH.replace('/', File.separator)
                if (rootPath.contains(buildClasspath)) {
                    return new File(rootPath - buildClasspath)
                }
                File appDir = findGrailsApp(rootFile)
                if (appDir != null) {
                    return appDir
                }
            }
            catch (FileNotFoundException ignored) {
                return null
            }
        }
        null
    }

    /**
     * Finds a source file for the given class name
     *
     * @param className The class name
     * @return The source file
     */
    @Memoized
    static File findSourceFile(String className) {
        File applicationDir = BuildSettings.BASE_DIR
        File file = null
        if (applicationDir != null) {
            String fileName = className.replace('.' as char, File.separatorChar) + '.groovy'
            List<File> allDirs = [new File(applicationDir, 'src/main/groovy')]

            for (String dir : ['grails-app', 'app']) {
                File grailsAppDir = new File(applicationDir, dir)
                if (grailsAppDir.exists()) {
                    grailsAppDir.eachFile(FileType.DIRECTORIES) { File d ->
                        if (!d.isHidden() && !d.name.startsWith('.')) {
                            allDirs.add(d)
                        }
                    }
                    break
                }
            }
            for (File dir in allDirs) {
                File possibleFile = new File(dir, fileName)
                if (possibleFile.exists()) {
                    file = possibleFile
                    break
                }
            }
        }
        file
    }

    /**
     * Finds the directory where the Application class is contained
     * @return The application directory
     */
    @Memoized
    static String findApplicationDirectory() {
        if (applicationDirectory) {
            return applicationDirectory
        }

        String location = null
        try {
            String mainClassName = System.getProperty(BuildSettings.MAIN_CLASS_NAME)
            if (!mainClassName) {
                List<StackTraceElement> stackTraceElements = Arrays.asList(Thread.currentThread().getStackTrace()).reverse()
                if (stackTraceElements) {
                    for (lastElement in stackTraceElements) {
                        String className = lastElement.className
                        String methodName = lastElement.methodName
                        if (className.endsWith('.Application') && methodName == '<clinit>') {
                            mainClassName = className
                            break
                        }
                    }
                }
            }
            if (mainClassName) {
                final Class<?> mainClass = Thread.currentThread().contextClassLoader.loadClass(mainClassName)
                final URL classResource = mainClass ? findClassResource(mainClass) : null
                if (classResource) {
                    File file = new UrlResource(classResource).getFile()
                    String path = file.canonicalPath
                    String buildClassesPath = BuildSettings.BUILD_CLASSES_PATH.replace('/', File.separator)
                    if (path.contains(buildClassesPath)) {
                        location = path.substring(0, path.indexOf(buildClassesPath) - 1)
                    }
                    else {
                        File appDir = findGrailsApp(file)
                        if (appDir != null) {
                            location = appDir.canonicalPath
                        }
                    }
                }
            }
        }
        catch (ClassNotFoundException | IOException ignored) {
        }
        applicationDirectory = location
        location
    }

    private static File findGrailsApp(File file) {
        File parent = file.parentFile
        while (parent != null) {
            for (String dir : ['grails-app', 'app']) {
                if (new File(parent, dir).isDirectory()) {
                    return parent
                }
            }
            parent = parent.parentFile
        }
        null
    }

}
