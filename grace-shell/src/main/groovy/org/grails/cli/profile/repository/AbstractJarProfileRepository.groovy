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

import org.grails.cli.GrailsCli
import org.grails.cli.profile.AbstractProfile
import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProjectContext
import org.grails.cli.profile.ProjectContextAware
import org.grails.io.support.ClassPathResource
import org.grails.io.support.Resource

/**
 * A repository that loads profiles from JAR files
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.1
 */
@CompileStatic
abstract class AbstractJarProfileRepository implements ProfileRepository {

    protected final List<Profile> allProfiles = []
    protected final Map<String, Profile> profilesByName = [:]
    protected static final String DEFAULT_PROFILE_GROUPID = 'org.graceframework.profiles'
    protected static final String GRAILS_PROFILE_GROUPID = 'org.grails.profiles'

    private final Set<URL> registeredUrls = []

    @Override
    Profile getProfile(String profileName) {
        profilesByName[profileName]
    }

    @Override
    Profile getProfile(String profileName, Boolean parentProfile) {
        getProfile(profileName)
    }

    List<Profile> getAllProfiles() {
        allProfiles
    }

    @Override
    Resource getProfileDirectory(String profile) {
        getProfile(profile)?.profileDir
    }

    @Override
    List<Profile> getProfileAndDependencies(Profile profile) {
        List<Profile> sortedProfiles = []
        Set<Profile> visitedProfiles = [] as Set
        visitTopologicalSort(profile, sortedProfiles, visitedProfiles)
        sortedProfiles
    }

    String getDefaultGroupId() {
        DEFAULT_PROFILE_GROUPID
    }

    Artifact getProfileArtifact(String profileName) {
        if (profileName.contains(':')) {
            return new DefaultArtifact(profileName)
        }

        String groupId = getDefaultGroupId()
        String version = null

        Map<String, Map> defaultValues = GrailsCli.getSetting('grails.profiles', Map, [:])
        defaultValues.remove('repositories')
        def data = defaultValues.get(profileName)
        if (data instanceof Map) {
            groupId = data.get('groupId')
            version = data.get('version')
        }

        new DefaultArtifact(groupId, profileName, null, version)
    }

    protected void registerProfile(URL url, ClassLoader parent) {
        if (registeredUrls.contains(url)) {
            return
        }

        URLClassLoader classLoader = new URLClassLoader([url] as URL[])
        URL profileYml = classLoader.getResource('META-INF/grails-profile/profile.yml')
        if (profileYml != null) {
            registeredUrls.add(url)
            JarProfile profile = new JarProfile(this, new ClassPathResource('META-INF/grails-profile/', classLoader), parent)
            profile.profileRepository = this
            allProfiles.add profile
            profilesByName[profile.name] = profile
        }
    }

    private void visitTopologicalSort(Profile profile, List<Profile> sortedProfiles, Set<Profile> visitedProfiles) {
        if (profile != null && !visitedProfiles.contains(profile)) {
            visitedProfiles.add(profile)
            profile.getExtends().each { Profile dependentProfile ->
                visitTopologicalSort(dependentProfile, sortedProfiles, visitedProfiles)
            }
            sortedProfiles.add(profile)
        }
    }

    static class JarProfile extends AbstractProfile {

        JarProfile(ProfileRepository repository, Resource profileDir, ClassLoader classLoader) {
            super(profileDir, classLoader)
            this.profileRepository = repository
            initialize()
        }

        @Override
        String getName() {
            super.name
        }

        @Override
        Iterable<Command> getCommands(ProjectContext context) {
            super.getCommands(context)
            for (cmd in internalCommands) {
                if (cmd instanceof ProjectContextAware) {
                    ((ProjectContextAware) cmd).setProjectContext(context)
                }
                commandsByName[cmd.name] = cmd
            }

            commandsByName.values()
        }

    }

}
