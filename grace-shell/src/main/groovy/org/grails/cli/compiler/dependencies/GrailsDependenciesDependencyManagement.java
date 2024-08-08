/*
 * Copyright 2014-2024 the original author or authors.
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
package org.grails.cli.compiler.dependencies;

import java.io.InputStream;
import java.net.URL;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.locator.DefaultModelLocator;

import grails.util.GrailsVersion;
import org.grails.cli.compiler.maven.MavenVersionUtils;

/**
 * DependencyManagement derived from the effective pom of
 * {@code grails-bom}.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsDependenciesDependencyManagement extends MavenModelDependencyManagement {

    public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    public static final String SONATYPE_REPO_SNAPSHOT = "https://s01.oss.sonatype.org/content/repositories/snapshots/";

    private static final String GRAILS_BOM_URL = MAVEN_CENTRAL + "org/grails/grails-bom/%s/grails-bom-%s.pom";
    private static final String GRACE_BOM_URL = MAVEN_CENTRAL + "org/graceframework/grace-bom/%s/grace-bom-%s.pom";
    private static final String GRACE_BOM_SNAPSHOT_URL = SONATYPE_REPO_SNAPSHOT + "org/graceframework/grace-bom/%s/grace-bom-%s.pom";
    private static final String GRACE_BOM_SNAPSHOT_MAVEN_METADATA = SONATYPE_REPO_SNAPSHOT + "org/graceframework/grace-bom/%s/maven-metadata.xml";

    private final String grailsVersion;

    public GrailsDependenciesDependencyManagement() {
        this(null);
    }

    public GrailsDependenciesDependencyManagement(String grailsVersion) {
        super(readModel(grailsVersion));
        this.grailsVersion = grailsVersion;
    }

    private static Model readModel(String grailsVersion) {
        DefaultModelProcessor modelProcessor = new DefaultModelProcessor();
        modelProcessor.setModelLocator(new DefaultModelLocator());
        modelProcessor.setModelReader(new DefaultModelReader());

        try {
            InputStream is;
            if (grailsVersion != null) {
                URL url;
                if (GrailsVersion.isGraceSnapshotVersion(grailsVersion)) {
                    String mavenMetaUrl = String.format(GRACE_BOM_SNAPSHOT_MAVEN_METADATA, grailsVersion);
                    String buildSnapshotVersion = MavenVersionUtils.parseSnapshotVersion(mavenMetaUrl);
                    String graceSnapshotVersion = grailsVersion.substring(0, grailsVersion.indexOf("-")) + "-" + buildSnapshotVersion;
                    url = new URL(String.format(GRACE_BOM_SNAPSHOT_URL, grailsVersion, graceSnapshotVersion));
                }
                else if (GrailsVersion.isGrace(grailsVersion)) {
                    url = new URL(String.format(GRACE_BOM_URL, grailsVersion, grailsVersion));
                }
                else {
                    url = new URL(String.format(GRAILS_BOM_URL, grailsVersion, grailsVersion));
                }
                is = url.openStream();
            }
            else {
                is = GrailsDependenciesDependencyManagement.class.getResourceAsStream("grace-bom-effective.xml");
            }
            return modelProcessor.read(is, null);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to build model from effective pom", ex);
        }
    }

    public String getGrailsVersion() {
        String artifactId = this.grailsVersion == null || GrailsVersion.isGrace(this.grailsVersion) ?
                "grace-core" : "grails-core";
        return find(artifactId).getVersion();
    }

    public String getGrailsGradlePluginVersion() {
        String artifactId = this.grailsVersion == null || GrailsVersion.isGrace(this.grailsVersion) ?
                "grace-core" : "grails-gradle-plugin";
        Dependency dependency = find(artifactId);
        if (dependency == null && GrailsVersion.isGrails(this.grailsVersion)) {
            // Workaround for Grails 4.1.x
            dependency = find("grails-core");
        }
        return dependency.getVersion();
    }

    public String getGroovyVersion() {
        String artifactId = this.grailsVersion == null || GrailsVersion.isGrace(this.grailsVersion) ?
                "groovy-bom" : "groovy";
        return find(artifactId).getVersion();
    }

    public String getGormVersion() {
        String artifactId = this.grailsVersion == null || GrailsVersion.isGrace(this.grailsVersion) ?
                "grace-datastore-core" : "grails-datastore-core";
        return find(artifactId).getVersion();
    }

    @Override
    public String getSpringBootVersion() {
        return this.grailsVersion == null || GrailsVersion.isGrace(this.grailsVersion) ?
                find("spring-boot-dependencies").getVersion() : null;
    }

}
