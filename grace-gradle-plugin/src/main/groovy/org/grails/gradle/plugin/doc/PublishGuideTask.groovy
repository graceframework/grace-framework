/*
 * Copyright 2014-2025 the original author or authors.
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
package org.grails.gradle.plugin.doc

import javax.inject.Inject

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

import grails.doc.DocPublisher
import grails.doc.macros.HiddenMacro

/**
 * A task used to publish the user guide if a publish that is in GDoc or Asciidoc format
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
@DisableCachingByDefault(because = "DocPublisher use Ant tasks")
abstract class PublishGuideTask extends DefaultTask {

    private org.gradle.api.AntBuilder ant
    private Logger logger
    private final FileSystemOperations fileSystemOperations

    @Internal
    abstract Property<String> getProjectName()

    @Internal
    abstract Property<String> getProjectVersion()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getSourceDir()

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResourcesDir()

    @Optional
    @OutputDirectory
    abstract DirectoryProperty getTargetDir()

    @Optional
    @OutputDirectory
    abstract DirectoryProperty getWorkDir()

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getPropertiesFile()

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getGroovydocDir()

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getJavadocDir()

    @Input
    @Optional
    abstract Property<Boolean> getAsciidoc()

    @Input
    @Optional
    abstract Property<Boolean> getBookmarks()

    @Input
    @Optional
    abstract Property<String> getLanguage()

    @Input
    @Optional
    abstract Property<String> getSourceRepo()

    @Input
    @Optional
    abstract ListProperty<Object> getMacros()

    @Input
    @Optional
    abstract Property<String> getSingleHtml()

    @Inject
    PublishGuideTask(Project project, FileSystemOperations fileSystemOperations) {
        this.ant = project.ant
        this.logger = project.logger
        this.fileSystemOperations = fileSystemOperations
        getProjectName().convention(project.provider(project::getName))
        getProjectVersion().convention(project.provider(() -> project.getVersion().toString()))
        getTargetDir().convention(project.layout.buildDirectory.dir('docs/manual'))
        getWorkDir().convention(project.layout.buildDirectory.dir('tmp'))

        if (!getResourcesDir().isPresent()) {
            getResourcesDir().set(getSourceDir().dir('resources'))
        }
    }

    @TaskAction
    protected void publishGuide() {
        DocPublisher docPublisher = new DocPublisher(getSourceDir().get().asFile, getTargetDir().get().asFile, this.logger)

        docPublisher.ant = this.ant
        docPublisher.asciidoc = getAsciidoc().getOrElse(true)
        docPublisher.bookmarks = getBookmarks().getOrElse(false)
        docPublisher.language = getLanguage().getOrElse('')
        docPublisher.title = getProjectName().getOrNull()
        docPublisher.version = getProjectVersion().getOrNull()
        docPublisher.workDir = getWorkDir().get().asFile
        docPublisher.apiDir = getTargetDir().get().asFile
        docPublisher.sourceRepo = getSourceRepo().getOrElse(null)
        docPublisher.fonts = getResourcesDir().file('fonts').get().asFile
        docPublisher.images = getResourcesDir().file('img').get().asFile
        docPublisher.css = getResourcesDir().file('css').get().asFile
        docPublisher.js = getResourcesDir().file('js').get().asFile
        docPublisher.style = getResourcesDir().file('style').get().asFile
        docPublisher.propertiesFile = getPropertiesFile().getOrElse(getResourcesDir().file('doc.properties').get()).asFile
        docPublisher.singleHtml = getSingleHtml().getOrElse('single.html')

        // Add custom macros.
        // {hidden} macro for enabling translations.
        docPublisher.registerMacro(new HiddenMacro())

        for (Object m in getMacros().get()) {
            docPublisher.registerMacro(m)
        }

        docPublisher.publish()

        if (getGroovydocDir().getOrNull()?.asFile?.exists()) {
            this.fileSystemOperations.copy { CopySpec copy ->
                copy.from(getGroovydocDir())
                copy.into(getTargetDir().file('gapi'))
            }
        }
        if (getJavadocDir().getOrNull()?.asFile?.exists()) {
            this.fileSystemOperations.copy { CopySpec copy ->
                copy.from(getJavadocDir())
                copy.into(getTargetDir().file('api'))
            }
        }
    }

}
