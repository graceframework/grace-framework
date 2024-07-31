/*
 * Copyright 2015-2024 the original author or authors.
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
package org.grails.cli.profile.repository

import groovy.transform.CompileStatic
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency

import org.grails.cli.compiler.dependencies.GrailsDependenciesDependencyManagement
import org.grails.cli.compiler.grape.DependencyResolutionContext
import org.grails.cli.compiler.grape.DependencyResolutionFailedException
import org.grails.cli.compiler.grape.MavenResolverGrapeEngine
import org.grails.cli.profile.Profile

/**
 *  Resolves profiles from a configured list of repositories using Aether
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
class MavenProfileRepository extends AbstractJarProfileRepository {

    public static final GrailsRepositoryConfiguration DEFAULT_REPO = new GrailsRepositoryConfiguration(
            'mavenCentral', new URI('https://repo1.maven.org/maven2/'), false)

    public static final GrailsRepositoryConfiguration MAVEN_LOCAL_REPO = getMavenLocalRepoConfiguration()

    List<GrailsRepositoryConfiguration> repositoryConfigurations
    MavenResolverGrapeEngine grapeEngine
    GroovyClassLoader classLoader
    DependencyResolutionContext resolutionContext
    GrailsDependenciesDependencyManagement profileDependencyVersions
    private boolean resolved = false

    MavenProfileRepository() {
        this([MAVEN_LOCAL_REPO, DEFAULT_REPO])
    }

    MavenProfileRepository(String grailsVersion) {
        this([MAVEN_LOCAL_REPO, DEFAULT_REPO], grailsVersion)
    }

    MavenProfileRepository(List<GrailsRepositoryConfiguration> repositoryConfigurations) {
        this(repositoryConfigurations, null)
    }

    MavenProfileRepository(List<GrailsRepositoryConfiguration> repositoryConfigurations, String grailsVersion) {
        this.repositoryConfigurations = repositoryConfigurations
        this.classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)
        this.resolutionContext = new DependencyResolutionContext()
        this.grapeEngine = GrailsMavenGrapeEngineFactory.create(classLoader, repositoryConfigurations, resolutionContext)
        this.profileDependencyVersions = grailsVersion != null ?
                new GrailsDependenciesDependencyManagement(grailsVersion) :
                new GrailsDependenciesDependencyManagement()
        this.resolutionContext.addDependencyManagement(profileDependencyVersions)
    }

    @Override
    Profile getProfile(String profileName, Boolean parentProfile) {
        String profileShortName = profileName
        if (profileName.contains(':')) {
            def art = new DefaultArtifact(profileName)
            profileShortName = art.artifactId
        }
        if (!profilesByName.containsKey(profileShortName)) {
            if (parentProfile && this.profileDependencyVersions.find(profileShortName)) {
                return resolveProfile(profileShortName)
            }
            return resolveProfile(profileName)
        }
        super.getProfile(profileShortName)
    }

    @Override
    Profile getProfile(String profileName) {
        getProfile(profileName, false)
    }

    protected Profile resolveProfile(String profileName) {
        Artifact art = getProfileArtifact(profileName)

        try {
            this.grapeEngine.grab(group: art.groupId, module: art.artifactId, version: art.version ?: null)
        }
        catch (DependencyResolutionFailedException ignore) {
        }

        processUrls()
        super.getProfile(art.artifactId)
    }

    protected void processUrls() {
        def urls = this.classLoader.getURLs()
        for (URL url in urls) {
            registerProfile(url, new URLClassLoader([url] as URL[], Thread.currentThread().contextClassLoader))
        }
    }

    @Override
    List<Profile> getAllProfiles() {
        if (!this.resolved) {
            List<Map> profiles = []
            this.resolutionContext.managedDependencies.each { Dependency dep ->
                if (dep.artifact.groupId == 'org.graceframework.profiles') {
                    profiles.add([group: dep.artifact.groupId, module: dep.artifact.artifactId])
                }
            }
            profiles.sort { it.module }

            for (Map profile in profiles) {
                this.grapeEngine.grab(profile)
            }

            processUrls()
            this.resolved = true
        }
        super.getAllProfiles()
    }

    private static GrailsRepositoryConfiguration getMavenLocalRepoConfiguration() {
        String mavenRoot = System.getProperty('maven.home')
        File defaultM2HomeDirectory = mavenRoot ? new File(mavenRoot)
                : new File(System.getProperty('user.home'), '.m2')

        File m2RepoDirectory = new File(defaultM2HomeDirectory, 'repository')
        new GrailsRepositoryConfiguration('mavenLocal', m2RepoDirectory.toURI(), true)
    }

}
