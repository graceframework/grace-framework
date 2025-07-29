/*
 * Copyright 2011-2022 the original author or authors.
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
package grails.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for a value inside a cache that adds timestamp information
 * for expiration and prevents "cache storms" with a Lock.
 *
 * JMM happens-before is ensured with AtomicReference.
 *
 * Objects in cache are assumed to not change after publication.
 *
 * @author Lari Hotari
 * @since 2.3.4
 */
public class CacheEntry<V> {

    private static final Logger logger = LoggerFactory.getLogger(CacheEntry.class);

    private final AtomicReference<V> valueRef = new AtomicReference<>(null);

    private long createdMillis;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = this.lock.readLock();

    private final Lock writeLock = this.lock.writeLock();

    private volatile boolean initialized = false;

    public CacheEntry() {
        expire();
    }

    public CacheEntry(V value) {
        setValue(value);
    }

    /**
     * Gets a value from cache. If the key doesn't exist, it will create the value using the updater callback
     * Prevents cache storms with a lock
     * The key is always added to the cache. Null return values will also be cached.
     * You can use this together with ConcurrentLinkedHashMap to create a bounded LRU cache
     *
     * @param map the cache map
     * @param key the key to look up
     * @param timeoutMillis cache entry timeout
     * @param updater callback to create/update value
     * @param cacheEntryFactory callback to get CacheEntry
     * @param returnExpiredWhileUpdating when true, return expired value while updating new value
     * @param cacheRequestObject context object that gets passed to hasExpired,
     *                           shouldUpdate and updateValue methods, not used in default implementation
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> V getValue(ConcurrentMap<K, CacheEntry<V>> map, K key, long timeoutMillis,
            Callable<V> updater, Callable<? extends CacheEntry> cacheEntryFactory,
            boolean returnExpiredWhileUpdating, Object cacheRequestObject) {

        CacheEntry<V> cacheEntry = map.get(key);
        if (cacheEntry == null) {
            try {
                cacheEntry = cacheEntryFactory.call();
            }
            catch (Exception e) {
                throw new UpdateException(e);
            }
            CacheEntry<V> previousEntry = map.putIfAbsent(key, cacheEntry);
            if (previousEntry != null) {
                cacheEntry = previousEntry;
            }
        }

        try {
            return cacheEntry.getValue(timeoutMillis, updater, returnExpiredWhileUpdating, cacheRequestObject);
        }
        catch (UpdateException e) {
            e.rethrowRuntimeException();
            // make compiler happy
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final Callable<CacheEntry> DEFAULT_CACHE_ENTRY_FACTORY = CacheEntry::new;

    public static <K, V> V getValue(ConcurrentMap<K, CacheEntry<V>> map, K key, long timeoutMillis, Callable<V> updater) {
        return getValue(map, key, timeoutMillis, updater, DEFAULT_CACHE_ENTRY_FACTORY, true, null);
    }

    public static <K, V> V getValue(ConcurrentMap<K, CacheEntry<V>> map, K key, long timeoutMillis,
            Callable<V> updater, boolean returnExpiredWhileUpdating) {
        return getValue(map, key, timeoutMillis, updater, DEFAULT_CACHE_ENTRY_FACTORY, returnExpiredWhileUpdating, null);
    }

    public V getValue(long timeout, Callable<V> updater) {
        return getValue(timeout, updater, true, null);
    }

    /**
     * gets the current value from the entry and updates it if it's older than timeout
     *
     * updater is a callback for creating an updated value.
     *
     * @param timeout
     * @param updater
     * @param returnExpiredWhileUpdating
     * @param cacheRequestObject
     * @return the current value
     */
    public V getValue(long timeout, Callable<V> updater, boolean returnExpiredWhileUpdating, Object cacheRequestObject) {
        if (!isInitialized() || hasExpired(timeout, cacheRequestObject)) {
            boolean lockAcquired = false;
            try {
                long beforeLockingCreatedMillis = this.createdMillis;
                if (returnExpiredWhileUpdating) {
                    if (!this.writeLock.tryLock()) {
                        if (isInitialized()) {
                            return getValueWhileUpdating(cacheRequestObject);
                        }
                        else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Locking cache for update");
                            }
                            this.writeLock.lock();
                        }
                    }
                }
                else {
                    logger.debug("Locking cache for update");
                    this.writeLock.lock();
                }

                lockAcquired = true;
                V value;
                if (!isInitialized() || shouldUpdate(beforeLockingCreatedMillis, cacheRequestObject)) {
                    try {
                        value = updateValue(getValue(), updater, cacheRequestObject);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Updating cache for value [{}]", value);
                        }
                        setValue(value);
                    }
                    catch (Exception e) {
                        throw new UpdateException(e);
                    }
                }
                else {
                    value = getValue();
                    resetTimestamp(false);
                }
                return value;
            }
            finally {
                if (lockAcquired) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unlocking cache for update");
                    }
                    this.writeLock.unlock();
                }
            }
        }
        else {
            return getValue();
        }
    }

    protected V getValueWhileUpdating(Object cacheRequestObject) {
        return this.valueRef.get();
    }

    protected V updateValue(V oldValue, Callable<V> updater, Object cacheRequestObject) throws Exception {
        return updater != null ? updater.call() : oldValue;
    }

    public V getValue() {
        try {
            this.readLock.lock();
            return this.valueRef.get();
        }
        finally {
            this.readLock.unlock();
        }
    }

    public void setValue(V val) {
        try {
            this.writeLock.lock();
            this.valueRef.set(val);
            setInitialized(true);
            resetTimestamp(true);
        }
        finally {
            this.writeLock.unlock();
        }
    }

    protected boolean hasExpired(long timeout, Object cacheRequestObject) {
        return timeout >= 0 && System.currentTimeMillis() - timeout > this.createdMillis;
    }

    protected boolean shouldUpdate(long beforeLockingCreatedMillis, Object cacheRequestObject) {
        return beforeLockingCreatedMillis == this.createdMillis || this.createdMillis == 0L;
    }

    protected void resetTimestamp(boolean updated) {
        if (updated) {
            this.createdMillis = System.currentTimeMillis();
        }
    }

    public long getCreatedMillis() {
        return this.createdMillis;
    }

    public void expire() {
        this.createdMillis = 0L;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public static final class UpdateException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public UpdateException(String message, Throwable cause) {
            super(message, cause);
        }

        public UpdateException(Throwable cause) {
            super(cause);
        }

        public void rethrowCause() throws Exception {
            if (getCause() instanceof Exception) {
                throw (Exception) getCause();
            }

            throw this;
        }

        public void rethrowRuntimeException() {
            if (getCause() instanceof RuntimeException) {
                throw (RuntimeException) getCause();
            }
            throw this;
        }

    }

}
