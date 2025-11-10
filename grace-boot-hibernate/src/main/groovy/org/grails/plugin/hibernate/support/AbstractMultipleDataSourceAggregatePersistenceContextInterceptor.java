/*
 * Copyright 2013-2025 the original author or authors.
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
package org.grails.plugin.hibernate.support;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;

import grails.persistence.support.PersistenceContextInterceptor;

import org.grails.datastore.mapping.core.connections.ConnectionSource;
import org.grails.datastore.mapping.core.connections.ConnectionSources;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.connections.HibernateConnectionSourceSettings;

/**
 * Abstract implementation of the {@link grails.persistence.support.PersistenceContextInterceptor} interface that supports multiple data sources
 *
 * @author Graeme Rocher
 * @since 2.0.7
 */
public abstract class AbstractMultipleDataSourceAggregatePersistenceContextInterceptor implements PersistenceContextInterceptor {

    protected final List<PersistenceContextInterceptor> interceptors = new ArrayList<>();

    protected final AbstractHibernateDatastore hibernateDatastore;

    public AbstractMultipleDataSourceAggregatePersistenceContextInterceptor(AbstractHibernateDatastore hibernateDatastore) {
        this.hibernateDatastore = hibernateDatastore;
        ConnectionSources<SessionFactory, HibernateConnectionSourceSettings> connectionSources = hibernateDatastore.getConnectionSources();
        Iterable<ConnectionSource<SessionFactory, HibernateConnectionSourceSettings>> allConnectionSources =
                connectionSources.getAllConnectionSources();

        for (ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> connectionSource : allConnectionSources) {
            SessionFactoryAwarePersistenceContextInterceptor interceptor = createPersistenceContextInterceptor(connectionSource.getName());
            this.interceptors.add(interceptor);
        }
    }

    @Override
    public boolean isOpen() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            if (interceptor.isOpen()) {
                // true at least one is true
                return true;
            }
        }
        return false;
    }

    @Override
    public void reconnect() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.reconnect();
        }
    }

    @Override
    public void destroy() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            try {
                if (interceptor.isOpen()) {
                    interceptor.destroy();
                }
            }
            catch (Exception ignored) {
            }
        }
    }

    @Override
    public void clear() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.clear();
        }
    }

    @Override
    public void disconnect() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.disconnect();
        }
    }

    @Override
    public void flush() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.flush();
        }
    }

    @Override
    public void init() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.init();
        }
    }

    @Override
    public void setReadOnly() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.setReadOnly();
        }
    }

    @Override
    public void setReadWrite() {
        for (PersistenceContextInterceptor interceptor : this.interceptors) {
            interceptor.setReadWrite();
        }
    }

    protected abstract SessionFactoryAwarePersistenceContextInterceptor createPersistenceContextInterceptor(String dataSourceName);

}
