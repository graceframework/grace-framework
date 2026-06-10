/*
 * Copyright 2012-2026 the original author or authors.
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
package org.grails.cli.compiler.grape;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import groovy.lang.GroovyClassLoader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

/**
 * Utility class to create a pre-configured {@link MavenResolverGrapeEngine}.
 *
 * @author Andy Wilkinson
 * @author Michael Yan
 * @since 2022.1.0
 */
public abstract class MavenResolverGrapeEngineFactory {

    public static MavenResolverGrapeEngine create(GroovyClassLoader classLoader,
            List<RepositoryConfiguration> repositoryConfigurations,
            DependencyResolutionContext dependencyResolutionContext, boolean quiet) {
        RepositorySystem repositorySystem = new RepositorySystemSupplier().get();
        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils.newSession();
        repositorySystemSession.setSystemProperties(System.getProperties());
        ServiceLoader<RepositorySystemSessionAutoConfiguration> autoConfigurations = ServiceLoader
                .load(RepositorySystemSessionAutoConfiguration.class);
        for (RepositorySystemSessionAutoConfiguration autoConfiguration : autoConfigurations) {
            autoConfiguration.apply(repositorySystemSession, repositorySystem);
        }
        new DefaultRepositorySystemSessionAutoConfiguration().apply(repositorySystemSession, repositorySystem);
        return new MavenResolverGrapeEngine(classLoader, repositorySystem, repositorySystemSession,
                createRepositories(repositoryConfigurations), dependencyResolutionContext, quiet);
    }

    private static List<RemoteRepository> createRepositories(List<RepositoryConfiguration> repositoryConfigurations) {
        List<RemoteRepository> repositories = new ArrayList<>(repositoryConfigurations.size());
        for (RepositoryConfiguration repositoryConfiguration : repositoryConfigurations) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(repositoryConfiguration.getName(),
                    "default", repositoryConfiguration.getUri().toASCIIString());
            if (!repositoryConfiguration.getSnapshotsEnabled()) {
                builder.setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_NEVER,
                        RepositoryPolicy.CHECKSUM_POLICY_IGNORE));
            }
            repositories.add(builder.build());
        }
        return repositories;
    }

}
