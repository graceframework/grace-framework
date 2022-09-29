package org.grails.gradle.plugin.web.gsp

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.grails.gradle.plugin.util.SourceSets
import org.grails.gradle.plugin.core.GrailsExtension
import org.apache.tools.ant.taskdefs.condition.Os

/**
 * A plugin that adds support for compiling Groovy Server Pages (GSP)
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GroovyPagePlugin implements Plugin<Project> {

    @CompileDynamic
    @Override
    void apply(Project project) {

        project.configurations {
            gspCompile
        }

        project.dependencies {
            gspCompile 'javax.servlet:javax.servlet-api:3.1.0'
        }

        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)

        SourceSetOutput output = mainSourceSet?.output
        FileCollection classesDirs = resolveClassesDirs(output, project)
        File destDir = output?.dir("gsp-classes") ?: new File(project.buildDir, "gsp-classes/main")

        Configuration providedConfig = project.configurations.findByName('providedCompile')
        def allClasspath = project.configurations.compileClasspath + project.configurations.gspCompile + classesDirs
        if(providedConfig) {
            allClasspath += providedConfig
        }

        String grailsAppDir = SourceSets.resolveGrailsAppDir(project)

        def allTasks = project.tasks

        def compileGroovyPages = allTasks.create("compileGroovyPages", GroovyPageForkCompileTask) {
            destinationDir = destDir
            tmpDirPath = getTmpDirPath(project)
            source = project.file("${project.projectDir}/${grailsAppDir}/views")
            serverpath = "/WEB-INF/${grailsAppDir}/views/"
        }

        compileGroovyPages.setClasspath( allClasspath )

        def compileWebappGroovyPages = allTasks.create("compileWebappGroovyPages", GroovyPageForkCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/src/main/webapp")
            tmpDirPath = getTmpDirPath(project)
            serverpath = "/"
        }

        compileWebappGroovyPages.setClasspath( allClasspath )


        project.afterEvaluate {
            GrailsExtension grailsExt = project.extensions.getByType(GrailsExtension)
            if (grailsExt.pathingJar && Os.isFamily(Os.FAMILY_WINDOWS)) {
                Jar pathingJar = (Jar) allTasks.findByName('pathingJar')
                allClasspath = project.files("${project.buildDir}/classes/groovy/main", "${project.buildDir}/resources/main", pathingJar.archivePath)
                compileGroovyPages.dependsOn(pathingJar)
                compileGroovyPages.setClasspath(allClasspath)
                compileWebappGroovyPages.dependsOn(pathingJar)
                compileWebappGroovyPages.setClasspath(allClasspath)
            }
        }


        compileGroovyPages.dependsOn( allTasks.findByName("classes") )
        compileGroovyPages.dependsOn( compileWebappGroovyPages )

        allTasks.withType(War) { War war ->
            war.dependsOn compileGroovyPages
            if (war.classpath) {
                war.classpath = war.classpath + project.files(destDir)
            } else {
                war.classpath = project.files(destDir)
            }
        }
        allTasks.withType(Jar) { Jar jar ->
            if(!(jar instanceof War)) {
                if (jar.name == 'bootJar') {
                    jar.dependsOn compileGroovyPages
                    jar.from(destDir) {
                        into("BOOT-INF/classes")
                    }
                } else if(jar.name == 'jar') {
                    jar.dependsOn compileGroovyPages
                    jar.from destDir
                }
            }
        }
    }

    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(new File(project.buildDir, "classes/main"))
    }

    protected String getTmpDirPath(Project project) {
        def tmpdir = new File(project.buildDir as String, "gsptmp")
        return tmpdir.absolutePath
    }

}
