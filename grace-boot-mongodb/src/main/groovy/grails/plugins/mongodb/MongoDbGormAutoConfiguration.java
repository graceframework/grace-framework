/*
 * Copyright 2025-2026 the original author or authors.
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
package grails.plugins.mongodb;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import com.mongodb.client.MongoClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

import grails.artefact.ArtefactTypes;
import grails.boot.config.GrailsComponentScanner;
import grails.core.GrailsApplication;
import grails.core.GrailsClass;

import org.grails.compiler.gorm.GormEntityTraitProvider;
import org.grails.datastore.gorm.events.AutoTimestampEventListener;
import org.grails.datastore.gorm.events.ConfigurableApplicationContextEventPublisher;
import org.grails.datastore.gorm.plugin.support.PersistenceContextInterceptorAggregator;
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.mongo.MongoDatastore;
import org.grails.datastore.mapping.mongo.config.MongoMappingContext;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.grails.datastore.mapping.services.Service;
import org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-Configure} for GORM for MongoDB
 *
 * @author Michael Yan
 * @since 2023.3
 */
@AutoConfigureOrder(300)
@AutoConfiguration(after = MongoAutoConfiguration.class)
@ConditionalOnMissingBean(MongoDatastore.class)
public class MongoDbGormAutoConfiguration implements ApplicationContextAware {

    public static final String DATASTORE_TYPE = "mongo";

    private ConfigurableApplicationContext applicationContext;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public MongoDatastore mongoDatastore(ObjectProvider<MongoClient> mongo, ObjectProvider<GrailsApplication> grailsApplication) {
        GrailsClass[] grailsClasses = grailsApplication.getObject().getArtefacts(ArtefactTypes.DOMAIN_CLASS);
        Set<Class<?>> domainClasses = new HashSet<>();
        for (GrailsClass grailsClass : grailsClasses) {
            if (grailsClass.getClazz() != null) {
                domainClasses.add(grailsClass.getClazz());
            }
        }

        GrailsComponentScanner scanner = new GrailsComponentScanner(this.applicationContext);
        Set<Class<?>> entityClasses;
        try {
            entityClasses = scanner.scan(grails.gorm.annotation.Entity.class, grails.persistence.Entity.class);
        }
        catch (ClassNotFoundException ignored) {
            entityClasses = Collections.emptySet();
        }

        domainClasses.addAll(entityClasses);

        Set<Class<?>> mongoEntityClasses = new HashSet<>();
        List<GormEntityTraitProvider> entityTraitProviders = getEntityTraitProviders();
        if (entityTraitProviders.size() > 1) {
            for (Class<?> domainClass : domainClasses) {
                String mapWith = ClassPropertyFetcher.getStaticPropertyValue(domainClass, GormProperties.MAPPING_STRATEGY, String.class);
                if (mapWith != null && mapWith.equals(DATASTORE_TYPE)) {
                    mongoEntityClasses.add(domainClass);
                }
            }
        }
        else {
            mongoEntityClasses.addAll(domainClasses);
        }

        ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
        ConfigurableApplicationContextEventPublisher eventPublisher = new ConfigurableApplicationContextEventPublisher(this.applicationContext);
        MongoDatastore datastore;
        if (mongo.getIfAvailable() != null) {
            datastore = new MongoDatastore(mongo.getObject(), environment, eventPublisher, mongoEntityClasses.toArray(new Class[0]));
        }
        else {
            datastore = new MongoDatastore(environment, eventPublisher, mongoEntityClasses.toArray(new Class[0]));
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
    public MongoMappingContext mongoMappingContext(MongoDatastore mongoDatastore) {
        return mongoDatastore.getMappingContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public AutoTimestampEventListener mongoAutoTimestampEventListener(MongoDatastore mongoDatastore) {
        return mongoDatastore.getAutoTimestampEventListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatastorePersistenceContextInterceptor datastorePersistenceContextInterceptor(MongoDatastore mongoDatastore) {
        return new DatastorePersistenceContextInterceptor(mongoDatastore);
    }

    @Bean
    @ConditionalOnMissingBean
    public PersistenceContextInterceptorAggregator persistenceContextInterceptorAggregator() {
        return new PersistenceContextInterceptorAggregator();
    }

    @Bean({"mongoTransactionManager", "transactionManager"})
    @Primary
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(MongoDatastore mongoDatastore) {
        return mongoDatastore.getTransactionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor")
    public OpenSessionInViewInterceptor mongoOpenSessionInViewInterceptor(MongoDatastore mongoDatastore) {
        OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
        interceptor.setDatastore(mongoDatastore);
        return interceptor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (!(applicationContext instanceof ConfigurableApplicationContext)) {
            throw new IllegalArgumentException("MongoDbGormAutoConfiguration requires an instance of ConfigurableApplicationContext");
        }
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    private static List<GormEntityTraitProvider> getEntityTraitProviders() {
        ServiceLoader<GormEntityTraitProvider> serviceProviders = ServiceLoader.load(GormEntityTraitProvider.class, Thread.currentThread().getContextClassLoader());
        List<GormEntityTraitProvider> entityTraitProviders = new ArrayList<>();
        for (GormEntityTraitProvider provider : serviceProviders) {
            if (provider.isAvailable()) {
                entityTraitProviders.add(provider);
            }
        }
        return entityTraitProviders;
    }

}
