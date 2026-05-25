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

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.change.DatabaseChange;
import liquibase.integration.spring.Customizer;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseConnectionDetails;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import grails.core.GrailsApplication;

import org.grails.plugins.databasemigration.liquibase.DataSourceClosingGrailsLiquibase;
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibase;
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibaseCustomizer;

/**
 * @author Michael Yan
 * @since 2024.1.0
 */
@AutoConfiguration(before = LiquibaseAutoConfiguration.class)
@ConditionalOnClass({ SpringLiquibase.class, DatabaseChange.class })
@ConditionalOnBooleanProperty(name = "grails.plugin.databasemigration.enabled", matchIfMissing = true)
@Conditional(DatabaseMigrationAutoConfiguration.LiquibaseDataSourceCondition.class)
public class DatabaseMigrationAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ConnectionCallback.class)
    @ConditionalOnMissingBean(SpringLiquibase.class)
    @EnableConfigurationProperties({ LiquibaseProperties.class, DatabaseMigrationProperties.class})
    public static class LiquibaseConfiguration {

        @Bean
        @ConditionalOnMissingBean(LiquibaseConnectionDetails.class)
        PropertiesLiquibaseConnectionDetails liquibaseConnectionDetails(LiquibaseProperties properties) {
            return new PropertiesLiquibaseConnectionDetails(properties);
        }

        @Bean
        public SpringLiquibase liquibase(ObjectProvider<GrailsApplication> grailsApplicationObjectProvider, ObjectProvider<DataSource> dataSource,
                @LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource, LiquibaseProperties properties, DatabaseMigrationProperties  databaseMigrationProperties,
                ObjectProvider<GrailsLiquibaseCustomizer> customizers, LiquibaseConnectionDetails connectionDetails) {
            GrailsApplication grailsApplication = grailsApplicationObjectProvider.getIfAvailable();
            Assert.notNull(grailsApplication, "grailsApplication must not be null");

            GrailsLiquibase liquibase = createGrailsLiquibase(grailsApplication, liquibaseDataSource.getIfAvailable(),
                    dataSource.getIfUnique(), connectionDetails);
            liquibase.setDropFirst(databaseMigrationProperties.isDropOnStart());
            liquibase.setShouldRun(databaseMigrationProperties.isUpdateOnStart());
            liquibase.setChangeLog(databaseMigrationProperties.getUpdateOnStartFileName());
            liquibase.setContexts(StringUtils.collectionToCommaDelimitedString(databaseMigrationProperties.getUpdateOnStartContexts()));
            liquibase.setDefaultSchema(databaseMigrationProperties.getUpdateOnStartDefaultSchema());
            liquibase.setDatabaseChangeLogTableName(databaseMigrationProperties.getDatabaseChangeLogTableName());
            liquibase.setDatabaseChangeLogLockTableName(databaseMigrationProperties.getDatabaseChangeLogLockTableName());
            liquibase.setDataSourceName(PluginConstants.DEFAULT_DATASOURCE_NAME);

            customizers.orderedStream().forEach((customizer) -> customizer.customize(liquibase));
            return liquibase;
        }

        private GrailsLiquibase createGrailsLiquibase(GrailsApplication grailsApplication, DataSource liquibaseDataSource, DataSource dataSource,
                LiquibaseConnectionDetails connectionDetails) {
            DataSource migrationDataSource = getMigrationDataSource(liquibaseDataSource, dataSource, connectionDetails);
            GrailsLiquibase liquibase = (migrationDataSource == liquibaseDataSource
                    || migrationDataSource == dataSource) ? new GrailsLiquibase(grailsApplication.getMainContext())
                    : new DataSourceClosingGrailsLiquibase(grailsApplication.getMainContext());
            liquibase.setDataSource(migrationDataSource);
            return liquibase;
        }

        private DataSource getMigrationDataSource(DataSource liquibaseDataSource, DataSource dataSource,
                LiquibaseConnectionDetails connectionDetails) {
            if (liquibaseDataSource != null) {
                return liquibaseDataSource;
            }
            String url = connectionDetails.getJdbcUrl();
            if (url != null) {
                DataSourceBuilder<?> builder = DataSourceBuilder.create().type(SimpleDriverDataSource.class);
                builder.url(url);
                applyConnectionDetails(connectionDetails, builder);
                return builder.build();
            }
            String user = connectionDetails.getUsername();
            if (user != null && dataSource != null) {
                DataSourceBuilder<?> builder = DataSourceBuilder.derivedFrom(dataSource)
                        .type(SimpleDriverDataSource.class);
                applyConnectionDetails(connectionDetails, builder);
                return builder.build();
            }
            Assert.state(dataSource != null, "Liquibase migration DataSource missing");
            return dataSource;
        }

        private void applyConnectionDetails(LiquibaseConnectionDetails connectionDetails,
                DataSourceBuilder<?> builder) {
            builder.username(connectionDetails.getUsername());
            builder.password(connectionDetails.getPassword());
            String driverClassName = connectionDetails.getDriverClassName();
            if (StringUtils.hasText(driverClassName)) {
                builder.driverClassName(driverClassName);
            }
        }

    }

    @ConditionalOnClass(Customizer.class)
    @Configuration(proxyBeanMethods = false)
    static class CustomizerConfiguration {

        @Bean
        @ConditionalOnBean(Customizer.class)
        GrailsLiquibaseCustomizer grailsLiquibaseCustomizer(Customizer<Liquibase> customizer) {
            return (grailsLiquibase) -> grailsLiquibase.setCustomizer(customizer);
        }

    }

    static final class LiquibaseDataSourceCondition extends AnyNestedCondition {

        LiquibaseDataSourceCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(DataSource.class)
        private static final class DataSourceBeanCondition {

        }

        @ConditionalOnBean(JdbcConnectionDetails.class)
        private static final class JdbcConnectionDetailsCondition {

        }

        @ConditionalOnProperty("dataSource.url")
        private static final class DataSourceUrlCondition {

        }

    }

    /**
     * Adapts {@link LiquibaseProperties} to {@link LiquibaseConnectionDetails}.
     */
    static final class PropertiesLiquibaseConnectionDetails implements LiquibaseConnectionDetails {

        private final LiquibaseProperties properties;

        PropertiesLiquibaseConnectionDetails(LiquibaseProperties properties) {
            this.properties = properties;
        }

        @Override
        public String getUsername() {
            return this.properties.getUser();
        }

        @Override
        public String getPassword() {
            return this.properties.getPassword();
        }

        @Override
        public String getJdbcUrl() {
            return this.properties.getUrl();
        }

        @Override
        public String getDriverClassName() {
            String driverClassName = this.properties.getDriverClassName();
            return (driverClassName != null) ? driverClassName : LiquibaseConnectionDetails.super.getDriverClassName();
        }

    }

}
