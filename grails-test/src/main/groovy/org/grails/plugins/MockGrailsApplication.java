package org.grails.plugins;

import grails.config.Config;
import grails.core.*;
import grails.util.Metadata;
import groovy.lang.GroovyClassLoader;
import org.grails.config.PropertySourcesConfig;
import org.grails.datastore.mapping.model.MappingContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.Map;

class MockGrailsApplication implements GrailsApplication {
    private ClassLoader classLoader;

    MockGrailsApplication() {
    }

    public MockGrailsApplication(Class[] classes, GroovyClassLoader groovyClassLoader) {
        this.classLoader = groovyClassLoader;
    }

    public GrailsClass getArtefact(String artefactType, String name) {
        return null;
    }

    public void setApplicationContext(ApplicationContext ctx) {
        throw new UnsupportedOperationException();
    }

    public Config getConfig() {
        return new PropertySourcesConfig();
    }

    public Map getFlatConfig() {
        throw new UnsupportedOperationException();
    }

    public ClassLoader getClassLoader() {
        return this.classLoader != null ? this.classLoader : new GroovyClassLoader();
    }

    public Class[] getAllClasses() {
        throw new UnsupportedOperationException();
    }

    public Class[] getAllArtefacts() {
        throw new UnsupportedOperationException();
    }

    public ApplicationContext getMainContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MappingContext getMappingContext() {
        return null;
    }

    public void setMainContext(ApplicationContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMappingContext(MappingContext mappingContext) {

    }

    public ApplicationContext getParentContext() {
        throw new UnsupportedOperationException();
    }

    public Class getClassForName(String className) {
        throw new UnsupportedOperationException();
    }

    public void refreshConstraints() {
        throw new UnsupportedOperationException();
    }

    public void refresh() {
        throw new UnsupportedOperationException();
    }

    public void rebuild() {
        throw new UnsupportedOperationException();
    }

    public Resource getResourceForClass(Class theClazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isArtefact(Class theClazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isArtefactOfType(String artefactType, Class theClazz) {
        throw new UnsupportedOperationException();
    }

    public boolean isArtefactOfType(String artefactType, String className) {
        throw new UnsupportedOperationException();
    }

    public ArtefactHandler getArtefactType(Class theClass) {
        throw new UnsupportedOperationException();
    }

    public ArtefactInfo getArtefactInfo(String artefactType) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass[] getArtefacts(String artefactType) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass addArtefact(String artefactType, Class artefactClass) {
        throw new UnsupportedOperationException();
    }

    public GrailsClass addArtefact(String artefactType, GrailsClass gc) {
        throw new UnsupportedOperationException();
    }

    public void registerArtefactHandler(ArtefactHandler handler) {
        throw new UnsupportedOperationException();
    }

    public boolean hasArtefactHandler(String type) {
        throw new UnsupportedOperationException();
    }

    public ArtefactHandler[] getArtefactHandlers() {
        throw new UnsupportedOperationException();
    }

    public void initialise() {
        throw new UnsupportedOperationException();
    }

    public boolean isInitialised() {
        throw new UnsupportedOperationException();
    }

    public Metadata getMetadata() {
        throw new UnsupportedOperationException();
    }

    public GrailsClass getArtefactByLogicalPropertyName(String type, String logicalName) {
        throw new UnsupportedOperationException();
    }

    public void addArtefact(Class artefact) {
        throw new UnsupportedOperationException();
    }

    public boolean isWarDeployed() {
        throw new UnsupportedOperationException();
    }

    public void addOverridableArtefact(Class artefact) {
        throw new UnsupportedOperationException();
    }

    public void configChanged() {
        throw new UnsupportedOperationException();
    }

    public ArtefactHandler getArtefactHandler(String type) {
        throw new UnsupportedOperationException();
    }
}
