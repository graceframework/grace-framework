/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.cli.command.about

import groovy.transform.CompileStatic
import org.gradle.util.GradleVersion
import org.springframework.boot.SpringBootVersion

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsVersion

import org.grails.config.CodeGenConfig

/**
 * ApplicationInfo gives information about version numbers for Grace, Groovy, Gradle, Spring Boot,
 * the application's name, version, folder, and the current environment name.
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
@CompileStatic
class ApplicationInfo {

    static String getInfo() {
        StringBuilder appInfo = new StringBuilder()
        appInfo.append('%nName:               ').append(appName)
        appInfo.append('%nVersion:            ').append(appVersion)

        appInfo.append('%nApplication root:   ').append(appRoot)
        appInfo.append('%nEnvironment:        ').append(appEnvironment)

        appInfo.append('%n%nGrails:             ').append(grailsVersion)
        appInfo.append('%nGroovy:             ').append(groovyVersion)
        appInfo.append('%nGradle:             ').append(gradleVersion)
        appInfo.append('%nSpring Boot:        ').append(springBootVersion)

        appInfo.append('%nJVM:                ').append(javaVersion)
        appInfo.append('%nOS:                 ').append(osVersion)
        appInfo.append('%n%n')
        String.format(appInfo.toString())
    }

    static String getAppName() {
        String appName = getApplicationConfig().getProperty('info.app.name')
        if (!appName) {
            File settingsFile = new File(BuildSettings.BASE_DIR, 'settings.gradle')
            if (settingsFile.exists()) {
                settingsFile.eachLine { line ->
                    if (line.startsWith('rootProject.name')) {
                        appName = line[20..-2]
                        return appName
                    }
                }
            }
        }
        appName
    }

    static String getAppVersion() {
        String appVersion = getApplicationConfig().getProperty('info.app.version')
        if (!appVersion) {
            File gradlePropertiesFile = new File(BuildSettings.BASE_DIR, 'gradle.properties')
            Properties fileProps = new Properties()
            gradlePropertiesFile.withInputStream { InputStream input ->
                fileProps.load(input)
                String version = fileProps.get('version')
                appVersion = version
            }
        }
        appVersion
    }

    static String getAppRoot() {
        BuildSettings.BASE_DIR.canonicalPath
    }

    static String getAppEnvironment() {
        Environment.current.name
    }

    static String getGrailsVersion() {
        GrailsVersion.current().version
    }

    static String getGroovyVersion() {
        GroovySystem.version
    }

    static String getGradleVersion() {
        String gradleVersion = null
        File gradleWrapperFile = new File(BuildSettings.BASE_DIR, 'gradle/wrapper/gradle-wrapper.properties')
        if (gradleWrapperFile.exists()) {
            Properties fileProps = new Properties()
            gradleWrapperFile.withInputStream { InputStream input ->
                fileProps.load(input)
                String distributionUrl = fileProps.get('distributionUrl')
                gradleVersion = distributionUrl[49..-9]
            }
        }
        gradleVersion ?: GradleVersion.current().version
    }

    static String getSpringBootVersion() {
        SpringBootVersion.version
    }

    static String getJavaVersion() {
        String.format('%s (%s %s)', System.getProperty('java.version'),
                System.getProperty('java.vm.vendor'), System.getProperty('java.vm.version'))
    }

    static String getOsVersion() {
        String.format('%s %s %s', System.getProperty('os.name'), System.getProperty('os.version'), System.getProperty('os.arch'))
    }

    static CodeGenConfig getApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File(BuildSettings.RESOURCES_DIR, "application.yml")
        if (applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        config
    }

}
