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
package org.grails.buffer;

import java.io.IOException;
import java.io.Writer;

public class GrailsLazyProxyPrintWriter extends GrailsPrintWriter {

    private DestinationFactory factory;

    private boolean destinationActivated = false;

    public GrailsLazyProxyPrintWriter(DestinationFactory factory) {
        super(null);
        this.factory = factory;
    }

    @Override
    public Writer getOut() {
        if (!this.destinationActivated) {
            try {
                super.setOut(this.factory.activateDestination());
            }
            catch (IOException e) {
                setError();
            }
            this.destinationActivated = true;
        }
        return super.getOut();
    }

    @Override
    public boolean isAllowUnwrappingOut() {
        return this.destinationActivated ? super.isAllowUnwrappingOut() : false;
    }

    @Override
    public Writer unwrap() {
        return this.destinationActivated ? super.unwrap() : this;
    }

    public void updateDestination(DestinationFactory f) {
        setDestinationActivated(false);
        this.factory = f;
    }

    @Override
    public boolean isDestinationActivated() {
        return this.destinationActivated;
    }

    public void setDestinationActivated(boolean destinationActivated) {
        this.destinationActivated = destinationActivated;
        if (!this.destinationActivated) {
            super.setOut(null);
        }
    }

    /**
     * Factory to lazily instantiate the destination.
     */
    public static interface DestinationFactory {

        Writer activateDestination() throws IOException;

    }

}
