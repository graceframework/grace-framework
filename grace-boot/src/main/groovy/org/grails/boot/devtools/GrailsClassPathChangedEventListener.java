/*
 * Copyright 2022-2025 the original author or authors.
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
package org.grails.boot.devtools;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.context.ApplicationListener;

import grails.plugins.GrailsPlugin;
import grails.plugins.GrailsPluginManager;
import grails.plugins.PluginManagerAware;
import grails.util.BuildSettings;

import org.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.BinaryGrailsPlugin;

/**
 * {@link ApplicationListener} to handle {@link org.springframework.boot.devtools.classpath.ClassPathChangedEvent}.
 *
 * @author Michael Yan
 * @since 2024.0.0
 * @see ClassPathChangedEvent
 */
public class GrailsClassPathChangedEventListener implements PluginManagerAware, ApplicationListener<ClassPathChangedEvent> {

    private static final Log logger = LogFactory.getLog(GrailsClassPathChangedEventListener.class);

    private GrailsPluginManager pluginManager;

    private final GrailsSourceCompiler grailsSourceCompiler = new GrailsSourceCompiler();

    @Override
    public void onApplicationEvent(ClassPathChangedEvent event) {
        if (event.isRestartRequired()) {
            return;
        }

        for (ChangedFiles changedFiles : event.getChangeSet()) {
            for (ChangedFile changedFile : changedFiles) {
                String changedFilePath = changedFile.getFile().getAbsolutePath();
                ChangedFile.Type type = changedFile.getType();

                if ((type == ChangedFile.Type.MODIFY) && (isInGrailsAppDir(BuildSettings.BASE_DIR, changedFilePath)
                        || isInGrailsPluginDir(changedFilePath))) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("DevTools found file changed [%s]",
                                GrailsResourceUtils.getPathFromBaseDir(changedFile.getFile().getAbsolutePath())));
                    }
                    this.pluginManager.informOfFileChange(changedFile.getFile());
                }
                else {
                    this.grailsSourceCompiler.compile(changedFile.getFile());
                }
            }
        }
    }

    private boolean isInGrailsPluginDir(String changedFilePath) {
        for (GrailsPlugin plugin : this.pluginManager.getAllPlugins()) {
            if (plugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryGrailsPlugin = (BinaryGrailsPlugin) plugin;
                File pluginDirectory = binaryGrailsPlugin.getProjectDirectory();
                if (isInGrailsAppDir(pluginDirectory, changedFilePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInGrailsAppDir(File baseDir, String changedFilePath) {
        if (baseDir != null) {
            String confPath = new File(baseDir, BuildSettings.GRAILS_APP_PATH + File.separator + "conf").getAbsolutePath();
            String i18nPath = new File(baseDir, BuildSettings.GRAILS_APP_PATH + File.separator + "i18n").getAbsolutePath();
            return changedFilePath.startsWith(confPath) || changedFilePath.startsWith(i18nPath);
        }
        return false;
    }

    @Override
    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

}
