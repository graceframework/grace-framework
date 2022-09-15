package org.grails.boot.context.configwarnings.dflt;

import org.grails.boot.context.configwarnings.real.nested.ExampleBean;
import org.springframework.context.annotation.Configuration;

import grails.boot.annotation.GrailsComponentScan;

@Configuration(proxyBeanMethods = false)
@GrailsComponentScan(basePackageClasses = ExampleBean.class)
public class InDefaultPackageWithBasePackageClassesConfiguration {

}
