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
package org.grails.plugins.databasemigration.liquibase;

import java.lang.reflect.Method;

import liquibase.exception.LiquibaseException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael Yan
 * @since 2024.1.0
 */
public class DataSourceClosingGrailsLiquibase extends GrailsLiquibase implements DisposableBean {

    private volatile boolean closeDataSourceOnceMigrated = true;

    public DataSourceClosingGrailsLiquibase(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public void setCloseDataSourceOnceMigrated(boolean closeDataSourceOnceMigrated) {
        this.closeDataSourceOnceMigrated = closeDataSourceOnceMigrated;
    }

    @Override
    public void afterPropertiesSet() throws LiquibaseException {
        super.afterPropertiesSet();
        if (this.closeDataSourceOnceMigrated) {
            closeDataSource();
        }
    }

    private void closeDataSource() {
        Class<?> dataSourceClass = getDataSource().getClass();
        Method closeMethod = ReflectionUtils.findMethod(dataSourceClass, "close");
        if (closeMethod != null) {
            ReflectionUtils.invokeMethod(closeMethod, getDataSource());
        }
    }

    @Override
    public void destroy() throws Exception {
        if (!this.closeDataSourceOnceMigrated) {
            closeDataSource();
        }
    }

}
