/*
 * Copyright 2022-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package grails.cli.generator

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString

import grails.build.logging.GrailsConsole
import grails.dev.commands.io.FileSystemInteraction
import grails.dev.commands.io.FileSystemInteractionImpl
import grails.dev.commands.template.TemplateRenderer
import grails.dev.commands.template.TemplateRendererImpl
import grails.util.BuildSettings
import org.grails.config.CodeGenConfig
import org.grails.io.support.DefaultResourceLoader
import org.grails.io.support.Resource
import org.grails.io.support.SpringIOUtils

/**
 * @author Michael Yan
 * @since 2023.2.0
 */
class AbstractGenerator implements Generator {

    protected GrailsConsole console = GrailsConsole.getInstance()

    protected File baseDir

    protected CodeGenConfig loadApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File(getBaseDir(), "app/conf/application.yml")
        File applicationGroovy = new File(getBaseDir(), "app/conf/application.groovy")
        if (applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        if (applicationGroovy.exists()) {
            config.loadGroovy(applicationGroovy)
        }
        config
    }

    protected File getBaseDir() {
        this.baseDir ?: BuildSettings.BASE_DIR
    }

    protected void setBaseDir(File baseDir) {
        this.baseDir = baseDir
    }

    protected String getTemplateRoot() {
        "templates/generators/$name"
    }

    protected TemplateRenderer getTemplateRenderer() {
        TemplateRenderer templateRenderer = new TemplateRendererImpl(getBaseDir())
        templateRenderer
    }

    protected FileSystemInteraction getFileSystemInteraction() {
        FileSystemInteraction fileSystemInteraction = new FileSystemInteractionImpl(getBaseDir(), new DefaultResourceLoader())
        fileSystemInteraction
    }

    protected void createFile(String source, String destination, Map<String, Object> model, boolean overwrite) {
        templateRenderer.render(templateRenderer.template(getTemplateRoot(), source),
                fileSystemInteraction.file(destination), model, overwrite)
    }

    void template(String source, String destination) {
        template(source, destination, new HashMap(), true)
    }

    void template(String source, String destination, Map<String, Object> model) {
        templateRenderer.render(templateRenderer.template(getTemplateRoot(), source), file(destination), model, true)
    }

    void template(String source, String destination, Map<String, Object> model, boolean overwrite) {
        templateRenderer.render(templateRenderer.template(getTemplateRoot(), source), file(destination), model, overwrite)
    }

    void copyFile(String source, String destination) {
        this.console.addStatus('create '.padLeft(13), destination, 'GREEN')
        SpringIOUtils.copy(templateRenderer.template(getTemplateRoot(), source), file(destination))
    }

    File file(Object path) {
        if (path instanceof File) {
            return (File) path
        }
        else if (path instanceof Resource) {
            return ((Resource) path).file
        }
        new File(getBaseDir(), path.toString())
    }

    /**
     * Creates a file with the specified name and the text contents using the system default encoding.
     * @param name name of the file to be created
     * @param contents the contents of the file, written using the system default encoding
     * @return the file being created
     */
    File file(String name, CharSequence contents) {
        new File(getBaseDir(), name) << contents
    }

    /**
     * Creates a file with the specified name and the specified binary contents
     * @param name name of the file to be created
     * @param contents the contents of the file
     * @return the file being created
     */
    File file(String name, byte[] contents) {
        new File(getBaseDir(), name) << contents
    }

    /**
     * Creates a file with the specified name and the contents from the source file (copy).
     * @param name name of the file to be created
     * @param contents the contents of the file
     * @return the file being created
     */
    File file(String name, File source) {
        file(name, source.bytes)
    }

    /**
     * Creates a new file in the current directory, whose contents is going to be generated in the
     * closure. The delegate of the closure is the file being created.
     * @param name name of the file to create
     * @param spec closure for generating the file contents
     * @return the created file
     */
    File file(String name, @ClosureParams(value = FromString, options = 'File') @DelegatesTo(value = File, strategy = Closure.DELEGATE_FIRST) Closure spec) {
        def file = new File(getBaseDir(), name)
        file.tap(spec)
    }

    /**
     * Creates a new empty directory
     * @param name the name of the directory to create
     * @return the created directory
     */
    File dir(String name) {
        console.addStatus('create '.padLeft(13), name, 'GREEN')
        def file = new File(getBaseDir(), name)
        file.mkdirs()
        file
    }

    /**
     * Creates a new directory and allows to specify a subdirectory structure using the closure as a specification
     * @param name name of the directory to be created
     * @param spec specification of the subdirectory structure
     * @return the created directory
     */
    File dir(String name, @DelegatesTo(value = FileTreeBuilder, strategy = Closure.DELEGATE_FIRST) Closure spec) {
        def oldBase = this.baseDir
        def newBase = dir(name)
        try {
            this.baseDir = newBase
            def clone = (Closure) spec.clone()
            clone.delegate = this
            clone.resolveStrategy = Closure.DELEGATE_FIRST
            clone()
        }
        finally {
            this.baseDir = oldBase
        }
        newBase
    }

    File call(@DelegatesTo(value = FileTreeBuilder, strategy = Closure.DELEGATE_FIRST) Closure spec) {
        def clone = (Closure) spec.clone()
        clone.delegate = this
        clone.resolveStrategy = Closure.DELEGATE_FIRST
        clone.call()
        this.baseDir
    }

    def methodMissing(String name, Object args) {
        if (args instanceof Object[] && args.length == 1) {
            def arg = args[0]
            if (arg instanceof Closure) {
                dir(name, (Closure<?>) arg)
            }
            else if (arg instanceof CharSequence) {
                file(name, arg.toString())
            }
            else if (arg instanceof byte[]) {
                file(name, (byte[]) arg)
            }
            else if (arg instanceof File) {
                file(name, (File) arg)
            }
        }
    }

}
