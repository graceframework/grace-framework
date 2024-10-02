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
package grails.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import grails.util.Environment;

/**
 * Extends the {@link SpringApplication} with reloading behavior and other Grails features
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
public class Grails extends SpringApplication {

    private static final String GRAILS_BANNER = "grails-banner.txt";

    /**
     * Create a new {@link Grails} instance. The application context will load
     * beans from the specified sources (see {@link SpringApplication class-level}
     * documentation for details. The instance can be customized before calling
     * {@link #run(String...)}.
     * @param sources the bean sources
     */
    public Grails(Class<?>... sources) {
        super(sources);
    }

    /**
     * Create a new {@link Grails} instance. The application context will load
     * beans from the specified sources (see {@link SpringApplication class-level}
     * documentation for details. The instance can be customized before calling
     * {@link #run(String...)}.
     * @param resourceLoader the resource loader to use
     * @param sources the bean sources
     */
    public Grails(ResourceLoader resourceLoader, Class<?>... sources) {
        super(resourceLoader, sources);
    }

    @Override
    public ConfigurableApplicationContext run(String... args) {
        Environment environment = Environment.getCurrent();
        configureBanner(environment);

        return super.run(args);
    }

    @Override
    protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {
        Environment env = Environment.getCurrent();
        environment.addActiveProfile(env.getName());
    }

    protected void configureBanner(Environment environment) {
        ClassPathResource resource = new ClassPathResource(GRAILS_BANNER);
        if (resource.exists()) {
            setBanner(new GrailsResourceBanner(resource));
        }
        else {
            setBanner(new GrailsBanner());
        }
    }

    /**
     * Static helper that can be used to run a {@link Grails} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class<?> source, String... args) {
        return run(new Class<?>[] { source }, args);
    }

    /**
     * Static helper that can be used to run a {@link Grails} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class<?>[] sources, String[] args) {
        Grails grails = new Grails(sources);
        return grails.run(args);
    }

    public static void main(String[] args) throws Exception {
        Grails.run(new Class<?>[0], args);
    }

}
