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
package org.grails.web.converters.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import grails.core.support.proxy.DefaultProxyHandler;
import grails.core.support.proxy.ProxyHandler;
import grails.util.Environment;

import org.grails.web.converters.Converter;
import org.grails.web.converters.exceptions.ConverterException;
import org.grails.web.converters.marshaller.ObjectMarshaller;

/**
 * An immutable ConverterConfiguration which chains the lookup calls for ObjectMarshallers
 * for performance reasons.
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 *
 * @since 1.1
 */
@SuppressWarnings("rawtypes")
public class ChainedConverterConfiguration<C extends Converter> implements ConverterConfiguration<C> {

    private final List<ObjectMarshaller<C>> marshallerList;

    private final ChainedObjectMarshaller<C> root;

    private final String encoding;

    private final Converter.CircularReferenceBehaviour circularReferenceBehaviour;

    private final boolean prettyPrint;

    private final ProxyHandler proxyHandler;

    private final boolean cacheObjectMarshallerByClass;

    private Map<Integer, ObjectMarshaller<C>> objectMarshallerForClassCache;

    private final boolean developmentMode = Environment.isDevelopmentMode();

    private final ObjectMarshaller<C> NULL_HOLDER = new ObjectMarshaller<C>() {

        public boolean supports(Object object) {
            return false;
        }

        public void marshalObject(Object object, C converter) throws ConverterException {
        }

    };

    public ChainedConverterConfiguration(ConverterConfiguration<C> cfg) {
        this(cfg, new DefaultProxyHandler());
    }

    public ChainedConverterConfiguration(ConverterConfiguration<C> cfg, ProxyHandler proxyHandler) {
        this.marshallerList = cfg.getOrderedObjectMarshallers();
        this.proxyHandler = proxyHandler;

        this.encoding = cfg.getEncoding();
        this.prettyPrint = cfg.isPrettyPrint();
        this.cacheObjectMarshallerByClass = cfg.isCacheObjectMarshallerByClass();
        if (this.cacheObjectMarshallerByClass) {
            this.objectMarshallerForClassCache = new ConcurrentHashMap<>();
        }
        this.circularReferenceBehaviour = cfg.getCircularReferenceBehaviour();

        List<ObjectMarshaller<C>> oms = new ArrayList<>(this.marshallerList);
        Collections.reverse(oms);
        ChainedObjectMarshaller<C> prev = null;
        for (ObjectMarshaller<C> om : oms) {
            prev = new ChainedObjectMarshaller<C>(om, prev);
        }
        this.root = prev;
    }

    public ObjectMarshaller<C> getMarshaller(Object o) {
        ObjectMarshaller<C> marshaller = null;

        Integer cacheKey = null;
        if (!this.developmentMode && this.cacheObjectMarshallerByClass && o != null) {
            cacheKey = System.identityHashCode(o.getClass());
            marshaller = this.objectMarshallerForClassCache.get(cacheKey);
            if (marshaller != this.NULL_HOLDER && marshaller != null && !marshaller.supports(o)) {
                marshaller = null;
            }
        }
        if (marshaller == null) {
            marshaller = this.root.findMarhallerFor(o);
            if (cacheKey != null) {
                this.objectMarshallerForClassCache.put(cacheKey, marshaller != null ? marshaller : this.NULL_HOLDER);
            }
        }
        return marshaller != this.NULL_HOLDER ? marshaller : null;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public Converter.CircularReferenceBehaviour getCircularReferenceBehaviour() {
        return this.circularReferenceBehaviour;
    }

    public boolean isPrettyPrint() {
        return this.prettyPrint;
    }

    public List<ObjectMarshaller<C>> getOrderedObjectMarshallers() {
        return this.marshallerList;
    }

    public ProxyHandler getProxyHandler() {
        return this.proxyHandler;
    }

    public boolean isCacheObjectMarshallerByClass() {
        return this.cacheObjectMarshallerByClass;
    }

    @SuppressWarnings("hiding")
    public class ChainedObjectMarshaller<C extends Converter> implements ObjectMarshaller<C> {

        private final ObjectMarshaller<C> om;

        private final ChainedObjectMarshaller<C> next;

        public ChainedObjectMarshaller(ObjectMarshaller<C> om, ChainedObjectMarshaller<C> next) {
            this.om = om;
            this.next = next;
        }

        public ObjectMarshaller<C> findMarhallerFor(Object o) {
            if (supports(o)) {
                return this.om;
            }

            return this.next != null ? this.next.findMarhallerFor(o) : null;
        }

        public boolean supports(Object object) {
            return this.om.supports(object);
        }

        public void marshalObject(Object object, C converter) throws ConverterException {
            this.om.marshalObject(object, converter);
        }

    }

}
