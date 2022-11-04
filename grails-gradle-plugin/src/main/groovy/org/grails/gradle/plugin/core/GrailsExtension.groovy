package org.grails.gradle.plugin.core

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * A extension to the Gradle plugin to configure Grails settings
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsExtension {
    Project project

    GrailsExtension(Project project) {
        this.project = project
    }
    /**
     * Whether to invoke native2ascii on resource bundles
     */
    boolean native2ascii = !Os.isFamily(Os.FAMILY_WINDOWS)

    /**
     * Whether to use Ant to do the conversion
     */
    boolean native2asciiAnt = false

    /**
     * Whether assets should be packaged in META-INF/assets for plugins
     */
    boolean packageAssets = true

    /**
     * Whether to include subproject dependencies as directories directly on the classpath, instead of as JAR files
     */
    boolean exploded = true

    /**
     * Whether to create a jar file to reference the classpath to prevent classpath too long issues in Windows
     */
    boolean pathingJar = false

    /**
     * Allows defining plugins in the available scopes
     */
    void plugins(Closure pluginDefinitions) {
        def definer = new PluginDefiner(project,exploded)
        ConfigureUtil.configureSelf(pluginDefinitions, definer)
    }

}
