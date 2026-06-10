/*
 * Copyright 2015-2026 the original author or authors.
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

import groovy.grape.GrapeEngine
import groovy.transform.CompileStatic
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.grails.cli.compiler.grape.DefaultRepositorySystemSessionAutoConfiguration
import org.grails.cli.compiler.grape.DependencyResolutionContext
import org.grails.cli.compiler.grape.MavenResolverGrapeEngine
import org.grails.cli.compiler.grape.RepositorySystemSessionAutoConfiguration

/**
 *  Creates {@link GrapeEngine} to resolve profiles.
 *
 * @author James Kleeh
 * @author Michael Yan
 * @since 3.2
 */
@CompileStatic
class GrailsMavenGrapeEngineFactory {

    static MavenResolverGrapeEngine create(GroovyClassLoader classLoader,
                                           List<GrailsRepositoryConfiguration> repositoryConfigurations,
                                           DependencyResolutionContext dependencyResolutionContext) {

        RepositorySystem repositorySystem = new RepositorySystemSupplier().get()

        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils.newSession()

        ServiceLoader<RepositorySystemSessionAutoConfiguration> autoConfigurations = ServiceLoader
                .load(RepositorySystemSessionAutoConfiguration)

        for (RepositorySystemSessionAutoConfiguration autoConfiguration : autoConfigurations) {
            autoConfiguration.apply(repositorySystemSession, repositorySystem)
        }

        new DefaultRepositorySystemSessionAutoConfiguration().apply(repositorySystemSession, repositorySystem)

        new MavenResolverGrapeEngine(classLoader, repositorySystem,
                repositorySystemSession, createRepositories(repositoryConfigurations),
                dependencyResolutionContext, false)
    }

    private static List<RemoteRepository> createRepositories(List<GrailsRepositoryConfiguration> repositoryConfigurations) {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>(repositoryConfigurations.size())

        for (GrailsRepositoryConfiguration repositoryConfiguration : repositoryConfigurations) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(
                    repositoryConfiguration.getName(), 'default',
                    repositoryConfiguration.getUri().toASCIIString())
            if (repositoryConfiguration.hasCredentials()) {
                builder.authentication = new AuthenticationBuilder()
                        .addUsername(repositoryConfiguration.username)
                        .addPassword(repositoryConfiguration.password)
                        .build()
            }
            if (!repositoryConfiguration.getSnapshotsEnabled()) {
                builder.setSnapshotPolicy(
                        new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_NEVER,
                                RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
            }
            repositories.add(builder.build())
        }
        repositories
    }

}
