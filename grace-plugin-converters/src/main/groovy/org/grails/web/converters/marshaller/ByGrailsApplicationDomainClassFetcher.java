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
package org.grails.web.converters.marshaller;

import grails.core.GrailsApplication;

import org.grails.datastore.mapping.model.PersistentEntity;

public class ByGrailsApplicationDomainClassFetcher implements DomainClassFetcher {

    GrailsApplication grailsApplication;

    public ByGrailsApplicationDomainClassFetcher(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    public PersistentEntity findDomainClass(Object instance) {
        return this.grailsApplication.getMappingContext().getPersistentEntity(instance.getClass().getName());
    }

}