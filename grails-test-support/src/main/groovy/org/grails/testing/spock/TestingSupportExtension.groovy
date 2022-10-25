package org.grails.testing.spock

import grails.testing.spock.OnceBefore
import grails.testing.spring.AutowiredTest
import groovy.transform.CompileStatic
import org.grails.testing.GrailsUnitTest
import org.junit.After
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.MethodKind
import org.spockframework.runtime.model.SpecInfo

import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@CompileStatic
class TestingSupportExtension extends AbstractGlobalExtension {

    AutowiredInterceptor autowiredInterceptor = new AutowiredInterceptor()
    CleanupContextInterceptor cleanupContextInterceptor = new CleanupContextInterceptor()

    @Override
    void visitSpec(SpecInfo spec) {
        if (AutowiredTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupInterceptor(autowiredInterceptor)
        }
        if (GrailsUnitTest.isAssignableFrom(spec.reflection)) {
            spec.addCleanupSpecInterceptor(cleanupContextInterceptor)
        }
        for (Method method : spec.getReflection().getDeclaredMethods()) {
            if (method.isAnnotationPresent(BeforeEach.class)) {
                spec.addSetupMethod(createJUnitFixtureMethod(spec, method, MethodKind.SETUP, BeforeEach.class));
            }
        }
    }

    private MethodInfo createMethod(SpecInfo specInfo, Method method, MethodKind kind, String name) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setParent(specInfo);
        methodInfo.setName(name);
        methodInfo.setReflection(method);
        methodInfo.setKind(kind);
        return methodInfo;
    }

    private MethodInfo createJUnitFixtureMethod(SpecInfo specInfo, Method method, MethodKind kind, Class<? extends Annotation> annotation) {
        MethodInfo methodInfo = createMethod(specInfo, method, kind, method.getName());
        methodInfo.setExcluded(isOverriddenJUnitFixtureMethod(specInfo, method, annotation));
        return methodInfo;
    }
    private boolean isOverriddenJUnitFixtureMethod(SpecInfo specInfo, Method method, Class<? extends Annotation> annotation) {
        if (Modifier.isPrivate(method.getModifiers())) return false;

        for (Class<?> currClass = specInfo.class; currClass != specInfo.class.superclass; currClass = currClass.getSuperclass()) {
            for (Method currMethod : currClass.getDeclaredMethods()) {
                if (!currMethod.isAnnotationPresent(annotation)) continue;
                if (!currMethod.getName().equals(method.getName())) continue;
                if (!Arrays.deepEquals(currMethod.getParameterTypes(), method.getParameterTypes())) continue;
                return true;
            }
        }

        return false;
    }
}
