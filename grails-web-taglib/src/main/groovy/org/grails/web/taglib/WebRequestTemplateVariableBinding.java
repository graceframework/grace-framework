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
package org.grails.web.taglib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.Binding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import grails.util.Environment;

import org.grails.taglib.AbstractTemplateVariableBinding;
import org.grails.web.servlet.mvc.GrailsWebRequest;

/**
 * Script binding to be used as the top-level binding in GSP evaluation.
 *
 * @author Lari Hotari
 */
public class WebRequestTemplateVariableBinding extends AbstractTemplateVariableBinding {

    private static final Log log = LogFactory.getLog(WebRequestTemplateVariableBinding.class);

    private final GrailsWebRequest webRequest;

    private final boolean developmentMode = Environment.isDevelopmentMode();

    private final Set<String> requestAttributeVariables = new HashSet<>();

    private static final Map<String, LazyRequestBasedValue> lazyRequestBasedValuesMap = new HashMap<>();

    static {
        Map<String, LazyRequestBasedValue> m = lazyRequestBasedValuesMap;
        m.put("webRequest", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest;
            }
        });
        m.put("request", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getCurrentRequest();
            }
        });
        m.put("response", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getCurrentResponse();
            }
        });
        m.put("flash", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getFlashScope();
            }
        });
        m.put("application", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getServletContext();
            }
        });
        m.put("applicationContext", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getApplicationContext();
            }
        });
        m.put("grailsApplication", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getAttributes().getGrailsApplication();
            }
        });
        m.put("session", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getSession();
            }
        });
        m.put("params", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getParams();
            }
        });
        m.put("actionName", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getActionName();
            }
        });
        m.put("controllerName", new LazyRequestBasedValue() {
            public Object evaluate(GrailsWebRequest webRequest) {
                return webRequest.getControllerName();
            }
        });
    }

    public WebRequestTemplateVariableBinding(GrailsWebRequest webRequest) {
        this.webRequest = webRequest;
    }

    public Binding findBindingForVariable(String name) {
        Binding binding = super.findBindingForVariable(name);
        if (binding == null) {
            if (this.webRequest.getCurrentRequest().getAttribute(name) != null) {
                this.requestAttributeVariables.add(name);
                binding = this;
            }
        }
        if (binding == null && lazyRequestBasedValuesMap.containsKey(name)) {
            binding = this;
        }
        return binding;
    }

    public boolean isRequestAttributeVariable(String name) {
        return this.requestAttributeVariables.contains(name);
    }

    public boolean isVariableCachingAllowed(String name) {
        return !isRequestAttributeVariable(name);
    }

    @Override
    public Object getVariable(String name) {
        Object val = getVariablesMap().get(name);
        if (val == null && !getVariablesMap().containsKey(name) && this.webRequest != null) {
            val = this.webRequest.getCurrentRequest().getAttribute(name);
            if (val != null) {
                this.requestAttributeVariables.add(name);
            }
            else {
                LazyRequestBasedValue lazyValue = lazyRequestBasedValuesMap.get(name);
                if (lazyValue != null) {
                    val = lazyValue.evaluate(this.webRequest);
                }
                else {
                    val = resolveMissingVariable(name);
                }

                // warn about missing variables in development mode
                if (val == null && this.developmentMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Variable '" + name + "' not found in binding or the value is null.");
                    }
                }
            }
        }
        return val;
    }

    protected Object resolveMissingVariable(String name) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getVariableNames() {
        if (getVariablesMap().isEmpty()) {
            return lazyRequestBasedValuesMap.keySet();
        }

        Set<String> variableNames = new HashSet<>(lazyRequestBasedValuesMap.keySet());
        variableNames.addAll(getVariablesMap().keySet());
        return variableNames;
    }

    private interface LazyRequestBasedValue {

        Object evaluate(GrailsWebRequest webRequest);

    }

}
