/*
 * Copyright 2014-2023 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import grails.compiler.ast.ClassInjector;
import grails.core.GrailsApplication;
import grails.io.IOUtils;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.util.BuildSettings;
import grails.util.Environment;

import org.grails.boot.internal.JavaCompiler;
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.grails.io.watch.DirectoryWatcher;
import org.grails.io.watch.FileExtensionFileChangeListener;
import org.grails.plugins.BinaryGrailsPlugin;
import org.grails.plugins.support.WatchPattern;

/**
 * Extends the {@link SpringApplication} with reloading behavior and other Grails features
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
public class Grails extends SpringApplication {

    private static final String GRAILS_BANNER = "grails-banner.txt";

    private static final String SPRING_PROFILES = "spring.profiles.active";

    private static boolean developmentModeActive = false;

    private static DirectoryWatcher directoryWatcher;

    protected ConfigurableEnvironment configuredEnvironment;

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
        ConfigurableApplicationContext applicationContext = super.run(args);

        Log log = getApplicationLog();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Application directory discovered as: %s", IOUtils.findApplicationDirectory()));
            log.debug(String.format("Current base directory is [%s]. Reloading base directory is [%s]",
                    new File("."), BuildSettings.BASE_DIR));
        }
        if (environment.isReloadEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Reloading status: %s", environment.isReloadEnabled()));
            }
            try {
                enableDevelopmentModeWatch(environment, applicationContext);
            }
            catch (IOException e) {
                log.error("Enable development mode watch fail", e);
            }
        }

        printRunStatus(applicationContext);

        return applicationContext;
    }

    @Override
    protected ConfigurableApplicationContext createApplicationContext() {
        setAllowBeanDefinitionOverriding(true);
        setAllowCircularReferences(true);

        ConfigurableApplicationContext applicationContext = super.createApplicationContext();
        return applicationContext;
    }

    @Override
    protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
        configurePropertySources(environment, args);

        String[] springProfile = environment.getProperty(SPRING_PROFILES, String[].class);
        if (springProfile != null && springProfile.length > 0) {
            environment.setActiveProfiles(springProfile);
        }

        Environment env = Environment.getCurrent();
        environment.addActiveProfile(env.getName());
        this.configuredEnvironment = environment;
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

    protected void enableDevelopmentModeWatch(Environment environment, ConfigurableApplicationContext applicationContext, String... args)
            throws IOException {
        String location = environment.getReloadLocation();

        if (location != null && location.length() > 0) {
            directoryWatcher = new DirectoryWatcher();

            Queue<File> changedFiles = new ConcurrentLinkedQueue<>();
            Queue<File> newFiles = new ConcurrentLinkedQueue<>();

            directoryWatcher.addListener(new FileExtensionFileChangeListener(Arrays.asList("groovy", "java")) {

                @Override
                public void onChange(File file, List<String> extensions) throws IOException {
                    changedFiles.add(file.getCanonicalFile());
                }

                @Override
                public void onNew(File file, List<String> extensions) throws IOException {
                    changedFiles.add(file.getCanonicalFile());
                    // For some bizarro reason Windows fires onNew events even for files that have
                    // just been modified and not created
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        return;
                    }
                    newFiles.add(file.getCanonicalFile());
                }

            });

            GrailsPluginManager pluginManager = applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
            DirectoryWatcher.FileChangeListener pluginManagerListener = createPluginManagerListener(applicationContext);
            directoryWatcher.addListener(pluginManagerListener);

            File baseDir = new File(location).getCanonicalFile();
            String baseDirPath = baseDir.getCanonicalPath();
            List<File> watchBaseDirectories = new ArrayList<>(Collections.singletonList(baseDir));
            for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
                if (plugin instanceof BinaryGrailsPlugin) {
                    BinaryGrailsPlugin binaryGrailsPlugin = (BinaryGrailsPlugin) plugin;
                    File pluginDirectory = binaryGrailsPlugin.getProjectDirectory();
                    if (pluginDirectory != null) {
                        watchBaseDirectories.add(pluginDirectory);
                    }
                }
            }

            for (File dir : watchBaseDirectories) {
                configureDirectoryWatcher(directoryWatcher, dir.getAbsolutePath());
            }

            for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
                List<WatchPattern> watchedResourcePatterns = plugin.getWatchedResourcePatterns();
                if (watchedResourcePatterns != null) {
                    for (WatchPattern wp : new ArrayList<>(watchedResourcePatterns)) {
                        boolean first = true;
                        for (File watchBase : watchBaseDirectories) {
                            if (!first) {
                                if (wp.getFile() != null) {
                                    String relativePath = wp.getFile().getCanonicalPath().substring(
                                            wp.getFile().getCanonicalPath().indexOf(baseDirPath) + baseDirPath.length());
                                    File watchFile = new File(watchBase, relativePath);
                                    // the base project will already been in the list of watch patterns, but we add any subprojects here
                                    WatchPattern watchPattern = new WatchPattern();
                                    watchPattern.setFile(watchFile);
                                    watchPattern.setExtension(wp.getExtension());
                                    plugin.getWatchedResourcePatterns().add(watchPattern);
                                }
                                else if (wp.getDirectory() != null) {
                                    String relativePath = wp.getDirectory().getCanonicalPath().substring(
                                            wp.getDirectory().getCanonicalPath().indexOf(baseDirPath) + baseDirPath.length());
                                    File watchDir = new File(watchBase, relativePath);
                                    // the base project will already been in the list of watch patterns, but we add any subprojects here
                                    WatchPattern watchPattern = new WatchPattern();
                                    watchPattern.setDirectory(watchDir);
                                    watchPattern.setExtension(wp.getExtension());
                                    plugin.getWatchedResourcePatterns().add(watchPattern);
                                }
                            }
                            first = false;
                            if (wp.getFile() != null) {
                                String relativePath = wp.getFile().getCanonicalPath().substring(
                                        wp.getFile().getCanonicalPath().indexOf(baseDirPath) + baseDirPath.length());
                                File resolvedPath = new File(watchBase, relativePath);
                                directoryWatcher.addWatchFile(resolvedPath);
                            }
                            else if (wp.getDirectory() != null && wp.getExtension() != null) {
                                String relativePath = wp.getDirectory().getCanonicalPath().substring(
                                        wp.getDirectory().getCanonicalPath().indexOf(baseDirPath) + baseDirPath.length());
                                File resolvedPath = new File(watchBase, relativePath);
                                directoryWatcher.addWatchDirectory(resolvedPath, wp.getExtension());
                            }
                        }
                    }
                }
            }

            developmentModeActive = true;
            new Thread(() -> {
                Log log = getApplicationLog();
                CompilerConfiguration compilerConfig = new CompilerConfiguration();
                compilerConfig.setTargetDirectory(new File(location, BuildSettings.BUILD_CLASSES_PATH));

                while (isDevelopmentModeActive()) {
                    // Workaround for some IDE / OS combos - 2 events (new + update) for the same file
                    Set<File> uniqueChangedFiles = new HashSet<>(Arrays.asList(changedFiles.toArray(new File[0])));

                    int i = uniqueChangedFiles.size();
                    try {
                        if (i > 1) {
                            changedFiles.clear();
                            for (File f : uniqueChangedFiles) {
                                recompile(f, compilerConfig, location);
                                if (newFiles.contains(f)) {
                                    newFiles.remove(f);
                                }
                                pluginManager.informOfFileChange(f);
                                try {
                                    Thread.sleep(1000);
                                }
                                catch (InterruptedException ignored) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                        else if (i == 1) {
                            changedFiles.clear();
                            File changedFile = uniqueChangedFiles.iterator().next();
                            changedFile = changedFile.getCanonicalFile();
                            // Groovy files within the 'conf' directory are not compiled
                            boolean configFileChanged = false;
                            for (String dir : Arrays.asList("grails-app", "app")) {
                                String confPath = File.separator + dir + File.separator + "conf" + File.separator;
                                if (changedFile.getPath().contains(confPath)) {
                                    configFileChanged = true;
                                }
                            }
                            if (configFileChanged) {
                                pluginManager.informOfFileChange(changedFile);
                            }
                            else {
                                recompile(changedFile, compilerConfig, location);
                                if (newFiles.contains(changedFile)) {
                                    newFiles.remove(changedFile);
                                }
                                pluginManager.informOfFileChange(changedFile);
                            }
                        }

                        newFiles.clear();
                    }
                    catch (CompilationFailedException | IOException cfe) {
                        log.error(String.format("Compilation Error: %s", cfe.getMessage()), cfe);
                    }

                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
            directoryWatcher.start();
        }
    }

    public static boolean isDevelopmentModeActive() {
        return developmentModeActive;
    }

    public static void setDevelopmentModeActive(boolean active) {
        developmentModeActive = active;
        if (directoryWatcher != null) {
            directoryWatcher.setActive(active);
        }
    }

    protected void recompile(File changedFile, CompilerConfiguration compilerConfig, String location) {
        String changedPath = changedFile.getPath();

        String sourceMainGroovy = "src" + File.separator + "main" + File.separator + "groovy";

        File appDir = null;
        for (String dir : Arrays.asList("grails-app", "app", sourceMainGroovy)) {
            String changedDir = File.separator + dir;
            if (changedPath.contains(changedDir)) {
                appDir = new File(changedPath.substring(0, changedPath.indexOf(changedDir)));
                break;
            }
        }

        String baseFileLocation = appDir != null ? appDir.getAbsolutePath() : location;
        compilerConfig.setTargetDirectory(new File(baseFileLocation, BuildSettings.BUILD_CLASSES_PATH));

        System.out.printf("File %s changed, recompiling...%n", changedFile);

        if (changedFile.getName().endsWith(".java")) {
            if (JavaCompiler.isAvailable()) {
                JavaCompiler.recompile(compilerConfig, changedFile);
            }
            else {
                Log log = getApplicationLog();
                log.error(String.format("Cannot recompile [%s], " +
                                "the current JVM is not a JDK (recompilation will not work on a JRE missing the compiler APIs).",
                        changedFile.getName()));
            }
        }
        else {
            compileGroovyFile(compilerConfig, changedFile);
        }
    }

    protected void compileGroovyFile(CompilerConfiguration compilerConfig, File changedFile) {
        ClassInjector[] classInjectors = GrailsAwareInjectionOperation.getClassInjectors();
        for (ClassInjector classInjector : classInjectors) {
            if (classInjector instanceof AbstractGrailsArtefactTransformer) {
                ((AbstractGrailsArtefactTransformer) classInjector).clearCachedState();
            }
        }
        // only one change, just to a simple recompile and propagate the change
        CompilationUnit unit = new CompilationUnit(compilerConfig);
        unit.addSource(changedFile);
        unit.compile();
    }

    /**
     * Creates and returns a file change listener for notifying the plugin manager of changes.
     * @param applicationContext - The running {@link org.springframework.context.ApplicationContext}
     * @return {@link DirectoryWatcher.FileChangeListener}
     */
    protected static DirectoryWatcher.FileChangeListener createPluginManagerListener(ConfigurableApplicationContext applicationContext) {
        GrailsPluginManager pluginManager = applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);

        return new DirectoryWatcher.FileChangeListener() {

            @Override
            public void onChange(File file) {
                if (!file.getName().endsWith(".groovy") && !file.getName().endsWith(".java")) {
                    pluginManager.informOfFileChange(file);
                }
            }

            @Override
            public void onNew(File file) {
                if (!file.getName().endsWith(".groovy") && !file.getName().endsWith(".java")) {
                    pluginManager.informOfFileChange(file);
                }
            }

        };
    }

    protected void configureDirectoryWatcher(DirectoryWatcher directoryWatcher, String location) {
        for (String dir : Arrays.asList("grails-app", "app", "src/main/groovy", "src/main/java")) {
            directoryWatcher.addWatchDirectory(new File(location, dir), Arrays.asList("groovy", "java"));
        }
    }

    protected void printRunStatus(ConfigurableApplicationContext applicationContext) {
        try {
            GrailsApplication app = applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            String protocol = app.getConfig().getProperty("server.ssl.key-store") != null ? "https" : "http";
            if (applicationContext.getParent() != null) {
                applicationContext.publishEvent(
                        new ApplicationPreparedEvent(
                                this,
                                new String[0],
                                (ConfigurableApplicationContext) applicationContext.getParent())
                );
            }
            String contextPath = app.getConfig().getProperty("server.servlet.context-path", "");
            String hostName = app.getConfig().getProperty("server.address", "localhost");
            int port = 8080;
            if (applicationContext instanceof WebServerApplicationContext) {
                port = ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
            }
            String hostAddress = "localhost";
            try {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (UnknownHostException e) {
                getApplicationLog().warn("The host name could not be determined, using `localhost` as fallback");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("%n----------------------------------------------------------------------------------------------");
            sb.append("%n        Application:   %s");
            sb.append("%n        Version:       %s");
            sb.append("%n        Environment:   %s");
            sb.append("%n        Local:         %s://%s:%s%s");
            sb.append("%n        External:      %s://%s:%s%s");
            sb.append("%n----------------------------------------------------------------------------------------------");
            sb.append("%n");
            getApplicationLog().info(String.format(sb.toString(),
                    app.getConfig().getProperty("info.app.name"),
                    app.getConfig().getProperty("info.app.version"),
                    Environment.getCurrent().getName(),
                    protocol,
                    hostName,
                    port,
                    contextPath,
                    protocol,
                    hostAddress,
                    port,
                    contextPath));
        }
        catch (Exception ignored) {
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
