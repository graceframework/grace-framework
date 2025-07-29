package org.grails.boot.context.configwarnings.real;

import org.springframework.context.annotation.Configuration;

import grails.boot.annotation.GrailsComponentScan;

import org.grails.boot.context.configwarnings.dflt.InDefaultPackageConfiguration;
import org.grails.boot.context.configwarnings.orggrails.InOrgGrailsPackageConfiguration;

@Configuration(proxyBeanMethods = false)
@GrailsComponentScan(basePackageClasses = { InDefaultPackageConfiguration.class, InOrgGrailsPackageConfiguration.class })
public class InRealButScanningProblemPackages {

}
