/*
 * Copyright 2014-2022 the original author or authors.
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
package org.grails.cli.boot

import groovy.grape.Grape
import groovy.grape.GrapeEngine
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.springframework.boot.cli.compiler.dependencies.Dependency
import org.springframework.boot.cli.compiler.dependencies.DependencyManagement

/**
 * Introduces dependency management based on a published BOM file
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsDependencyVersions implements DependencyManagement {

    protected Map<String, Dependency> groupAndArtifactToDependency = [:]
    protected Map<String, String> artifactToGroupAndArtifact = [:]
    protected List<Dependency> dependencies = []
    protected Map<String, String> versionProperties = [:]

    GrailsDependencyVersions() {
        this(getDefaultEngine())
    }

    GrailsDependencyVersions(Map<String, String> bomCoords) {
        this(getDefaultEngine(), bomCoords)
    }

    GrailsDependencyVersions(GrapeEngine grape) {
        this(grape, [group: 'org.graceframework', module: 'grace-bom', version: GrailsDependencyVersions.package.implementationVersion, type: 'pom'])
    }

    GrailsDependencyVersions(GrapeEngine grape, Map<String, String> bomCoords) {
        def results = grape.resolve(null, bomCoords)

        for (URI u in results) {
            def pom = new XmlSlurper().parseText(u.toURL().text)
            addDependencyManagement(pom)
        }
    }

    static GrapeEngine getDefaultEngine() {
        def grape = Grape.getInstance()
        grape.addResolver([name: 'mavenCentral', root: 'https://repo.maven.apache.org/maven2/'] as Map<String, Object>)
        grape
    }

    @CompileDynamic
    void addDependencyManagement(GPathResult pom) {
        versionProperties = pom.properties.'*'.collectEntries { [(it.name()): it.text()] }
        pom.dependencyManagement.dependencies.dependency.each { dep ->
            String version = dep.version.text()
            if (version.startsWith('$')) {
                String key = version.substring(2, version.length() - 1)
                version = versionProperties[key]
            }
            addDependency(dep.groupId.text(), dep.artifactId.text(), version)
        }
    }

    protected void addDependency(String group, String artifactId, String version) {
        def groupAndArtifactId = "$group:$artifactId".toString()
        artifactToGroupAndArtifact[artifactId] = groupAndArtifactId

        def dep = new Dependency(group, artifactId, version)
        dependencies.add(dep)
        groupAndArtifactToDependency[groupAndArtifactId] = dep
    }

    Dependency find(String groupId, String artifactId) {
        groupAndArtifactToDependency["$groupId:$artifactId".toString()]
    }

    @Override
    List<Dependency> getDependencies() {
        dependencies
    }

    Map<String, String> getVersionProperties() {
        versionProperties
    }

    @Override
    String getSpringBootVersion() {
        find('spring-boot').getVersion()
    }

    @Override
    Dependency find(String artifactId) {
        def groupAndArtifact = artifactToGroupAndArtifact[artifactId]
        groupAndArtifact ? groupAndArtifactToDependency[groupAndArtifact] : null
    }

    Iterator<Dependency> iterator() {
        groupAndArtifactToDependency.values().iterator()
    }

}
