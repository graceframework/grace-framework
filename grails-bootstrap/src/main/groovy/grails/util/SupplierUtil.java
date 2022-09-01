/*
 * Copyright 2020-2022 the original author or authors.
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
package grails.util;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper methods for dealing with {@link Supplier}.
 *
 * @author James Kleeh
 * @since 1.0
 */
public final class SupplierUtil {

    private SupplierUtil() {
    }

    /**
     * Caches the result of supplier in a thread safe manner.
     *
     * @param actual The supplier providing the result
     * @param <T> The type of result
     * @return A new supplier that will cache the result
     */
    public static <T> Supplier<T> memoized(Supplier<T> actual) {
        return new Supplier<T>() {
            Supplier<T> delegate = this::initialize;

            boolean initialized;

            public T get() {
                return this.delegate.get();
            }

            private synchronized T initialize() {
                if (!this.initialized) {
                    T value = actual.get();
                    this.delegate = () -> value;
                    this.initialized = true;
                }
                return this.delegate.get();
            }
        };
    }

    /**
     * Caches the result of supplier in a thread safe manner. The result
     * is only cached if it is non null or non empty if an optional.
     *
     * @param actual The supplier providing the result
     * @param <T> The type of result
     * @return A new supplier that will cache the result
     */
    public static <T> Supplier<T> memoizedNonEmpty(Supplier<T> actual) {
        return new Supplier<T>() {
            Supplier<T> delegate = this::initialize;

            boolean initialized;

            public T get() {
                return this.delegate.get();
            }

            private synchronized T initialize() {
                if (!this.initialized) {
                    T value = actual.get();
                    if (value == null) {
                        return null;
                    }
                    if (value instanceof Optional) {
                        if (!((Optional) value).isPresent()) {
                            return value;
                        }
                    }
                    this.delegate = () -> value;
                    this.initialized = true;
                }
                return this.delegate.get();
            }
        };
    }
}
