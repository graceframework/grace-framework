/*
 * Copyright 2004-2023 the original author or authors.
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
package grails.util

import java.util.function.Supplier
import java.util.jar.Attributes
import java.util.jar.Manifest

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import grails.io.IOUtils

import org.grails.io.support.Resource
import org.grails.io.support.UrlResource

/**
 * Represents the current environment.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@CompileStatic
@SuppressWarnings('FieldName')
enum Environment {

    /** The development environment */
    DEVELOPMENT,

    /** The production environment */
    PRODUCTION,

    /** The test environment */
    TEST,

    /**
     * For the application data source, primarily for backward compatibility for those applications
     * that use ApplicationDataSource.groovy.
     */
    APPLICATION,

    /** A custom environment */
    CUSTOM

    private static final Supplier<Logger> LOG = SupplierUtil.memoized(() -> LoggerFactory.getLogger(Environment))

    /**
     * Constant used to resolve the environment via System.getProperty(Environment.KEY)
     */
    public static String KEY = 'grails.env'

    /**
     * Constant used to resolve the environment via System.getenv(Environment.ENV_KEY).
     */
    public static final String ENV_KEY = 'GRAILS_ENV'

    /**
     * The name of the GRAILS_HOME environment variable
     */
    public static String ENV_GRAILS_HOME = 'GRAILS_HOME'

    /**
     * Specify whether reloading is enabled for this environment
     */
    public static String RELOAD_ENABLED = 'grails.reload.enabled'

    /**
     * Constant indicating whether run-app or test-app was executed
     */
    public static String RUN_ACTIVE = 'grails.run.active'

    /**
     * Whether the display of full stack traces is needed
     */
    public static String FULL_STACKTRACE = 'grails.full.stacktrace'

    /**
     * The location where to reload resources from
     */
    public static final String RELOAD_LOCATION = 'grails.reload.location'

    /**
     * Whether interactive mode is enabled
     */
    public static final String INTERACTIVE_MODE_ENABLED = 'grails.interactive.mode.enabled'

    /**
     * Constants that indicates whether this GrailsApplication is running in the default environment
     */
    public static final String DEFAULT = 'grails.env.default'

    /**
     * Whether Grails is in the middle of bootstrapping or not
     */
    public static final String INITIALIZING = 'grails.env.initializing'

    /**
     * Whether Grails has been executed standalone via the static void main method and not loaded in via the container
     */
    public static final String STANDALONE = 'grails.env.standalone'

    private static final String PRODUCTION_ENV_SHORT_NAME = 'prod'

    private static final String DEVELOPMENT_ENVIRONMENT_SHORT_NAME = 'dev'
    private static final String TEST_ENVIRONMENT_SHORT_NAME = 'test'

    private static final Map<String, String> ENV_NAME_MAPPINGS = CollectionUtils.<String, String> newMap(
            DEVELOPMENT_ENVIRONMENT_SHORT_NAME, DEVELOPMENT.getName(),
            PRODUCTION_ENV_SHORT_NAME, PRODUCTION.getName(),
            TEST_ENVIRONMENT_SHORT_NAME, TEST.getName())
    private static final Holder<Environment> cachedCurrentEnvironment = new Holder<>('Environment')
    private static final boolean DEVELOPMENT_MODE = getCurrent() == DEVELOPMENT && BuildSettings.GRAILS_APP_DIR_PRESENT
    private static Boolean RELOADING_AGENT_ENABLED = null
    private static boolean initializingState = false

    private static final String GRAILS_IMPLEMENTATION_TITLE = 'Grails'
    private static final String GRAILS_VERSION
    private static final boolean STANDALONE_DEPLOYED
    private static final boolean WAR_DEPLOYED

    static {
        Package p = Environment.getPackage()
        String version = p != null ? p.getImplementationVersion() : null
        if (version == null || isBlank(version)) {
            try {
                URL manifestURL = IOUtils.findResourceRelativeToClass(Environment, '/META-INF/MANIFEST.MF')
                Manifest grailsManifest = null
                if (manifestURL != null) {
                    Resource r = new UrlResource(manifestURL)
                    if (r.exists()) {
                        InputStream inputStream = null
                        Manifest mf = null
                        try {
                            inputStream = r.getInputStream()
                            mf = new Manifest(inputStream)
                        }
                        finally {
                            try {
                                inputStream.close()
                            }
                            catch (IOException ignored) {
                            }
                        }
                        String implTitle = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE)
                        if (!isBlank(implTitle) && implTitle == GRAILS_IMPLEMENTATION_TITLE) {
                            grailsManifest = mf
                        }
                    }
                }

                if (grailsManifest != null) {
                    version = grailsManifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION)
                }

                if (isBlank(version)) {
                    Properties grailsBuildProperties = new Properties()
                    grailsBuildProperties.load(BuildSettings.getResourceAsStream('/grails-build.properties'))
                    version = grailsBuildProperties.getProperty('grails.version')
                }

                if (isBlank(version)) {
                    version = 'Unknown'
                }
            }
            catch (Exception e) {
                version = 'Unknown'
            }
        }
        GRAILS_VERSION = version

        URL url = Environment.getResource('')
        if (url != null) {
            String protocol = url.getProtocol()
            if (protocol == 'jar') {
                String fullPath = url
                if (fullPath.contains(IOUtils.RESOURCE_WAR_PREFIX)) {
                    STANDALONE_DEPLOYED = true
                }
                else {
                    int i = fullPath.indexOf(IOUtils.RESOURCE_JAR_PREFIX)
                    if (i > -1) {
                        fullPath = fullPath.substring(i + IOUtils.RESOURCE_JAR_PREFIX.length())
                        STANDALONE_DEPLOYED = fullPath.contains(IOUtils.RESOURCE_JAR_PREFIX)
                    }
                    else {
                        STANDALONE_DEPLOYED = false
                    }
                }
            }
            else {
                STANDALONE_DEPLOYED = false
            }
        }
        else {
            STANDALONE_DEPLOYED = false
        }

        URL loadedLocation = Environment.getClassLoader().getResource(Metadata.FILE)
        if (loadedLocation != null) {
            String path = loadedLocation.getPath()
            WAR_DEPLOYED = isWebPath(path)
        }
        else {
            loadedLocation = Thread.currentThread().getContextClassLoader().getResource(Metadata.FILE)
            if (loadedLocation != null) {
                String path = loadedLocation.getPath()
                WAR_DEPLOYED = isWebPath(path)
            }
            else {
                WAR_DEPLOYED = false
            }
        }
    }

    public static Throwable currentReloadError = null
    public static MultipleCompilationErrorsException currentCompilationError = null
    private String name
    private String reloadLocation

    Environment() {
        initialize()
    }

    /**
     * @return The current Grails version
     */
    static String getGrailsVersion() {
        GRAILS_VERSION
    }

    static void setCurrentReloadError(Throwable currentReloadError) {
        Environment.@currentReloadError = currentReloadError
    }

    static MultipleCompilationErrorsException getCurrentCompilationError() {
        currentCompilationError
    }

    static Throwable getCurrentReloadError() {
        currentReloadError
    }

    static boolean isReloadInProgress() {
        Boolean.getBoolean('grails.reloading.in.progress')
    }

    private void initialize() {
        name = toString().toLowerCase(Locale.ENGLISH)
    }

    /**
     * Returns the current environment which is typically either DEVELOPMENT, PRODUCTION or TEST.
     * For custom environments CUSTOM type is returned.
     *
     * @return The current environment.
     */
    static Environment getCurrent() {
        String envName = getEnvironmentInternal()

        Environment env
        if (!isBlank(envName)) {
            env = getEnvironment(envName)
            if (env != null) {
                return env
            }
        }

        Environment current = cachedCurrentEnvironment.get()
        if (current != null) {
            return current
        }
        cacheCurrentEnvironment()
    }

    private static Environment resolveCurrentEnvironment() {
        String envName = getEnvironmentInternal()

        if (isBlank(envName)) {
            Metadata metadata = Metadata.getCurrent()
            if (metadata != null) {
                envName = metadata.getEnvironment()
            }
            if (isBlank(envName)) {
                return DEVELOPMENT
            }
        }

        Environment env = getEnvironment(envName)
        if (env == null) {
            try {
                env = valueOf(envName.toUpperCase())
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        if (env == null) {
            env = CUSTOM
            env.setName(envName)
        }
        env
    }

    private static Environment cacheCurrentEnvironment() {
        Environment env = resolveCurrentEnvironment()
        cachedCurrentEnvironment.set(env)
        env
    }

    /**
     * @see #getCurrent()
     * @return the current environment
     */
    static Environment getCurrentEnvironment() {
        getCurrent()
    }

    /**
     * Reset the current environment
     */
    static void reset() {
        cachedCurrentEnvironment.set(null)
        Metadata.reset()
    }

    /**
     * Returns true if the application is running in development mode (within grails run-app)
     *
     * @return true if the application is running in development mode
     */
    static boolean isDevelopmentMode() {
        DEVELOPMENT_MODE
    }

    /**
     * This method will return true if the 'grails-app' directory was found, regardless of whether reloading is active or not
     *
     * @return True if the development sources are present
     */
    static boolean isDevelopmentEnvironmentAvailable() {
        BuildSettings.GRAILS_APP_DIR_PRESENT && !isStandaloneDeployed() && !isWarDeployed()
    }

    /**
     * This method will return true the application is run
     *
     * @return True if the development sources are present
     */
    static boolean isDevelopmentRun() {
        Environment env = getCurrent()
        isDevelopmentEnvironmentAvailable() && Boolean.getBoolean(RUN_ACTIVE) && (env == DEVELOPMENT)
    }

    /**
     * Check whether the application is deployed
     * @return true if is
     */
    static boolean isWarDeployed() {
        if (!isStandalone()) {
            return WAR_DEPLOYED
        }
        false
    }

    private static boolean isWebPath(String path) {
        // Workaround for WebLogic who repacks files from 'classes' into a new jar under lib/
        path.contains('/WEB-INF/classes') || path.contains('_wl_cls_gen.jar!/')
    }

    /**
     * Whether the application has been executed standalone via static void main.
     *
     * This method will return true when the application is executed via `java -jar` or
     * if the application is run directly via the main method within an IDE
     *
     * @return True if it is running standalone outside of a servlet container
     */
    static boolean isStandalone() {
        Boolean.getBoolean(STANDALONE)
    }

    /**
     * Whether the application is running standalone within a JAR
     *
     * This method will return true only if the the application is executed via `java -jar`
     * and not if it is run via the main method within an IDE
     *
     * @return True if it is running standalone outside a servlet container from within a JAR or WAR file
     */
    static boolean isStandaloneDeployed() {
        isStandalone() && STANDALONE_DEPLOYED
    }

    /**
     * Whether this is a fork of the Grails command line environment
     * @return True if it is a fork
     */
    static boolean isFork() {
        Boolean.getBoolean('grails.fork.active')
    }

    /**
     * Returns whether the environment is running within the Grails shell (executed via the 'grails' command line in a terminal window)
     * @return true if is
     */
    static boolean isWithinShell() {
        DefaultGroovyMethods.getRootLoader(Environment.getClassLoader()) != null
    }

    /**
     * @return Return true if the environment has been set as a System property
     */
    static boolean isSystemSet() {
        getEnvironmentInternal() != null
    }

    /**
     * Returns the environment for the given short name
     * @param shortName The short name
     * @return The Environment or null if not known
     */
    static Environment getEnvironment(String shortName) {
        String envName = ENV_NAME_MAPPINGS.get(shortName)
        if (envName != null) {
            return valueOf(envName.toUpperCase())
        }
        try {
            return valueOf(shortName.toUpperCase())
        }
        catch (IllegalArgumentException ise) {
            return null
        }
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And returns the closure that relates to the current environment
     *
     * @param closure The top level closure
     * @return The environment specific block or null if non exists
     */
    static Closure<?> getEnvironmentSpecificBlock(Closure<?> closure) {
        Environment env = getCurrent()
        getEnvironmentSpecificBlock(env, closure)
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And returns the closure that relates to the specified
     *
     * @param env The environment to use
     * @param closure The top level closure
     * @return The environment specific block or null if non exists
     */
    static Closure<?> getEnvironmentSpecificBlock(Environment env, Closure<?> closure) {
        if (closure == null) {
            return null
        }

        EnvironmentBlockEvaluator evaluator = evaluateEnvironmentSpecificBlock(env, closure)
        evaluator.getCallable()
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And executes the closure that relates to the current environment
     *
     * @param closure The top level closure
     * @return The result of the closure execution
     */
    static Object executeForCurrentEnvironment(Closure<?> closure) {
        Environment env = getCurrent()
        executeForEnvironment(env, closure)
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And executes the closure that relates to the specified environment
     *
     * @param env The environment to use
     * @param closure The top level closure
     * @return The result of the closure execution
     */
    static Object executeForEnvironment(Environment env, Closure<?> closure) {
        if (closure == null) {
            return null
        }

        EnvironmentBlockEvaluator evaluator = evaluateEnvironmentSpecificBlock(env, closure)
        evaluator.execute()
    }

    private static EnvironmentBlockEvaluator evaluateEnvironmentSpecificBlock(Environment environment, Closure<?> closure) {
        EnvironmentBlockEvaluator evaluator = new EnvironmentBlockEvaluator(environment)
        closure.setDelegate(evaluator)
        closure.call()
        evaluator
    }

    private static boolean isBlank(String value) {
        value == null || value.trim().length() == 0
    }

    /**
     * @return the name of the environment
     */
    String getName() {
        name
    }

    /**
     * Set the name.
     * @param name the name
     */
    void setName(String name) {
        this.name = name
    }

    /**
     * @return Returns whether reload is enabled for the environment
     */
    boolean isReloadEnabled() {
        boolean reloadOverride = Boolean.getBoolean(RELOAD_ENABLED)
        getReloadLocation()
        boolean reloadLocationSpecified = hasLocation(reloadLocation)
        this == DEVELOPMENT && reloadLocationSpecified || reloadOverride && reloadLocationSpecified
    }

    /**
     *
     * @return Whether interactive mode is enabled
     */
    static boolean isInteractiveMode() {
        Boolean.getBoolean(INTERACTIVE_MODE_ENABLED)
    }

    /**
     *
     * @return Whether interactive mode is enabled
     */
    static boolean isInitializing() {
        initializingState
    }

    static void setInitializing(boolean initializing) {
        initializingState = initializing
        System.setProperty(INITIALIZING, String.valueOf(initializing))
    }

    /**
     * @return true if the reloading agent is active
     */
    static boolean isReloadingAgentEnabled() {
        if (RELOADING_AGENT_ENABLED != null) {
            return RELOADING_AGENT_ENABLED
        }
        try {
            Class.forName('org.springframework.boot.devtools.RemoteSpringApplication')
            boolean devToolsEnabled = Metadata.current.getProperty('spring.devtools.restart.enabled', Boolean, true)
            RELOADING_AGENT_ENABLED = getCurrent().isReloadEnabled() && devToolsEnabled
            LOG.get().debug("Found 'spring-boot-devtools' on the classpath, spring.devtools.restart.enabled: $devToolsEnabled")
        }
        catch (ClassNotFoundException e) {
            RELOADING_AGENT_ENABLED = false
        }
        RELOADING_AGENT_ENABLED
    }

    /**
     * @return Obtains the location to reload resources from
     */
    String getReloadLocation() {
        if (this.reloadLocation != null) {
            return this.reloadLocation
        }
        String location = getReloadLocationInternal()
        if (hasLocation(location)) {
            reloadLocation = location
            return location
        }
        '.' // default to the current directory
    }

    private static boolean hasLocation(String location) {
        location != null && location.length() > 0
    }

    /**
     * @return Whether a reload location is specified
     */
    boolean hasReloadLocation() {
        getReloadLocation()
        hasLocation(reloadLocation)
    }

    private static String getReloadLocationInternal() {
        String location = System.getProperty(RELOAD_LOCATION)
        if (!hasLocation(location)) {
            location = System.getProperty(BuildSettings.APP_BASE_DIR)
        }
        if (!hasLocation(location)) {
            String file = ['grails-app', 'app', 'settings.gradle'].find { new File('.', it).exists() }
            if (file == 'settings.gradle') {
                location = IOUtils.findApplicationDirectory()
            }
            else if (file) {
                File current = new File('.', file)
                location = current.getParentFile().getAbsolutePath()
            }
        }
        location
    }

    private static String getEnvironmentInternal() {
        String envName = System.getProperty(KEY)
        isBlank(envName) ? System.getenv(ENV_KEY) : envName
    }

}
