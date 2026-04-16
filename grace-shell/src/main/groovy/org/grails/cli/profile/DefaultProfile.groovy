/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.cli.profile

import org.springframework.core.io.DescriptiveResource
import org.springframework.core.io.Resource

/**
 * Default profile used when no profile configured.
 *
 * @author Michael Yan
 * @since 2024.1
 */
class DefaultProfile extends AbstractProfile {

    static final String PROFILE_NAME = 'Default Profile'
    static final Resource PROFILE_RESOURCE = new DescriptiveResource(PROFILE_NAME) {

        @Override
        URL getURL() throws IOException {
            return null
        }

        @Override
        Resource createRelative(String relativePath) throws IOException {
            return null
        }

    }

    DefaultProfile() {
        super(PROFILE_RESOURCE)
        this.name = PROFILE_NAME
    }

}
