# <img src="media/logos/Grace.svg" width="80" height="80"> Grace Framework

[![Main branch build status](https://github.com/graceframework/grace-framework/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-framework/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework/grace-core.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework)
[![Grace Document](https://img.shields.io/badge/Grace_Document-latest-blue?style=flat&logo=asciidoctor&logoColor=E40046&labelColor=ffffff&color=f49b06)](https://graceframework.org/grace-framework/latest/)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/graceframework/grace-framework)
[![Grace on Gradle](https://img.shields.io/badge/Gradle_Plugin_Portal--blue?style=social&logo=gradle
)](https://plugins.gradle.org/u/grace)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://x.com/graceframework)

[![Java Version](https://img.shields.io/badge/Java-17-blue?style=flat&logo=openjdk&color=437291)](https://docs.oracle.com/en/java/javase/17/)
[![Groovy Version](https://img.shields.io/badge/Groovy-4.0.33-blue?logo=apachegroovy&style=flat&color=4298b8)](https://groovy-lang.org/releasenotes/groovy-4.0.html)
[![Gradle Version](https://img.shields.io/badge/Gradle-8.14.5-blue?logo=gradle&style=flat&color=1da2bd)](https://docs.gradle.org/8.14.5/release-notes.html)
[![Spring Boot Version](https://img.shields.io/badge/Spring_Boot-3.5.16-blue?logo=springboot&style=flat&color=6db33f)](https://github.com/spring-projects/spring-boot/releases/tag/v3.5.16)


[Grace Framework](https://github.com/graceframework/grace-framework) is a fork of [Grails 5.1.x](https://github.com/apache/grails-core/tree/5.1.x) that started development in early 2022, it's a powerful and open-source One-Person web framework used to build enterprise-grade [Spring Boot](https://spring.io/projects/spring-boot/) applications with the powerful [Groovy](https://groovy-lang.org/) programming language. The core framework is very extensible and there are numerous [Plugins](https://github.com/grace-plugins/) available that provide easy integration of add-on features.

## Why Grace Framework?

Grace framework inherits the excellent concepts and designs of Grails, and based on this, has undergone significant restructuring to ensure that each module is independent and decoupled, such as [Grace API](/grace-api), [Grace Boot](/grace-boot), [Grace CLI](/grace-cli), [Grace Plugin API](/grace-plugin-api), [Grace Spring](/grace-spring), [Grace Util](/grace-util). Meanwhile, in order to better focus on maintenance and upgrades, Grace also merged the previously spun-off modules, [Converters plugin](/grace-plugin-converters), [GSP](/grace-plugin-gsp), [Grace Test Support](/grace-test-support). [Grace Boot](/grace-boot/README.md), as a Spring Boot [Auto-configuration](/grace-boot/src/main/groovy/grails/boot/config/GrailsAutoConfiguration.java), it will load all other modules and plugins. Grace framework follows good modular design, and these modules can be used independently in Spring Boot applications.

Spring is the foundation for Grace framework, which is built on top of Spring Boot. To better support Spring Boot and integrate with other Spring ecosystems, Grace framework has rewritten `Plugin.doWithSpring()` using Spring Boot's [Auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html), which also reduces redundant configurations and provides significant performance improvements. Grace framework has also provided [Configuration Metadata](https://docs.spring.io/spring-boot/specification/configuration-metadata/index.html) files include in Grace plugins' jars, the files are designed to let IDE developers offer contextual help and “code completion” as users are working with application.properties or application.yaml files. So, a Grace plugin is an extended Spring Boot Starter.

It is worth mentioning that Grace framework supports all current versions of Spring Boot, including [2.7](https://github.com/graceframework/grace-framework/releases/tag/v2022.2.9), [3.0](https://github.com/graceframework/grace-framework/releases/tag/v2023.0.3), [3.1](https://github.com/graceframework/grace-framework/releases/tag/v2023.1.0), [3.2](https://github.com/graceframework/grace-framework/releases/tag/v2023.2.0), [3.3](https://github.com/graceframework/grace-framework/releases/tag/v2023.3.0), [3.4](https://github.com/graceframework/grace-framework/releases/tag/v2024.0.0), and [3.5](https://github.com/graceframework/grace-framework/releases/tag/v2024.1.0). This makes the upgrade path easier and more manageable.

Grace framework has been actively developing, bringing numerous improvements and new features, including [API](/grace-api/README.md), [Boot](/grace-boot/README.md), [Core](/grace-core/README.md), [Plugins](/grace-plugin-api/README.md), [Dynamic Modules](/grace-plugin-dynamic-modules/README.md), [GSP](/grace-plugin-gsp/README.md), [Console](/grace-console/README.md), [Shell](/grace-shell/README.md), [Views](/grace-views-core/README.md), and [Profiles](https://github.com/grace-profiles). Of course, it has also fixed a large number of legacy defects left in Grails 5&6, this makes developers happy.

You can learn more on the page [What's New in Grace Framework](https://github.com/graceframework/grace-framework/wiki/What's-New-in-Grace-Framework).

### Grace Framework vs Spring Boot

Grace and Spring Boot frameworks are excellent for building web applications, but their use depends on what you want. Generally, Grace framework may be advantageous in full-stack and monolithic applications, but Spring Boot is preferred for developing complex and microservice applications.

Grace framework is not a replacement for Spring Boot, it is a "framework of frameworks" built on top of Spring Boot. It provides its' own [Spring Boot Starters](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.custom-starter) - [Grace Boot](/grace-boot/README.md), as an [AutoConfiguration](/grace-boot/src/main/groovy/grails/boot/config/GrailsAutoConfiguration.java), it will load all other modules and plugins. Grace framework follows good modular design, and these modules can be used independently in Spring Boot applications. So, a Grace application is really a Spring Boot application. And Grace plugins also gives your applications more extended capabilities, like Dynamic Modules, Runtime Hooks, Artifacts, Scaffold Templates and Taglibs, so you can write less code and get more done. That's the secret to boosting development efficiency.

Grace framework has better developer productivity than Spring Boot. Because it follows the convention over the configuration principle, it minimizes code requirements. This enhances productivity and fosters faster app development. The framework creates faster and more functional prototypes than Spring Boot due to its simple code generation like scaffolding and CoC.

Grace framework has better support Groovy than Spring Boot. Grace framework fully embraces Groovy to enable many features that would not be possible using Java alone, including flexible and powerful [Plug-in architecture](/grace-plugin-api/README.md) and a rich [Plugin ecosystem](https://github.com/grace-plugins) and many built-in [Dynamic Module types](https://github.com/grace-plugins/grace-dynamic-modules), many [DSLs](https://docs.groovy-lang.org/latest/html/documentation/#_domain_specific_languages), [AST Transformations](https://docs.groovy-lang.org/latest/html/documentation/#_available_ast_transformations), [Trait](https://docs.groovy-lang.org/latest/html/documentation/#_traits)-based solutions, and much more.

Grace framework provides a powerful [CLI](/grace-shell/README.md) that allows you to quickly create new projects of many different types using [Application Profiles](https://github.com/grace-profiles) and [Templates](https://github.com/grace-templates) and get started easily. These are all extensible and easy to customize, you can create your own Profiles, Templates, and Commands to meet any of your needs. The learning curve for Grace framework is moderate and more straightforward than Spring Boot due to its emphasis on convention and simplicity.

[Spring Boot 4 has refactored its codebase into a more modular structure](https://spring.io/blog/2025/10/28/modularizing-spring-boot), this is the right direction for the future.
Grace framework 2024.0 and 2024.1 have been released, and have refactored a lot of the core plugins and modules. Grace's plugins will be independent, more focused, and at the same time, they will integrate better with Spring Boot's modules.

You can learn more on the page [Grace Framework vs Spring Boot](https://github.com/graceframework/grace-framework/wiki/Grace-vs-Spring-Boot).

## What's New in Grace Framework?

| Grace Version | Dependency Upgrades | New Features and Important Changes |
|---------------|---------------------|------------------------------------|
| 2024.2.x      | Spring Boot 3.5, Spring Framework 6.2, Groovy 4.0, Spock 2.4 | Improve support Hibernate 5&6, Gradle build performance |
| 2024.1.x      | Spring Boot 3.5, Spring Framework 6.2, Groovy 4.0, SiteMesh 2.7.0 | New Commands and Generators, Restructure directory structure fro all Profiles, Restore dynamic-modules, Better integration for GORM Entity, Support combined use of MongoDB and Hibernate |
| 2024.0.x      | Spring Boot 3.4, Spring Framework 6.2, Groovy 4.0, SiteMesh 2.6.2 | Modularize codebase, Migrate to Central Portal OSSRH Staging API, Core Plugins Merged into the Framework, Decouple GORM from Grace framework, Restore dynamic-modules |
| 2023.3.x      | Spring Boot 3.3, Spring Framework 6.1, Groovy 4.0 | Refactoring `grace-docs` and Gradle Guide Publish plugin. Grace Data MongoDB. Many improvements for CLI, Support Groovy Template in the skeleton of Profile |
| 2023.2.x      | Spring Boot 3.2, Spring Framework 6.1, Groovy 4.0 | GORM domain inheritance with Groovy 4. Gradle deprecation warning fixes. Upgraded to Spring 6.1.x, JUnit 5.10.x, H2 2.2.224. |
| 2023.1.x      | Spring Boot 3.1, Spring Framework 6.0, Groovy 4.0 | Support for Groovy 4/5, Migrate many Plugins to Spring Boot's AutoConfiguration |
| 2023.0.x      | Spring Boot 3.0, Spring Framework 6.0, Groovy 4.0 | Jakarta migration. JDK 17 baseline. Removal of GORM for JPA, Remove supports for Micronaut Spring, Better support integration with other View templates, Auto-configure GroovyPages plugin, Improve the Shell and Console, Application Templates with Groovy scripts and Ant tasks, Namespaced Command |
| 2022.2.x      | Spring Boot 2.7, Spring Framework 5.3, Groovy 3.0, SiteMesh 2.4.4, JSP 2.2 and JSTL 1.2 | Migrated to Gradle 8, Merging GSP into the framework with many improvements for GSP, Support for RxJava 3.0, Removed support for JPA Entity from GORM, database-migration as a default feature |
| 2022.1.x      | Spring Boot 2.7, Spring Framework 5.3, Groovy 3.0, Gradle 7.6 | Migrated to Gradle 7, Integrated Spring Boot CLI, Adding Grails Dynamic Modules |
| 2022.0.x      | Spring Boot 2.7, Spring Framework 5.3, Groovy 3.0, Hibernate 5.6.15.Final | Migration and rebranding from Grails, initial release of the Grace Framework |

## Grace Roadmap

Grace framework 2025 will build on Spring Boot 4.x, upgrade to Spring Framework 7.x and Groovy 5.0, provides many more core plugins and modules, such as Admin, Jobs, Mail, Security, Web Async and Socket, also provides built-in support for [HTMX](https://htmx.org), [Unpoly](https://unpoly.com), and [Hotwire (Turbo & Stimulus)](https://hotwired.dev).

### Grace 2025

* Groovy 5
* Java 25
* Spring Boot 4
* Spring Framework 7
* Hibernate 7
* Gradle 9

### Grace 2024 (Active Development)

* Groovy 4
* Java 17
* Spring Boot 3.5
* Spring Framework 6.2
* Hibernate 5&6
* Gradle 8.14

## Version Support

To make it easier for users to use and upgrade, [Grace Framework](https://github.com/graceframework/grace-framework) adopts a version policy consistent with the [Spring Boot](https://github.com/spring-projects/spring-boot).


| Grace Version | Groovy Version | Spring Boot Version |
|---------------|----------------|---------------------|
| [2024.2.x](/tree/2024.2.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.5.x](https://docs.spring.io/spring-boot/3.5/index.html) |
| [2024.1.x](/tree/2024.1.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.5.x](https://docs.spring.io/spring-boot/3.5/index.html) |
| [2024.0.x](/tree/2024.0.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.4.x](https://docs.spring.io/spring-boot/3.4/index.html) |
| [2023.3.x](/tree/2023.3.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.3.x](https://docs.spring.io/spring-boot/3.3/index.html) |
| [2023.2.x](/tree/2023.2.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.2.x](https://docs.spring.io/spring-boot/docs/3.2.12/reference/html/) |
| [2023.1.x](/tree/2023.1.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.1.x](https://docs.spring.io/spring-boot/docs/3.1.12/reference/html/) |
| [2023.0.x](/tree/2023.0.x) | [4.0](https://groovy-lang.org/releasenotes/groovy-4.0.html) | [3.0.x](https://docs.spring.io/spring-boot/docs/3.0.13/reference/html/) |
| [2022.2.x](/tree/2022.2.x) | [3.0](https://groovy-lang.org/releasenotes/groovy-3.0.html) | [2.7.x](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/) |
| [2022.1.x](/tree/2022.1.x) | [3.0](https://groovy-lang.org/releasenotes/groovy-3.0.html) | [2.7.x](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/) |
| [2022.0.x](/tree/2022.0.x) | [3.0](https://groovy-lang.org/releasenotes/groovy-3.0.html) | [2.7.x](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/) |

## Getting Started

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grace distribution.

To install Grace, visit https://github.com/graceframework/grace-framework/releases and download the version you would like to use. Set a `GRACE_HOME` environment variable to point to the root of the extracted download and add `GRACE_HOME/bin` to your executable `PATH`. Then in a shell, type the following:

```bash
$ grace create-app com.example.blog
$ cd blog
$ grace generate scaffold Post title:string
$ grace run-app
```

SDKMAN! (The Software Development Kit Manager) can be used for managing multiple versions of various binary SDKs, including Groovy, Gradle and the Grace.
Get SDKMAN! from sdkman.io and install Grace by using the following commands:

```bash
$ sdk install grace
```

To build Grace, clone this GitHub repository and execute the install Gradle target:

```bash
$ git clone https://github.com/graceframework/grace-framework.git
$ cd grace-framework
$ ./gradlew pTML zipDist
```

If you encounter out of memory errors when trying to run the install target, try adjusting Gradle build settings. For example:

```bash
$ export GRADLE_OPTS="-Xmx2G -Xms2G -XX:NewSize=512m -XX:MaxNewSize=512m"
```

For installation instructions see [INSTALL.txt](INSTALL.txt).

## Plugins

Grace is first and foremost a web application framework, but it is also a platform. Grace provide [Plugin API](/grace-plugin-api/README.md) to expose a number of extension points that let you extend anything from the command line interface to the runtime configuration engine.

[Grace Framework](https://github.com/graceframework/) repository contains core plugins and most commonly used plugins, which are provided by default when creating a project.

* [Grace Async](/grace-plugin-async/README.md) provides asynchronous, parallel programming, which integrate Grace with various asynchronous libraries and frameworks such as GPars and RxJava.
* [Grace Cache](/grace-plugin-cache/README.md) provides powerful and easy to use caching functionality to Grace applications and plugins.
* [Grace Data](https://github.com/graceframework/grace-data) formerly known as `GORM` is the data access toolkit to provides a rich set of APIs for accessing relational and non-relational data including implementations for Hibernate (SQL), MongoDB, etc.
* [Grace Data Hibernate](https://github.com/graceframework/grace-data-hibernate) provides a GORM implementation for Hibernate ORM.
* [Grace Data MongoDB](https://github.com/graceframework/grace-data-mongodb) provides a GORM implementation for the MongodB Document Database.
* [Grace Database Migration](/grace-plugin-database-migration/README.md) helps you manage database changes uses the Liquibase library.
* [Grace Events](/grace-plugin-events/README.md) provides Events APIs, which integrate Grace with various asynchronous libraries and frameworks such as GPars and RxJava.
* [Grace Fields](/grace-plugin-fields/README.md) is a plugin allows you to customize the rendering of input fields for properties of domain objects, command beans and POGOs based on their type, name, etc.
* [Grace Geb](/grace-plugin-geb/README.md) provides the Geb dependencies and a `create-functional-test` command for generating Geb tests.
* [Grace Scaffolding](/grace-plugin-scaffolding/README.md) is a plugin to generate scaffolded controllers and views for your Grace application.
* [Grace Views](/grace-views-core/README.md) includes JSON views powered by Groovy's JsonBuilder, also provides the basis for implementation other view types.

[Grace Plugins](https://github.com/grace-plugins/) repository contains several plugins to develop applications more easier and productive.

* [Grace Admin](https://github.com/grace-plugins/grace-admin) is a powerful and flexible, extensible administration framework and management console for Grace, which use [Grace Dynamic Modules](/grace-plugin-dynamic-modules/README.md).
* [Grace Asset Pipeline Plugin](https://github.com/grace-plugins/grace-asset-pipeline) is a plugin used for managing and processing static assets in Grace applications.
* [Grace Dynamic Modules Plugin](https://github.com/grace-plugins/grace-dynamic-modules) is a powerful and flexible, extensible administration framework and management console for Grace.
* [Grace Htmx](https://github.com/grace-plugins/grace-htmx) is a plugin provide helpers to easy use [HTMX](https://htmx.org) with Grace.
* [Grace Hotwire](https://github.com/grace-plugins/grace-hotwire) is a plugin for using [Hotwire](https://hotwired.dev) Stimulus and Turbo with Grace.
* [Grace Inertia](https://github.com/grace-plugins/grace-inertia) is a plugin for using [Inertia.js](https://inertiajs.com) with Grace.
* [Grace Policy](https://github.com/grace-plugins/grace-policy) is an Authorization plugin for Grace applications.
* [Grace Unpoly](https://github.com/grace-plugins/grace-unpoly) is a plugin for using [Unpoly](https://unpoly.com) with Grace.
* [Grace View Components](https://github.com/grace-plugins/grace-view-components) is a plugin for creating reusable, testable and encapsulated view components.

## Profiles

Grace profile is a simple directory that contains a `profile.yml` file and directories containing the "commands", "skeleton" and "templates" defined by the profile.

Grace provides several profiles in the [Grace Profiles](https://github.com/grace-profiles) repository, 

* `base` - A profile for other profiles to extend from
* `plugin` - A profie to create a plugin
* `profile` - A profie to create a custom profile
* `rest-api` - A profie for REST API applications
* `starter` - A profile for getting start to create anything you like
* `web` - The default profile to creae a web app
* `web-plugin` - A profile for Web plugin that contains web resources `css` `js` `images`

## Guides

[Grace Guides](https://github.com/grace-guides) repository contains several guides that show how to use Grace.

* [Spring Boot Application with Plugins](https://github.com/grace-guides/gs-spring-boot) is a how-to guide that shows you how to use Grace Plugins in your Spring Boot application.
* [Spring Boot Application with GSP](https://github.com/grace-guides/gs-spring-boot-gsp) is a how-to guide that shows you how to use GSP as view templates in your Spring Boot application.
* [Spring Boot Application with GORM](https://github.com/grace-guides/gs-spring-boot-gorm) is a how-to guide that shows you how to use GORM as data persistence layer in your Spring Boot application.
* [Grace app and Spring Boot Test](https://github.com/grace-guides/gs-spring-boot-test) is a how-to guide that shows you how to use `@SpringBootTest` to test your Grace application.
* [Build Admin Console with Grace Admin Plugin](https://github.com/grace-guides/gs-admin-console) is a how-to guide that shows you how to use Grace Admin plugin to build flexible, extensible management console in your application.

## Ducumentation

* [2024.2.x](https://graceframework.org/grace-framework/2024.2.x/)

## License

Grace framework is Open Source software released under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
