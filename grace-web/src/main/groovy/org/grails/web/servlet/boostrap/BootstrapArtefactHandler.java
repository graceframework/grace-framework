/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.web.servlet.boostrap;

import grails.core.ArtefactHandlerAdapter;
import grails.web.servlet.bootstrap.GrailsBootstrapClass;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public class BootstrapArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Bootstrap";

    public BootstrapArtefactHandler() {
        super(TYPE, GrailsBootstrapClass.class, DefaultGrailsBootstrapClass.class,
                DefaultGrailsBootstrapClass.BOOT_STRAP);
    }

}