/*
 * Copyright 2024 the original author or authors.
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
package org.grails.plugins.datasource;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import grails.config.Config;
import grails.core.GrailsApplication;
import org.grails.datastore.gorm.jdbc.connections.DataSourceSettings;
import org.grails.datastore.mapping.core.connections.ConnectionSources;
import org.grails.plugins.datasource.DataSourcePluginConfiguration.GrailsDataSourceCondition;

/**
 * {@link EnableAutoConfiguration Auto-configure} for DataSource Plugin
 *
 * @author Michael Yan
 * @since 2023.1.0
 */
@AutoConfiguration(before = {
        DataSourceAutoConfiguration.class, SqlInitializationAutoConfiguration.class
})
@AutoConfigureOrder(100)
@Conditional(GrailsDataSourceCondition.class)
public class DataSourcePluginConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConnectionSources<DataSource, DataSourceSettings> dataSourceConnectionSources(
            ObjectProvider<GrailsApplication> grailsApplication) throws Exception {
        Config config = grailsApplication.getObject().getConfig();

        Map dataSources = config.getProperty("dataSources", Map.class, new HashMap<>());
        if (dataSources != null && dataSources.isEmpty()) {
            Map defaultDataSource = config.getProperty("dataSource", Map.class);
            if (defaultDataSource != null) {
                dataSources.put("dataSource", defaultDataSource);
            }
        }

        return new DataSourceConnectionSourcesFactoryBean(config).getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(ConnectionSources<DataSource, DataSourceSettings> dataSourceConnectionSources) {
        return dataSourceConnectionSources.getDefaultConnectionSource().getSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public EmbeddedDatabaseShutdownHook embeddedDatabaseShutdownHook() {
        return new EmbeddedDatabaseShutdownHook();
    }

    static final class GrailsDataSourceCondition extends AnyNestedCondition {

        GrailsDataSourceCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = "dataSource", name = "url")
        private static final class DataSourceUrlCondition {

        }

        @ConditionalOnProperty(prefix = "dataSources", name = "default")
        private static final class DataSourcesCondition {

        }

    }

}
