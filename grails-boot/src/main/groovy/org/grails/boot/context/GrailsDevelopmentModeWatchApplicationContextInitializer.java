/*
 * Copyright 2022-2023 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import grails.compiler.ast.ClassInjector;
import grails.config.Config;
import grails.core.GrailsApplication;
import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.util.BuildSettings;
import grails.util.Environment;
import org.grails.boot.internal.JavaCompiler;
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.io.watch.DirectoryWatcher;
import org.grails.io.watch.FileExtensionFileChangeListener;
import org.grails.plugins.BinaryGrailsPlugin;
import org.grails.plugins.support.WatchPattern;

/**
 * {@link ApplicationContextInitializer} to enable development mode watch.
 *
 * @author Michael Yan
 * @see ApplicationContextInitializer
 * @since 2022.1.6
 */
public class GrailsDevelopmentModeWatchApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ApplicationEvent>, Ordered {

    private static final Log logger = LogFactory.getLog(GrailsDevelopmentModeWatchApplicationContextInitializer.class);

    private static final List<String> FILE_EXTENSIONS = List.of("groovy", "java", "properties", "xml");
    private static final String SOURCE_MAIN_JAVA = "src/main/java";
    private static final String SOURCE_MAIN_GROOVY = "src/main/groovy";
    private static final String[] DEFAULT_RESTART_EXCLUDES = new String[] {
            "META-INF/maven/**",
            "META-INF/resources/**",
            "resources/**",
            "static/**",
            "public/**",
            "templates/**",
            "**/*Test.class",
            "**/*Tests.class",
            "git.properties",
            "META-INF/build-info.properties"
    };

    private boolean developmentModeActive = false;
    private DirectoryWatcher directoryWatcher;

    private final AntPathMatcher matcher = new AntPathMatcher();

    private int order = Ordered.LOWEST_PRECEDENCE - 10;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.addApplicationListener(this);
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        Environment environment = Environment.getCurrent();
        if (event instanceof ApplicationStartedEvent) {
            ApplicationStartedEvent springApplicationEvent = (ApplicationStartedEvent) event;
            ConfigurableApplicationContext applicationContext = springApplicationEvent.getApplicationContext();
            GrailsApplication grailsApplication = applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);

            if (environment.isReloadEnabled()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Application reloading status: %s, base directory is [%s]",
                            environment.isReloadEnabled(), BuildSettings.BASE_DIR));
                }
                try {
                    enableDevelopmentModeWatch(environment, grailsApplication, applicationContext);
                }
                catch (IOException e) {
                    logger.error("Enable development mode watch fail", e);
                }
            }
        }
        else if (event instanceof ContextClosedEvent) {
            setDevelopmentModeActive(false);
        }
    }

    private void enableDevelopmentModeWatch(Environment environment,
                                            GrailsApplication grailsApplication,
                                            ConfigurableApplicationContext applicationContext) throws IOException {

        List<String> allExclude = getRestartExcludes(grailsApplication);
        String location = environment.getReloadLocation();

        if (location != null && location.length() > 0) {
            this.directoryWatcher = new DirectoryWatcher();

            Queue<File> changedFiles = new ConcurrentLinkedQueue<>();
            Queue<File> newFiles = new ConcurrentLinkedQueue<>();

            this.directoryWatcher.addListener(new FileExtensionFileChangeListener(FILE_EXTENSIONS) {

                @Override
                public void onChange(File file, List<String> extensions) throws IOException {
                    changedFiles.add(file.getCanonicalFile());
                }

                @Override
                public void onNew(File file, List<String> extensions) throws IOException {
                    changedFiles.add(file.getCanonicalFile());
                    // For some bizarre reason Windows fires onNew events even for files that have
                    // just been modified and not created
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        return;
                    }
                    newFiles.add(file.getCanonicalFile());
                }

            });

            final GrailsPluginManager pluginManager = applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
            DirectoryWatcher.FileChangeListener pluginManagerListener = createPluginManagerListener(applicationContext);
            this.directoryWatcher.addListener(pluginManagerListener);

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
                configureDirectoryWatcher(this.directoryWatcher, dir.getAbsolutePath());
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
                                this.directoryWatcher.addWatchFile(resolvedPath);
                            }
                            else if (wp.getDirectory() != null && wp.getExtension() != null) {
                                String relativePath = wp.getDirectory().getCanonicalPath().substring(
                                        wp.getDirectory().getCanonicalPath().indexOf(baseDirPath) + baseDirPath.length());
                                File resolvedPath = new File(watchBase, relativePath);
                                this.directoryWatcher.addWatchDirectory(resolvedPath, wp.getExtension());
                            }
                        }
                    }
                }
            }

            this.developmentModeActive = true;
            new Thread(() -> {
                CompilerConfiguration compilerConfig = new CompilerConfiguration();
                compilerConfig.setTargetDirectory(new File(location, BuildSettings.BUILD_CLASSES_PATH));

                while (isDevelopmentModeActive()) {
                    // Workaround for some IDE / OS combos - 2 events (new + update) for the same file
                    Set<File> uniqueChangedFiles = new HashSet<>(Arrays.asList(changedFiles.toArray(new File[0])));

                    int count = uniqueChangedFiles.size();
                    try {
                        if (count > 0) {
                            changedFiles.clear();
                            for (File changedFile : uniqueChangedFiles) {
                                logger.debug(String.format("WatchService found file changed [%s]",
                                        GrailsResourceUtils.getPathFromBaseDir(changedFile.getAbsolutePath())));

                                changedFile = changedFile.getCanonicalFile();

                                String relativeName = getRelativeName(BuildSettings.BASE_DIR, changedFile);
                                if (isRestartRequired(relativeName, allExclude)) {
                                    recompile(changedFile, compilerConfig, location);
                                    newFiles.remove(changedFile);
                                    pluginManager.informOfFileChange(changedFile);
                                }
                                else {
                                    pluginManager.informOfFileChange(changedFile);
                                }
                            }
                        }

                        newFiles.clear();
                    }
                    catch (CompilationFailedException | IOException cfe) {
                        logger.error(String.format("Compilation Error: %s", cfe.getMessage()), cfe);
                    }

                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
            this.directoryWatcher.start();
        }
    }

    private boolean isDevelopmentModeActive() {
        return this.developmentModeActive;
    }

    private void setDevelopmentModeActive(boolean active) {
        this.developmentModeActive = active;
        if (this.directoryWatcher != null) {
            this.directoryWatcher.setActive(active);
        }
    }

    private void recompile(File changedFile, CompilerConfiguration compilerConfig, String location) {
        if (!(changedFile.getName().endsWith(".java") || changedFile.getName().endsWith(".groovy"))) {
            return;
        }

        String grailsAppPath = BuildSettings.GRAILS_APP_PATH;
        String changedPath = changedFile.getPath();

        File appDir = null;
        boolean sourceFileChanged = false;
        for (String dir : Arrays.asList(grailsAppPath, SOURCE_MAIN_JAVA, SOURCE_MAIN_GROOVY)) {
            String changedDir = File.separator + dir;
            if (changedPath.contains(changedDir)) {
                appDir = new File(changedPath.substring(0, changedPath.indexOf(changedDir)));
                sourceFileChanged = true;
                break;
            }
        }

        if (!sourceFileChanged) {
            return;
        }

        String baseFileLocation = appDir.getAbsolutePath();
        compilerConfig.setTargetDirectory(new File(baseFileLocation, BuildSettings.BUILD_CLASSES_PATH));

        logger.debug(String.format("Recompiling changed file... [%s]",
                GrailsResourceUtils.getPathFromBaseDir(changedFile.getAbsolutePath())));

        if (changedFile.getName().endsWith(".java")) {
            if (JavaCompiler.isAvailable()) {
                JavaCompiler.recompile(compilerConfig, changedFile);
            }
            else {
                logger.error(String.format("Cannot recompile [%s], " +
                                "the current JVM is not a JDK (recompilation will not work on a JRE missing the compiler APIs).",
                        changedFile.getName()));
            }
        }
        else {
            compileGroovyFile(compilerConfig, changedFile);
        }
    }

    private void compileGroovyFile(CompilerConfiguration compilerConfig, File changedFile) {
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
     *
     * @param applicationContext - The running {@link org.springframework.context.ApplicationContext}
     * @return {@link DirectoryWatcher.FileChangeListener}
     */
    private static DirectoryWatcher.FileChangeListener createPluginManagerListener(ConfigurableApplicationContext applicationContext) {
        final GrailsPluginManager pluginManager = applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);

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

    private void configureDirectoryWatcher(DirectoryWatcher directoryWatcher, String location) {
        String grailsAppPath = BuildSettings.GRAILS_APP_PATH;
        for (String dir : Arrays.asList(grailsAppPath, SOURCE_MAIN_JAVA, SOURCE_MAIN_GROOVY)) {
            directoryWatcher.addWatchDirectory(new File(location, dir), FILE_EXTENSIONS);
        }
    }

    private List<String> getRestartExcludes(GrailsApplication grailsApplication) {
        List<String> allExclude = new ArrayList<>();
        Config config = grailsApplication.getConfig();
        String[] exclude = config.getProperty("spring.devtools.restart.exclude", String[].class);
        if (exclude != null) {
            allExclude.addAll(Arrays.asList(exclude));
        }
        else {
            allExclude.addAll(Arrays.asList(DEFAULT_RESTART_EXCLUDES));
        }
        String[] additionalExclude = config.getProperty("spring.devtools.restart.additional-exclude", String[].class);
        allExclude.addAll(Arrays.asList(additionalExclude));
        return allExclude;
    }

    private boolean isRestartRequired(String relativeName, List<String> allExclude) {
        for (String pattern : allExclude) {
            if (this.matcher.match(pattern, relativeName)) {
                return false;
            }
        }
        return true;
    }

    private String getRelativeName(File baseDir, File changedFile) {
        File directory = baseDir.getAbsoluteFile();
        File file = changedFile.getAbsoluteFile();
        String directoryName = StringUtils.cleanPath(directory.getPath());
        String fileName = StringUtils.cleanPath(file.getPath());
        return fileName.substring(directoryName.length() + 1);
    }

}
