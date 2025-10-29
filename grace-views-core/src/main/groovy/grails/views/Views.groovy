/*
 * Copyright 2015-2025 the original author or authors.
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
package grails.views

/**
 * Settings and constants for the Groovy view infrastructure
 *
 * @author Graeme Rocher
 * @since 2024.0.0
 */
interface Views {

    /**
     * The artefact type identifier for Grails
     */
    String TYPE = 'views'

    /**
     * The identifier used for model types
     */
    String MODEL = 'model'

    /**
     * The identifier used for model types
     */
    String MODEL_TYPES = 'modelTypes'

    /**
     * Field used to hold the model types
     */
    String MODEL_TYPES_FIELD = 'MODEL_TYPES'

}
