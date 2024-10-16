/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.plugins.datasource

import javax.management.MalformedObjectNameException
import javax.management.ObjectName

import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.jmx.export.MBeanExporter
import org.springframework.jmx.support.RegistrationPolicy

import grails.core.GrailsApplication

@CompileStatic
class TomcatJDBCPoolMBeanExporter extends MBeanExporter {

    private static final Log log = LogFactory.getLog(TomcatJDBCPoolMBeanExporter)
    GrailsApplication grailsApplication
    private ListableBeanFactory beanFactory

    TomcatJDBCPoolMBeanExporter() {
        super()
        this.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING)
    }

    @Override
    protected void registerBeans() {
        Map<String, org.apache.tomcat.jdbc.pool.DataSource> dataSourceBeans = beanFactory.getBeansOfType(org.apache.tomcat.jdbc.pool.DataSource)
        for (Map.Entry<String, org.apache.tomcat.jdbc.pool.DataSource> entry : dataSourceBeans.entrySet()) {
            boolean jmxEnabled = false
            try {
                jmxEnabled = isJmxEnabled(entry.key, entry.value)
            }
            catch (Exception e) {
                log.warn("Unable to access dataSource bean ${entry.key}", e)
            }
            if (jmxEnabled) {
                ObjectName objectName = null
                try {
                    objectName = createJmxObjectName(entry.key, entry.value)
                    doRegister(entry.value.pool.jmxPool, objectName)
                }
                catch (Exception e) {
                    log.warn("Unable to register JMX MBean for ${objectName} beanName:${entry.key}", e)
                }
            }
        }
    }

    protected boolean isJmxEnabled(String beanName, org.apache.tomcat.jdbc.pool.DataSource dataSource) {
        dataSource.createPool().poolProperties.jmxEnabled
    }

    protected ObjectName createJmxObjectName(String beanName, org.apache.tomcat.jdbc.pool.DataSource dataSource)
            throws MalformedObjectNameException {
        Hashtable<String, String> properties = new Hashtable<String, String>()
        properties.type = 'ConnectionPool'
        properties.application = ((grailsApplication?.getMetadata()?.getApplicationName()) ?: 'grailsApplication')
                .replaceAll(/[,=;:]/, '_')
        String poolName = dataSource.pool.poolProperties.name

        if (beanName.startsWith('dataSourceUnproxied')) {
            def dataSourceName = beanName - ~/^dataSourceUnproxied_?/
            dataSourceName = dataSourceName ?: 'default'
            properties.dataSource = dataSourceName
        }
        else {
            if (poolName.startsWith('Tomcat Connection Pool[')) {
                // use bean name if the pool has a default name
                poolName = beanName
            }
        }
        if (!poolName.startsWith('Tomcat Connection Pool[')) {
            properties.pool = poolName
        }

        new ObjectName('grails.dataSource', properties)
    }

    @Override
    void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory)
        this.beanFactory = (ListableBeanFactory) beanFactory
    }

}
