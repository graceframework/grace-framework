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
package org.grails.web.filters;

import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.core.Ordered;

/**
 * {@link HiddenHttpMethodFilter} that also implements {@link Ordered}.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class OrderedHiddenHttpMethodFilter extends HiddenHttpMethodFilter implements OrderedFilter {

    /**
     * The default order is high to ensure the filter is applied before Spring Security.
     */
    public static final int DEFAULT_ORDER = REQUEST_WRAPPER_FILTER_MAX_ORDER - 10000;

    private int order = DEFAULT_ORDER;

    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * Set the order for this filter.
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

}
