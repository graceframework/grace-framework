/*
 * Copyright 2021-2022 the original author or authors.
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
package org.grails.plugins;

import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsClass;

/**
 * An {@link grails.core.ArtefactHandler} that identifies the Grails Plugin class
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsPluginArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "GrailsPlugin";
    private static final GrailsPluginArtefactHandler INSTANCE = new GrailsPluginArtefactHandler();

    public GrailsPluginArtefactHandler() {
        super(TYPE, GrailsClass.class, GrailsPluginClass.class,
                GrailsPluginClass.GRAILS_PLUGIN, false);
    }

    public static boolean isGrailsPlugin(Class<?> clazz) {
        return INSTANCE.isArtefactClass(clazz);
    }

}
