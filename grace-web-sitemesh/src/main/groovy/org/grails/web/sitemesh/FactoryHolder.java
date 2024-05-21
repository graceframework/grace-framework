/*
 * Copyright 2006-2022 the original author or authors.
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
package org.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Factory;

import grails.util.Holder;

/**
 * Holds a reference to the Sitemesh Factory object.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public final class FactoryHolder {

    private static Holder<Factory> holder = new Holder<>("factory");

    private FactoryHolder() {
        // static only
    }

    public static Factory getFactory() {
        Factory factory = holder.get();
        return factory;
    }

    public static Factory getSitemeshFactory() {
        return getFactory();
    }

    public static synchronized void setFactory(Factory factory) {
        holder.set(factory);
    }

}
