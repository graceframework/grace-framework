/*
 * Copyright 2022-2023 the original author or authors.
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
package org.grails.boot.context;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;

import grails.core.GrailsApplication;
import grails.util.Environment;

/**
 * {@link ApplicationContextInitializer} to print running status and app information.
 *
 * @author Michael Yan
 * @see ApplicationContextInitializer
 * @since 2022.1.6
 */
public class GrailsRunningStatusApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ApplicationReadyEvent>, Ordered {

    private static final Log logger = LogFactory.getLog(GrailsRunningStatusApplicationContextInitializer.class);

    private int order = Ordered.LOWEST_PRECEDENCE;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.addApplicationListener(this);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        SpringApplication springApplication = event.getSpringApplication();
        ConfigurableApplicationContext applicationContext = event.getApplicationContext();
        printRunStatus(springApplication, applicationContext);
    }

    protected void printRunStatus(SpringApplication springApplication, ConfigurableApplicationContext applicationContext) {
        try {
            GrailsApplication app = applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            String protocol = app.getConfig().getProperty("server.ssl.key-store") != null ? "https" : "http";
            if (applicationContext.getParent() != null) {
                applicationContext.publishEvent(
                        new ApplicationPreparedEvent(
                                springApplication,
                                new String[0],
                                (ConfigurableApplicationContext) applicationContext.getParent())
                );
            }
            String applicationName = app.getConfig().getProperty("info.app.name", "");
            String applicationVersion = app.getConfig().getProperty("info.app.version", "");
            String contextPath = app.getConfig().getProperty("server.servlet.context-path", "");
            String serverAddress = app.getConfig().getProperty("server.address", "localhost");
            int serverPort = Integer.parseInt(app.getConfig().getProperty("server.port", "8080"));
            if (applicationContext instanceof WebServerApplicationContext) {
                serverPort = ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
            }
            String hostAddress = "localhost";
            try {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (UnknownHostException e) {
                getApplicationLog(springApplication).warn("The host name could not be determined, using `localhost` as fallback");
            }
            String sb =
                    "%n----------------------------------------------------------------------------------------------" +
                    "%n        Application:   %s" +
                    "%n        Version:       %s" +
                    "%n        Environment:   %s" +
                    "%n        Local:         %s://%s:%s%s" +
                    "%n        External:      %s://%s:%s%s" +
                    "%n----------------------------------------------------------------------------------------------" + "%n";
            getApplicationLog(springApplication).info(
                    String.format(sb,
                            applicationName,
                            applicationVersion,
                            Environment.getCurrent().getName(),
                            protocol,
                            serverAddress,
                            serverPort,
                            contextPath,
                            protocol,
                            hostAddress,
                            serverPort,
                            contextPath)
            );
        }
        catch (Exception ignored) {
        }
    }

    protected Log getApplicationLog(SpringApplication springApplication) {
        Class<?> mainApplicationClass = springApplication.getMainApplicationClass();
        if (mainApplicationClass == null) {
            return logger;
        }
        return LogFactory.getLog(mainApplicationClass);
    }

}
