/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.plugins.databasemigration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Michael Yan
 * @since 2024.1.0
 */
@ConfigurationProperties(prefix = DatabaseMigrationProperties.CONFIG_PREFIX, ignoreUnknownFields = false)
public class DatabaseMigrationProperties {

    public static final String CONFIG_PREFIX = "grails.plugin.databasemigration";

    private Boolean enabled = true;

    private Boolean updateAllOnStart = false;

    private List<Class<?>> skipUpdateOnStartMainClasses = new ArrayList<>();

    private Boolean updateOnStart = false;

    private Boolean dropOnStart = false;

    private String updateOnStartFileName = "db/migrations/changelog.groovy";

    private List<String> updateOnStartContexts = new ArrayList<>();

    private List<String> updateOnStartLabels = new ArrayList<>();

    private String updateOnStartDefaultSchema;

    private String databaseChangeLogTableName;

    private String databaseChangeLogLockTableName;

    public Boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isUpdateAllOnStart() {
        return this.updateAllOnStart;
    }

    public void setUpdateAllOnStart(Boolean updateAllOnStart) {
        this.updateAllOnStart = updateAllOnStart;
    }

    public List<Class<?>> getSkipUpdateOnStartMainClasses() {
        return this.skipUpdateOnStartMainClasses;
    }

    public void setSkipUpdateOnStartMainClasses(List<Class<?>> skipUpdateOnStartMainClasses) {
        this.skipUpdateOnStartMainClasses = skipUpdateOnStartMainClasses;
    }

    public Boolean isUpdateOnStart() {
        return this.updateOnStart;
    }

    public void setUpdateOnStart(Boolean updateOnStart) {
        this.updateOnStart = updateOnStart;
    }

    public Boolean isDropOnStart() {
        return this.dropOnStart;
    }

    public void setDropOnStart(Boolean dropOnStart) {
        this.dropOnStart = dropOnStart;
    }

    public String getUpdateOnStartFileName() {
        return this.updateOnStartFileName;
    }

    public void setUpdateOnStartFileName(String updateOnStartFileName) {
        this.updateOnStartFileName = updateOnStartFileName;
    }

    public List<String> getUpdateOnStartContexts() {
        return this.updateOnStartContexts;
    }

    public void setUpdateOnStartContexts(List<String> updateOnStartContexts) {
        this.updateOnStartContexts = updateOnStartContexts;
    }

    public List<String> getUpdateOnStartLabels() {
        return this.updateOnStartLabels;
    }

    public void setUpdateOnStartLabels(List<String> updateOnStartLabels) {
        this.updateOnStartLabels = updateOnStartLabels;
    }

    public String getUpdateOnStartDefaultSchema() {
        return this.updateOnStartDefaultSchema;
    }

    public void setUpdateOnStartDefaultSchema(String updateOnStartDefaultSchema) {
        this.updateOnStartDefaultSchema = updateOnStartDefaultSchema;
    }

    public String getDatabaseChangeLogTableName() {
        return this.databaseChangeLogTableName;
    }

    public void setDatabaseChangeLogTableName(String databaseChangeLogTableName) {
        this.databaseChangeLogTableName = databaseChangeLogTableName;
    }

    public String getDatabaseChangeLogLockTableName() {
        return this.databaseChangeLogLockTableName;
    }

    public void setDatabaseChangeLogLockTableName(String databaseChangeLogLockTableName) {
        this.databaseChangeLogLockTableName = databaseChangeLogLockTableName;
    }

}
