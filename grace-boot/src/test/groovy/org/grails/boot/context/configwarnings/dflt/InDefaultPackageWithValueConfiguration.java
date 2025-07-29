package org.grails.boot.context.configwarnings.dflt;

import org.springframework.context.annotation.Configuration;

import grails.boot.annotation.GrailsComponentScan;

@Configuration(proxyBeanMethods = false)
@GrailsComponentScan("org.grails.boot.context.configwarnings.nested")
public class InDefaultPackageWithValueConfiguration {

}
