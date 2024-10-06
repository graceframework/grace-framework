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
package grails.plugin.hibernate;

import java.beans.Introspector;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import grails.config.Config;
import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.datastore.gorm.events.ConfigurableApplicationContextEventPublisher;
import org.grails.datastore.gorm.proxy.ProxyHandlerAdapter;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.services.Service;
import org.grails.orm.hibernate.HibernateDatastore;
import org.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.grails.plugin.hibernate.support.AggregatePersistenceContextInterceptor;
import org.grails.plugin.hibernate.support.GrailsOpenSessionInViewInterceptor;
import org.grails.transaction.ChainedTransactionManagerPostProcessor;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for GORM for Hibernate
 *
 * @author Michael Yan
 * @since 2023.1
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class,
        before = { HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@AutoConfigureOrder
@ConditionalOnClass(HibernateDatastore.class)
public class HibernateGormAutoConfiguration {

    private static final String TRANSACTION_MANAGER_WHITE_LIST_PATTERN = "grails.transaction.chainedTransactionManager.whitelistPattern";
    private static final String TRANSACTION_MANAGER_BLACK_LIST_PATTERN = "grails.transaction.chainedTransactionManager.blacklistPattern";

    private final ConfigurableApplicationContext applicationContext;
    private final ObjectProvider<GrailsApplication> grailsApplication;

    public HibernateGormAutoConfiguration(ApplicationContext applicationContext,
            ObjectProvider<GrailsApplication> grailsApplication) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        this.grailsApplication = grailsApplication;
    }

    @Bean
    @ConditionalOnMissingBean
    public HibernateDatastore hibernateDatastore(ObjectProvider<DataSource> dataSource) {
        GrailsClass[] grailsClasses = this.grailsApplication.getObject().getArtefacts(DomainClassArtefactHandler.TYPE);
        Set<Class<?>> domainClasses = new HashSet<>();
        for (GrailsClass grailsClass : grailsClasses) {
            if (grailsClass.getClazz() != null) {
                domainClasses.add(grailsClass.getClazz());
            }
        }

        HibernateDatastore datastore;
        if (dataSource.getIfAvailable() != null) {
            datastore = new HibernateDatastore(
                    dataSource.getObject(),
                    this.applicationContext.getEnvironment(),
                    new ConfigurableApplicationContextEventPublisher(this.applicationContext),
                    domainClasses.toArray(new Class[0])
            );
        }
        else {
            datastore = new HibernateDatastore(
                    this.applicationContext.getEnvironment(),
                    new ConfigurableApplicationContextEventPublisher(this.applicationContext),
                    domainClasses.toArray(new Class[0])
            );
        }

        for (Service<?> service : datastore.getServices()) {
            Class<?> serviceClass = service.getClass();
            grails.gorm.services.Service ann = serviceClass.getAnnotation(grails.gorm.services.Service.class);
            String serviceName;
            if (ann == null) {
                serviceName = Introspector.decapitalize(serviceClass.getSimpleName());
            }
            else {
                serviceName = ann.name();
            }
            if (!this.applicationContext.containsBean(serviceName)) {
                this.applicationContext.getBeanFactory().registerSingleton(
                        serviceName,
                        service
                );
            }
        }
        return datastore;
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionFactory sessionFactory(HibernateDatastore hibernateDatastore) {
        return hibernateDatastore.getSessionFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public MappingContext grailsDomainClassMappingContext(HibernateDatastore hibernateDatastore) {
        return hibernateDatastore.getMappingContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public AggregatePersistenceContextInterceptor persistenceInterceptor(HibernateDatastore hibernateDatastore) {
        return new AggregatePersistenceContextInterceptor(hibernateDatastore);
    }

    @Bean
    @ConditionalOnMissingBean
    public HibernateProxyHandler hibernateProxyHandler() {
        return new HibernateProxyHandler();
    }

    @Bean
    @Order(10)
    @ConditionalOnMissingBean
    public ProxyHandlerAdapter proxyHandler(HibernateProxyHandler hibernateProxyHandler) {
        return new ProxyHandlerAdapter(hibernateProxyHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hibernate.osiv.enabled", havingValue = "true", matchIfMissing = true)
    public GrailsOpenSessionInViewInterceptor openSessionInViewInterceptor(HibernateDatastore hibernateDatastore) {
        GrailsOpenSessionInViewInterceptor openSessionInViewInterceptor = new GrailsOpenSessionInViewInterceptor();
        openSessionInViewInterceptor.setHibernateDatastore(hibernateDatastore);
        return openSessionInViewInterceptor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "grails.transaction.chainedTransactionManager", name = "enabled", havingValue = "true")
    public ChainedTransactionManagerPostProcessor chainedTransactionManagerPostProcessor() {
        Config config = this.grailsApplication.getObject().getConfig();
        String whitelistPattern = config.getProperty(TRANSACTION_MANAGER_WHITE_LIST_PATTERN, "");
        String blacklistPattern = config.getProperty(TRANSACTION_MANAGER_BLACK_LIST_PATTERN, "");
        return new ChainedTransactionManagerPostProcessor(config, whitelistPattern, blacklistPattern);
    }

}
