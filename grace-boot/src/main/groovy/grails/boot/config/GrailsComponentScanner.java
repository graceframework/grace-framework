/*
 * Copyright 2022 the original author or authors.
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
package grails.boot.config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A Grails classes scanner that searches the classpath from an {@link GrailsComponentScanner @GrailsComponentScanner}
 * specified packages.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsComponentScanner {

    private final ApplicationContext context;

    private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

    /**
     * Create a new {@link GrailsComponentScanner} instance.
     * @param context the source application context
     */
    public GrailsComponentScanner(ApplicationContext context) {
        Assert.notNull(context, "Context must not be null");
        this.context = context;
    }

    public GrailsComponentScanner(ApplicationContext context, ApplicationStartup applicationStartup) {
        this(context);
        this.applicationStartup = applicationStartup;
    }

    /**
     * Scan for entities with the specified annotations.
     * @param annotationTypes the annotation types used on the artefacts
     * @return a set of artefact classes
     * @throws ClassNotFoundException if an artefact class cannot be loaded
     */
    @SafeVarargs
    public final Set<Class<?>> scan(Class<? extends Annotation>... annotationTypes) throws ClassNotFoundException {
        StartupStep artefactScan = this.applicationStartup.start("grails.application.artefact-classes.scan");
        List<String> packages = getPackages();
        if (packages.isEmpty()) {
            artefactScan.tag("packages", "[]").end();
            return Collections.emptySet();
        }
        ClassPathScanningCandidateComponentProvider scanner = createClassPathScanningCandidateComponentProvider(
                this.context);
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
        }
        Set<Class<?>> entitySet = new HashSet<>();
        for (String basePackage : packages) {
            if (StringUtils.hasText(basePackage)) {
                for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                    entitySet.add(ClassUtils.forName(candidate.getBeanClassName(), this.context.getClassLoader()));
                }
            }
        }
        artefactScan.tag("packages", Arrays.toString(packages.toArray())).end();
        return entitySet;
    }

    /**
     * Create a {@link ClassPathScanningCandidateComponentProvider} to scan entities based
     * on the specified {@link ApplicationContext}.
     * @param context the {@link ApplicationContext} to use
     * @return a {@link ClassPathScanningCandidateComponentProvider} suitable to scan
     * entities
     * @since 2.4.0
     */
    protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
            ApplicationContext context) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.setEnvironment(context.getEnvironment());
        scanner.setResourceLoader(context);
        return scanner;
    }

    public List<String> getPackages() {
        List<String> packages = GrailsComponentScanPackages.get(this.context).getPackageNames();
        if (packages.isEmpty() && AutoConfigurationPackages.has(this.context)) {
            packages = AutoConfigurationPackages.get(this.context);
        }
        return packages;
    }

}
