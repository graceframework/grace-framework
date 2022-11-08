package org.grails.gradle;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishException;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.artifacts.ArtifactPublicationServices;
import org.gradle.api.internal.artifacts.ArtifactPublisher;
import org.gradle.api.internal.artifacts.Module;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.internal.artifacts.repositories.PublicationAwareRepository;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.Transformers;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.util.internal.ConfigureUtil;

import static org.gradle.util.internal.CollectionUtils.collect;

public class Upload extends ConventionTask {

    private Configuration configuration;

    private boolean uploadDescriptor;

    private File descriptorDestination;

    private RepositoryHandler repositories;

    @Inject
    protected ArtifactPublicationServices getPublicationServices() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    protected void upload() {
        getLogger().info("Publishing configuration: {}", configuration);

        Module module = ((ConfigurationInternal) configuration).getModule();

        ArtifactPublisher artifactPublisher = getPublicationServices().createArtifactPublisher();
        File descriptorDestination = isUploadDescriptor() ? getDescriptorDestination() : null;
        List<PublicationAwareRepository> publishRepositories = collect(getRepositories(), Transformers.cast(PublicationAwareRepository.class));

        try {
            artifactPublisher.publish(publishRepositories, module, configuration, descriptorDestination);
        }
        catch (Exception e) {
            throw new PublishException(String.format("Could not publish configuration '%s'", configuration.getName()), e);
        }
    }

    /**
     * Specifies whether the dependency descriptor should be uploaded.
     */
    @Input
    public boolean isUploadDescriptor() {
        return uploadDescriptor;
    }

    public void setUploadDescriptor(boolean uploadDescriptor) {
        this.uploadDescriptor = uploadDescriptor;
    }

    /**
     * Returns the path to generate the dependency descriptor to.
     */
    @Internal
    public File getDescriptorDestination() {
        return descriptorDestination;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDescriptorDestination(File descriptorDestination) {
        this.descriptorDestination = descriptorDestination;
    }

    /**
     * Returns the repositories to upload to.
     */
    @Internal
    public RepositoryHandler getRepositories() {
        if (repositories == null) {
            repositories = getPublicationServices().createRepositoryHandler();
        }
        return repositories;
    }

    /**
     * Returns the configuration to upload.
     */
    @Internal
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Configures the set of repositories to upload to.
     */
    public RepositoryHandler repositories(@Nullable Closure configureClosure) {
        return ConfigureUtil.configure(configureClosure, getRepositories());
    }

    /**
     * Configures the set of repositories to upload to.
     * @since 3.5
     */
    public RepositoryHandler repositories(Action<? super RepositoryHandler> configureAction) {
        RepositoryHandler repositories = getRepositories();
        configureAction.execute(repositories);
        return repositories;
    }

    /**
     * Returns the artifacts which will be uploaded.
     *
     * @return the artifacts.
     */
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFiles
    public FileCollection getArtifacts() {
        Configuration configuration = getConfiguration();
        return configuration.getAllArtifacts().getFiles();
    }

}
