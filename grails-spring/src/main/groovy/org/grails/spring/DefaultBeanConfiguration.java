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
package org.grails.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import groovy.lang.GroovyObjectSupport;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Default implementation of the BeanConfiguration interface .
 *
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 *
 * @author Graeme
 * @author Michael Yan
 * @since 0.3
 */
public class DefaultBeanConfiguration extends GroovyObjectSupport implements BeanConfiguration {

    private static final String AUTOWIRE = "autowire";

    private static final String SINGLETON = "singleton";

    private static final String CONSTRUCTOR_ARGS = "constructorArgs";

    private static final String DESTROY_METHOD = "destroyMethod";

    private static final String FACTORY_BEAN = "factoryBean";

    private static final String FACTORY_METHOD = "factoryMethod";

    private static final String INIT_METHOD = "initMethod";

    private static final String BY_NAME = "byName";

    private static final String PARENT = "parent";

    private static final String BY_TYPE = "byType";

    private static final String BY_CONSTRUCTOR = "constructor";

    private static final String ROLE = "role";

    private static final String ROLE_APPLICATION = "application";

    private static final String ROLE_SUPPORT = "support";

    private static final String ROLE_INFRASTRUCTURE = "infrastructure";

    private static final List<String> DYNAMIC_PROPS = Arrays.asList(
            AUTOWIRE,
            CONSTRUCTOR_ARGS,
            DESTROY_METHOD,
            FACTORY_BEAN,
            FACTORY_METHOD,
            INIT_METHOD,
            BY_NAME,
            BY_TYPE,
            BY_CONSTRUCTOR);

    private Class<?> clazz;

    private String name;

    private boolean singleton = true;

    private AbstractBeanDefinition definition;

    private Resource resource;

    private boolean condition = true;

    private Collection<?> constructorArgs = Collections.emptyList();

    private BeanWrapper wrapper;

    private String parentName;

    public DefaultBeanConfiguration(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public DefaultBeanConfiguration(String name, Class<?> clazz, boolean prototype) {
        this(name, clazz, Collections.emptyList());
        this.singleton = !prototype;
    }

    public DefaultBeanConfiguration(String name) {
        this(name, null, Collections.emptyList());
    }

    public DefaultBeanConfiguration(Class<?> clazz2) {
        this.clazz = clazz2;
    }

    public DefaultBeanConfiguration(String name2, Class<?> clazz2, Collection<?> args) {
        this.name = name2;
        this.clazz = clazz2;
        this.constructorArgs = args;
    }

    public DefaultBeanConfiguration(String name2, boolean prototype) {
        this(name2, null, Collections.emptyList());
        this.singleton = !prototype;
    }

    public DefaultBeanConfiguration(Class<?> clazz2, Collection<?> constructorArguments) {
        this.clazz = clazz2;
        this.constructorArgs = constructorArguments;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Object getProperty(String property) {
        @SuppressWarnings("unused")
        AbstractBeanDefinition bd = getBeanDefinition();
        if (this.wrapper.isReadableProperty(property)) {
            return this.wrapper.getPropertyValue(property);
        }
        if (DYNAMIC_PROPS.contains(property)) {
            return null;
        }
        return super.getProperty(property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        if (PARENT.equals(property)) {
            setParent(newValue);
            return;
        }

        AbstractBeanDefinition bd = getBeanDefinition();
        if (AUTOWIRE.equals(property)) {
            if (BY_NAME.equals(newValue)) {
                bd.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
            }
            else if (BY_TYPE.equals(newValue)) {
                bd.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
            }
            else if (Boolean.TRUE.equals(newValue)) {
                bd.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
            }
            else if (BY_CONSTRUCTOR.equals(newValue)) {
                bd.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
            }
        }
        // role
        else if (ROLE.equals(property)) {
            if (ROLE_APPLICATION.equals(newValue) || Integer.valueOf(BeanDefinition.ROLE_APPLICATION).equals(newValue)) {
                bd.setRole(BeanDefinition.ROLE_APPLICATION);
            }
            else if (ROLE_SUPPORT.equals(newValue) || Integer.valueOf(BeanDefinition.ROLE_SUPPORT).equals(newValue)) {
                bd.setRole(BeanDefinition.ROLE_SUPPORT);
            }
            else if (ROLE_INFRASTRUCTURE.equals(newValue) || Integer.valueOf(BeanDefinition.ROLE_INFRASTRUCTURE).equals(newValue)) {
                bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            }
        }
        // constructorArgs
        else if (CONSTRUCTOR_ARGS.equals(property) && newValue instanceof List<?>) {
            ConstructorArgumentValues cav = new ConstructorArgumentValues();
            for (Object e : (List<?>) newValue) {
                cav.addGenericArgumentValue(e);
            }
            bd.setConstructorArgumentValues(cav);
        }
        // destroyMethod
        else if (DESTROY_METHOD.equals(property)) {
            if (newValue != null) {
                bd.setDestroyMethodName(newValue.toString());
            }
        }
        // factoryBean
        else if (FACTORY_BEAN.equals(property)) {
            if (newValue != null) {
                bd.setFactoryBeanName(newValue.toString());
            }
        }
        // factoryMethod
        else if (FACTORY_METHOD.equals(property)) {
            if (newValue != null) {
                bd.setFactoryMethodName(newValue.toString());
            }
        }
        // initMethod
        else if (INIT_METHOD.equals(property)) {
            if (newValue != null) {
                bd.setInitMethodName(newValue.toString());
            }
        }
        // singleton property
        else if (SINGLETON.equals(property)) {
            bd.setScope(Boolean.TRUE.equals(newValue) ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
        }
        else if (this.wrapper.isWritableProperty(property)) {
            this.wrapper.setPropertyValue(property, newValue);
        }
        // autowire
        else {
            super.setProperty(property, newValue);
        }
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public AbstractBeanDefinition getBeanDefinition() {
        if (this.definition == null) {
            this.definition = createBeanDefinition();
        }
        else if (this.definition.getResource() == null) {
            this.definition.setResource(this.resource);
        }
        return this.definition;
    }

    public void setBeanDefinition(BeanDefinition definition) {
        this.definition = (AbstractBeanDefinition) definition;
    }

    protected AbstractBeanDefinition createBeanDefinition() {
        AbstractBeanDefinition bd = new GenericBeanDefinition();
        if (!this.constructorArgs.isEmpty()) {
            ConstructorArgumentValues cav = new ConstructorArgumentValues();
            for (Object constructorArg : this.constructorArgs) {
                cav.addGenericArgumentValue(constructorArg);
            }
            bd.setConstructorArgumentValues(cav);
        }
        if (this.clazz != null) {
            Lazy lazy = this.clazz.getAnnotation(Lazy.class);
            if (lazy != null) {
                bd.setLazyInit(lazy.value());
            }
            Role role = this.clazz.getAnnotation(Role.class);
            if (role != null) {
                bd.setRole(role.value());
            }
            bd.setBeanClass(this.clazz);
        }
        bd.setScope(this.singleton ? AbstractBeanDefinition.SCOPE_SINGLETON : AbstractBeanDefinition.SCOPE_PROTOTYPE);
        if (this.parentName != null) {
            bd.setParentName(this.parentName);
        }
        if (this.resource != null) {
            bd.setResource(this.resource);
        }
        this.wrapper = new BeanWrapperImpl(bd);
        return bd;
    }

    public BeanConfiguration addProperty(String propertyName, Object propertyValue) {
        if (propertyValue instanceof BeanConfiguration) {
            propertyValue = ((BeanConfiguration) propertyValue).getBeanDefinition();
        }
        getBeanDefinition()
                .getPropertyValues()
                .addPropertyValue(propertyName, propertyValue);

        return this;
    }

    public BeanConfiguration setDestroyMethod(String methodName) {
        getBeanDefinition().setDestroyMethodName(methodName);
        return this;
    }

    public BeanConfiguration setDependsOn(String[] dependsOn) {
        getBeanDefinition().setDependsOn(dependsOn);
        return this;
    }

    public BeanConfiguration setFactoryBean(String beanName) {
        getBeanDefinition().setFactoryBeanName(beanName);
        return this;
    }

    public BeanConfiguration setFactoryMethod(String methodName) {
        getBeanDefinition().setFactoryMethodName(methodName);
        return this;
    }

    public BeanConfiguration setAutowire(String type) {
        if ("byName".equals(type)) {
            getBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        }
        else if ("byType".equals(type)) {
            getBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        }
        return this;
    }

    public void setName(String beanName) {
        this.name = beanName;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return this.resource;
    }

    public Object getPropertyValue(String propName) {
        PropertyValue propertyValue = getBeanDefinition()
                .getPropertyValues()
                .getPropertyValue(propName);
        if (propertyValue == null) {
            return null;
        }

        return propertyValue.getValue();
    }

    public boolean hasProperty(String propName) {
        return getBeanDefinition().getPropertyValues().contains(propName);
    }

    public void setPropertyValue(String property, Object newValue) {
        getBeanDefinition().getPropertyValues().addPropertyValue(property, newValue);
    }

    public BeanConfiguration setAbstract(boolean isAbstract) {
        getBeanDefinition().setAbstract(isAbstract);
        return this;
    }

    public void setParent(Object obj) {
        Assert.notNull(obj, "Parent bean cannot be set to a null runtime bean reference!");

        if (obj instanceof String) {
            this.parentName = (String) obj;
        }
        else if (obj instanceof RuntimeBeanReference) {
            this.parentName = ((RuntimeBeanReference) obj).getBeanName();
        }
        else if (obj instanceof BeanConfiguration) {
            this.parentName = ((BeanConfiguration) obj).getName();
        }
        getBeanDefinition().setParentName(this.parentName);
        setAbstract(false);
    }

    public boolean isConditionOn() {
        return this.condition;
    }

}
