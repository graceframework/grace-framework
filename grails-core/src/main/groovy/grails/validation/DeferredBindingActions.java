/*
 * Copyright 2011-2022 the original author or authors.
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
package grails.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.grails.core.lifecycle.ShutdownOperations;

/**
 * Binding operations that are deferred until either validate() or save() are called.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public final class DeferredBindingActions {

    private static final Log logger = LogFactory.getLog(DeferredBindingActions.class);

    private static ThreadLocal<List<Runnable>> deferredBindingActions = new ThreadLocal<>();

    static {
        ShutdownOperations.addOperation(() -> deferredBindingActions = new ThreadLocal<>(), true);
    }

    private DeferredBindingActions() {
    }

    public static void addBindingAction(Runnable runnable) {
        List<Runnable> bindingActions = getDeferredBindingActions();
        bindingActions.add(runnable);
    }

    private static List<Runnable> getDeferredBindingActions() {
        List<Runnable> runnables = deferredBindingActions.get();
        if (runnables == null) {
            runnables = new ArrayList<>();
            deferredBindingActions.set(runnables);
        }
        return runnables;
    }

    public static void runActions() {
        List<Runnable> runnables = deferredBindingActions.get();
        if (runnables != null) {
            try {
                for (Runnable runnable : getDeferredBindingActions()) {
                    if (runnable != null) {
                        try {
                            runnable.run();
                        }
                        catch (Exception e) {
                            logger.error("Error running deferred data binding: " + e.getMessage(), e);
                        }
                    }
                }
            }
            finally {
                clear();
            }
        }
    }

    public static void clear() {
        deferredBindingActions.remove();
    }

}
