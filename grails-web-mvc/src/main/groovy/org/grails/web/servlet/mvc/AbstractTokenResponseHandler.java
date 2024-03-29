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
package org.grails.web.servlet.mvc;

import groovy.lang.Closure;

/**
 * Implements the behavior of wasInvoked in the TokenResponseHandler interface.
 *
 * @see TokenResponseHandler#wasInvoked()
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public abstract class AbstractTokenResponseHandler implements TokenResponseHandler {

    private boolean invoked = false;

    private final boolean valid;

    public AbstractTokenResponseHandler(boolean valid) {
        this.valid = valid;
    }

    public final Object invalidToken(@SuppressWarnings("rawtypes") Closure callable) {
        this.invoked = true;
        return invalidTokenInternal(callable);
    }

    protected abstract Object invalidTokenInternal(@SuppressWarnings("rawtypes") Closure callable);

    public boolean wasInvoked() {
        return this.invoked;
    }

    public boolean wasInvalidToken() {
        return !this.valid;
    }

}
