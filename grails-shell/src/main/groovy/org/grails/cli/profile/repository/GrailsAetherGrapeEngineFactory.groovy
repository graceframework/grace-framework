package org.grails.cli.profile.repository

import groovy.transform.CompileStatic
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.internal.impl.DefaultRepositorySystem
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.spi.locator.ServiceLocator
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngine
import org.springframework.boot.cli.compiler.grape.DefaultRepositorySystemSessionAutoConfiguration
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext
import org.springframework.boot.cli.compiler.grape.RepositorySystemSessionAutoConfiguration

/**
 *  Creates aether engine to resolve profiles. Mostly copied from {@link AetherGrapeEngine}.
 *  Created to support repositories with authentication.
 *
 * @author James Kleeh
 * @since 3.2
 */
@CompileStatic
class GrailsAetherGrapeEngineFactory {

    static AetherGrapeEngine create(GroovyClassLoader classLoader,
                                    List<GrailsRepositoryConfiguration> repositoryConfigurations,
                                    DependencyResolutionContext dependencyResolutionContext) {

        RepositorySystem repositorySystem = createServiceLocator().getService(RepositorySystem)

        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils.newSession()

        ServiceLoader<RepositorySystemSessionAutoConfiguration> autoConfigurations = ServiceLoader
                .load(RepositorySystemSessionAutoConfiguration)

        for (RepositorySystemSessionAutoConfiguration autoConfiguration : autoConfigurations) {
            autoConfiguration.apply(repositorySystemSession, repositorySystem)
        }

        new DefaultRepositorySystemSessionAutoConfiguration().apply(repositorySystemSession, repositorySystem)

        return new AetherGrapeEngine(classLoader, repositorySystem,
                repositorySystemSession, createRepositories(repositoryConfigurations),
                dependencyResolutionContext, false)
    }

    private static ServiceLocator createServiceLocator() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositorySystem, DefaultRepositorySystem)
        locator.addService(RepositoryConnectorFactory,
                BasicRepositoryConnectorFactory)
        locator.addService(TransporterFactory, HttpTransporterFactory)
        locator.addService(TransporterFactory, FileTransporterFactory)
        return locator
    }

    private static List<RemoteRepository> createRepositories(List<GrailsRepositoryConfiguration> repositoryConfigurations) {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>(repositoryConfigurations.size())

        for (GrailsRepositoryConfiguration repositoryConfiguration : repositoryConfigurations) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(
                    repositoryConfiguration.getName(), "default",
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
        return repositories
    }

}
