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
package org.grails.boot.context;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import org.grails.boot.context.GrailsConfigurationWarningsApplicationContextInitializer.ComponentScanPackageCheck;
import org.grails.boot.context.configwarnings.dflt.InDefaultPackageConfiguration;
import org.grails.boot.context.configwarnings.dflt.InDefaultPackageWithBasePackageClassesConfiguration;
import org.grails.boot.context.configwarnings.dflt.InDefaultPackageWithBasePackagesConfiguration;
import org.grails.boot.context.configwarnings.dflt.InDefaultPackageWithValueConfiguration;
import org.grails.boot.context.configwarnings.dflt.InDefaultPackageWithoutScanConfiguration;
import org.grails.boot.context.configwarnings.orggrails.InOrgGrailsPackageConfiguration;
import org.grails.boot.context.configwarnings.real.InRealButScanningProblemPackages;
import org.grails.boot.context.configwarnings.real.InRealPackageConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrailsConfigurationWarningsApplicationContextInitializer}.
 *
 * @author Michael Yan
 */
@ExtendWith(OutputCaptureExtension.class)
public class GrailsConfigurationWarningsApplicationContextInitializerTests {

    private static final String DEFAULT_SCAN_WARNING = "Your ApplicationContext is unlikely to "
            + "start due to a @GrailsComponentScan of the default package.";

    private static final String ORGGRAILS_SCAN_WARNING = "Your ApplicationContext is unlikely to "
            + "start due to a @GrailsComponentScan of 'org.grails'.";

    @Test
    void logWarningInDefaultPackage(CapturedOutput output) {
        load(InDefaultPackageConfiguration.class);
        assertThat(output).contains(DEFAULT_SCAN_WARNING);
    }

    @Test
    void noLogIfInRealPackage(CapturedOutput output) {
        load(InRealPackageConfiguration.class);
        assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
    }

    @Test
    void noLogWithoutComponentScanAnnotation(CapturedOutput output) {
        load(InDefaultPackageWithoutScanConfiguration.class);
        assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
    }

    @Test
    void noLogIfHasValue(CapturedOutput output) {
        load(InDefaultPackageWithValueConfiguration.class);
        assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
    }

    @Test
    void noLogIfHasBasePackages(CapturedOutput output) {
        load(InDefaultPackageWithBasePackagesConfiguration.class);
        assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
    }

    @Test
    void noLogIfHasBasePackageClasses(CapturedOutput output) {
        load(InDefaultPackageWithBasePackageClassesConfiguration.class);
        assertThat(output).doesNotContain(DEFAULT_SCAN_WARNING);
    }

    @Test
    void logWarningInOrgGrailsPackage(CapturedOutput output) {
        load(InOrgGrailsPackageConfiguration.class);
        assertThat(output).contains(ORGGRAILS_SCAN_WARNING);
    }

    @Test
    void logWarningIfScanningProblemPackages(CapturedOutput output) {
        load(InRealButScanningProblemPackages.class);
        assertThat(output).contains("Your ApplicationContext is unlikely to start due to a "
                + "@GrailsComponentScan of the default package, 'org.grails'.");

    }

    private void load(Class<?> configClass) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            new TestConfigurationWarningsApplicationContextInitializer().initialize(context);
            context.register(configClass);
            context.refresh();
        }
    }

    /**
     * Testable version of {@link GrailsConfigurationWarningsApplicationContextInitializer}.
     */
    static class TestConfigurationWarningsApplicationContextInitializer
            extends GrailsConfigurationWarningsApplicationContextInitializer {

        @Override
        protected Check[] getChecks() {
            return new Check[] { new TestComponentScanPackageCheck() };
        }

    }

    /**
     * Testable ComponentScanPackageCheck that doesn't need to use the default or
     * {@code org.grails} package.
     */
    static class TestComponentScanPackageCheck extends ComponentScanPackageCheck {

        @Override
        protected Set<String> getComponentScanningPackages(BeanDefinitionRegistry registry) {
            Set<String> scannedPackages = super.getComponentScanningPackages(registry);
            Set<String> result = new LinkedHashSet<>();
            for (String scannedPackage : scannedPackages) {
                if (scannedPackage.endsWith("dflt")) {
                    result.add("");
                }
                else if (scannedPackage.endsWith("orggrails")) {
                    result.add("org.grails");
                }
                else {
                    result.add(scannedPackage);
                }
            }
            return result;
        }

    }

}
