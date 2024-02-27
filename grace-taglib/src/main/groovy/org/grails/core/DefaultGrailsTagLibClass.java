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
package org.grails.core;

import grails.core.GrailsTagLibClass;

import org.grails.core.artefact.gsp.TagLibArtefactHandler;

/**
 * Default implementation of a tag lib class.
 *
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.core.gsp.DefaultGrailsTagLibClass} instead
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public abstract class DefaultGrailsTagLibClass extends AbstractInjectableGrailsClass implements GrailsTagLibClass {

    public DefaultGrailsTagLibClass(Class<?> clazz) {
        super(clazz, TagLibArtefactHandler.TYPE);
    }

}
