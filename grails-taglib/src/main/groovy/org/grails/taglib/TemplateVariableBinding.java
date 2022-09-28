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
package org.grails.taglib;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.Binding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Script Binding that is used in GSP evaluation.
 *
 * @author Lari Hotari
 */
public class TemplateVariableBinding extends AbstractTemplateVariableBinding {

    private static final Log logger = LogFactory.getLog(TemplateVariableBinding.class);

    private Binding parent;

    private Object owner;

    private Set<String> cachedParentVariableNames = new HashSet<>();

    private boolean root;

    public TemplateVariableBinding() {
        super();
    }

    public TemplateVariableBinding(Binding parent) {
        setParent(parent);
    }

    @SuppressWarnings("rawtypes")
    public TemplateVariableBinding(Map variables) {
        super(variables);
    }

    public TemplateVariableBinding(String[] args) {
        super(args);
    }

    @Override
    public Object getProperty(String property) {
        return getVariable(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getVariable(String name) {
        Object val = getVariablesMap().get(name);
        if (val == null && !getVariablesMap().containsKey(name)) {
            if ("variables".equals(name)) {
                return getVariables();
            }
            if ("metaClass".equals(name)) {
                return getMetaClass();
            }
            Binding variableBinding = findBindingForVariable(name);
            if (variableBinding != null) {
                val = variableBinding.getVariable(name);
                if (val != null) {
                    if (!(variableBinding instanceof AbstractTemplateVariableBinding)
                            || ((AbstractTemplateVariableBinding) variableBinding).isVariableCachingAllowed(name)) {
                        // cache variable in this context since parent context cannot change during usage of this context
                        getVariablesMap().put(name, val);
                        this.cachedParentVariableNames.add(name);
                    }
                }
            }
        }
        return val;
    }

    @Override
    public void setProperty(String property, Object newValue) {
        setVariable(property, newValue);
    }

    /**
     * ModifyOurScopeWithBodyTagTests breaks if variable isn't changed in the binding it exists in.
     *
     * @param name The name of the variable
     * @return The binding
     */
    public Binding findBindingForVariable(String name) {
        if (this.cachedParentVariableNames.contains(name)) {
            if (this.parent instanceof AbstractTemplateVariableBinding) {
                return ((AbstractTemplateVariableBinding) this.parent).findBindingForVariable(name);
            }
            return this.parent;
        }

        if (getVariablesMap().containsKey(name)) {
            return this;
        }

        if (this.parent instanceof AbstractTemplateVariableBinding) {
            return ((AbstractTemplateVariableBinding) this.parent).findBindingForVariable(name);
        }

        if (this.parent != null && this.parent.getVariables().containsKey(name)) {
            return this.parent;
        }

        return null;
    }

    @Override
    public void setVariable(String name, Object value) {
        internalSetVariable(null, name, value);
    }

    @SuppressWarnings("unchecked")
    private void internalSetVariable(Binding bindingToUse, String name, Object value) {
        if (!isReservedName(name)) {
            if (bindingToUse == null) {
                bindingToUse = findBindingForVariable(name);
                if (bindingToUse == null || (bindingToUse instanceof TemplateVariableBinding
                        && ((TemplateVariableBinding) bindingToUse).shouldUseChildBinding(this))) {
                    bindingToUse = this;
                }
            }
            if (bindingToUse instanceof AbstractTemplateVariableBinding) {
                ((AbstractTemplateVariableBinding) bindingToUse).getVariablesMap().put(name, value);
            }
            else {
                bindingToUse.getVariables().put(name, value);
            }

            if (bindingToUse != this && this.cachedParentVariableNames.contains(name)) {
                // maintain cached value
                getVariablesMap().put(name, value);
            }
        }
        else {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot override reserved variable '" + name + "'");
            }
        }
    }

    protected boolean isReservedName(String name) {
        return false;
    }

    protected boolean shouldUseChildBinding(TemplateVariableBinding childBinding) {
        return isRoot();
    }

    public Binding getParent() {
        return this.parent;
    }

    public void setParent(Binding parent) {
        this.parent = parent;
    }

    protected void internalSetVariable(String name, Object value) {
        internalSetVariable(this, name, value);
    }

    public Object getOwner() {
        return this.owner;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    public boolean isRoot() {
        return this.root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getVariableNames() {
        Set<String> variableNames = new HashSet<>();
        if (this.parent != null) {
            if (this.parent instanceof AbstractTemplateVariableBinding) {
                variableNames.addAll(((AbstractTemplateVariableBinding) this.parent).getVariableNames());
            }
            else {
                variableNames.addAll(this.parent.getVariables().keySet());
            }
        }
        variableNames.addAll(getVariablesMap().keySet());
        return variableNames;
    }

    @Override
    public boolean hasVariable(String name) {
        return super.hasVariable(name)
                || this.cachedParentVariableNames.contains(name)
                || (this.parent != null && this.parent.hasVariable(name));
    }

}
