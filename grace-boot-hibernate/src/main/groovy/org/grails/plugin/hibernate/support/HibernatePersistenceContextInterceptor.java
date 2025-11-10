/*
 * Copyright 2016-2025 the original author or authors.
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

import java.sql.Connection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import grails.persistence.support.PersistenceContextInterceptor;
import grails.validation.DeferredBindingActions;

import org.grails.core.lifecycle.ShutdownOperations;
import org.grails.datastore.mapping.core.connections.ConnectionSource;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.support.HibernateRuntimeUtils;

/**
 * @author Graeme Rocher
 * @since 0.4
 */
public class HibernatePersistenceContextInterceptor implements PersistenceContextInterceptor, SessionFactoryAwarePersistenceContextInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(HibernatePersistenceContextInterceptor.class);

    private AbstractHibernateDatastore hibernateDatastore;

    private static ThreadLocal<Map<String, Boolean>> participate = ThreadLocal.withInitial(HashMap::new);

    private static ThreadLocal<Map<String, Integer>> nestingCount = ThreadLocal.withInitial(HashMap::new);

    private String dataSourceName;

    static {
        ShutdownOperations.addOperation(() -> {
            participate.remove();
            nestingCount.remove();
        });
    }

    private Deque<Connection> disconnected = new ConcurrentLinkedDeque<>();

    private final boolean transactionRequired;

    public HibernatePersistenceContextInterceptor() {
        this(ConnectionSource.DEFAULT);
    }

    /**
     * @param dataSourceName a name of dataSource
     */
    public HibernatePersistenceContextInterceptor(String dataSourceName) {
        this.dataSourceName = dataSourceName;
        this.transactionRequired = true;
    }

    @Override
    public void destroy() {
        DeferredBindingActions.clear();
        if (!this.disconnected.isEmpty()) {
            this.disconnected.pop();
        }
        if (getSessionFactory() == null || decNestingCount() > 0 || getParticipate()) {
            return;
        }

        // single session mode
        SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
        LOG.debug("Closing single Hibernate session in HibernatePersistenceContextInterceptor");
        try {
            this.disconnected.clear();
            SessionFactoryUtils.closeSession(holder.getSession());
        }
        catch (RuntimeException ex) {
            LOG.error("Unexpected exception on closing Hibernate Session", ex);
        }
    }

    @Override
    public void disconnect() {
        if (getSessionFactory() == null) {
            return;
        }
        try {
            this.disconnected.add(
                    getSession(false).disconnect()
            );
        }
        catch (Exception ignore) {
        }
    }

    @Override
    public void reconnect() {
        if (getSessionFactory() == null) {
            return;
        }
        Session session = getSession();
        if (!session.isConnected() && !this.disconnected.isEmpty()) {
            try {
                Connection connection = this.disconnected.peekLast();
                getSession().reconnect(connection);
            }
            catch (IllegalStateException e) {
                // cannot reconnect on different exception. ignore
                LOG.debug(e.getMessage(), e);
            }
        }
    }

    @Override
    public void flush() {
        if (getSessionFactory() == null) {
            return;
        }
        if (!getParticipate()) {
            if (!this.transactionRequired) {
                getSession().flush();
            }
            else if (TransactionSynchronizationManager.isSynchronizationActive()) {
                getSession().flush();
            }
        }
    }

    @Override
    public void clear() {
        if (getSessionFactory() == null) {
            return;
        }
        getSession().clear();
    }

    @Override
    public void setReadOnly() {
        if (getSessionFactory() == null) {
            return;
        }
        getSession().setHibernateFlushMode(FlushMode.MANUAL);
    }

    @Override
    public void setReadWrite() {
        if (getSessionFactory() == null) {
            return;
        }
        getSession().setHibernateFlushMode(FlushMode.AUTO);
    }

    @Override
    public boolean isOpen() {
        if (getSessionFactory() == null) {
            return false;
        }
        try {
            return getSession(false).isOpen();
        }
        catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public void init() {
        if (incNestingCount() > 1) {
            return;
        }
        SessionFactory sf = getSessionFactory();
        if (sf == null) {
            return;
        }
        if (TransactionSynchronizationManager.hasResource(sf)) {
            // Do not modify the Session: just set the participate flag.
            setParticipate(true);
        }
        else {
            setParticipate(false);
            LOG.debug("Opening single Hibernate session in HibernatePersistenceContextInterceptor");
            Session session = getSession();
            HibernateRuntimeUtils.enableDynamicFilterEnablerIfPresent(sf, session);
            TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
        }
    }

    private Session getSession() {
        return getSession(true);
    }

    private Session getSession(boolean allowCreate) {
        Object value = TransactionSynchronizationManager.getResource(getSessionFactory());
        if (value instanceof Session) {
            return (Session) value;
        }

        if (value instanceof SessionHolder) {
            SessionHolder sessionHolder = (SessionHolder) value;
            return sessionHolder.getSession();
        }

        if (allowCreate && this.hibernateDatastore != null) {
            return this.hibernateDatastore.openSession();
        }

        throw new IllegalStateException(
                "No Hibernate Session bound to thread, and configuration does not allow creation of non-transactional one here");
    }

    /**
     * @return the sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return this.hibernateDatastore.getSessionFactory();
    }

    public void setHibernateDatastore(AbstractHibernateDatastore hibernateDatastore) {
        this.hibernateDatastore = hibernateDatastore;
    }

    @Override
    public void setSessionFactory(SessionFactory sessionFactory) {
        // ignore
    }

    private int incNestingCount() {
        Map<String, Integer> map = nestingCount.get();
        Integer current = map.get(this.dataSourceName);
        int value = (current != null) ? current + 1 : 1;
        map.put(this.dataSourceName, value);
        return value;
    }

    private int decNestingCount() {
        Map<String, Integer> map = nestingCount.get();
        Integer current = map.get(this.dataSourceName);
        int value = (current != null) ? current - 1 : 0;
        if (value < 0) {
            value = 0;
        }
        map.put(this.dataSourceName, value);
        return value;
    }

    private void setParticipate(boolean flag) {
        Map<String, Boolean> map = participate.get();
        map.put(this.dataSourceName, flag);
    }

    private boolean getParticipate() {
        Map<String, Boolean> map = participate.get();
        Boolean ret = map.get(this.dataSourceName);
        return (ret != null) ? ret : false;
    }

}
