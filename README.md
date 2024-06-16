[![Main branch build status](https://github.com/graceframework/grace-framework/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-framework/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework/grace-core.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://twitter.com/graceframework)

Grace Framework
===

[Grace](https://github.com/graceframework/grace-framework) is a fork of Grails 5 that started development in early 2022, it's a powerful and open-source One-Person web framework used to build enterprise-grade [Spring Boot](https://spring.io/projects/spring-boot/) applications with the powerful [Groovy](https://groovy-lang.org/) programming language. The core framework is very extensible and there are numerous [Plugins](https://github.com/grace-plugins/) available that provide easy integration of add-on features.

Getting Started
---

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grace distribution.

To install Grace, visit https://github.com/graceframework/grace-framework/releases and download the version you would like to use. Set a `GRACE_HOME` environment variable to point to the root of the extracted download and add `GRACE_HOME/bin` to your executable `PATH`. Then in a shell, type the following:

```bash
    grace create-app com.example.blog
    cd blog
    grace run-app
```

To build Grace, clone this GitHub repository and execute the install Gradle target:

```bash
    git clone https://github.com/graceframework/grace-framework.git
    cd grace-framework
    ./gradlew pTML zipDist
```

If you encounter out of memory errors when trying to run the install target, try adjusting Gradle build settings. For example:

```bash
    export GRADLE_OPTS="-Xmx2G -Xms2G -XX:NewSize=512m -XX:MaxNewSize=512m"
```

For installation instructions see [INSTALL.txt](INSTALL.txt).

Plugins
---

Grace is first and foremost a web application framework, but it is also a platform. Grace provide [Plugin API](/grace-plugin-api/README.md) to expose a number of extension points that let you extend anything from the command line interface to the runtime configuration engine.

[Grace Plugins](https://github.com/grace-plugins/) repository contains several plugins to develop applications more easier and productive.

* [Grace Admin](https://github.com/grace-plugins/grace-admin) is a powerful and flexible, extensible administration framework and management console for Grace, which use [Grace Dynamic Modules](/grace-plugin-dynamic-modules/README.md).
* [Grace Htmx](https://github.com/grace-plugins/grace-htmx) is a plugin provide helpers to easy use [HTMX](https://htmx.org).
* [Grace Hotwire](https://github.com/grace-plugins/grace-hotwire) is a plugin for using [Hotwire](https://hotwired.dev) Stimulus and Turbo.
* [Grace Unpoly](https://github.com/grace-plugins/grace-unpoly) is a plugin for using [Unpoly](https://unpoly.com).
* [Grace View Components](https://github.com/grace-plugins/grace-view-components) is a plugin for creating reusable, testable and encapsulated view components.

Profiles
---

Grace profile is a simple directory that contains a `profile.yml` file and directories containing the "commands", "skeleton" and "templates" defined by the profile.

Grace provides several profiles in the [Grace Profiles](https://github.com/grace-profiles) repository, 

* `base` - a profile for other profiles to extend from
* `plugin` - a profie to create a plugin
* `profile` - a profie to create a custom profile
* `rest-api` - a profie for REST API applications
* `web-plugin` - a profile for Web plugin that contains web resources `css` `js` `images`
* `web` - default profile to creae a web app

Guides
---

[Grace Guides](https://github.com/grace-guides) repository contains several guides that show how to use Grace.

* [Spring Boot Application with Grace Plugins](https://github.com/grace-guides/gs-spring-boot) is an introductory guide that shows you how to use Grace Plugins in your Spring Boot application.
* [Spring Boot Application with GSP](https://github.com/grace-guides/gs-spring-boot-gsp) is an introductory guide that shows you how to use GSP as view templates in your Spring Boot application.
* [Spring Boot Application with GORM](https://github.com/grace-guides/gs-spring-boot-gorm) is an introductory guide that shows you how to use GORM as data persistence layer in your Spring Boot application.
* [Build Admin Console with Grace Admin Plugin](https://github.com/grace-guides/gs-admin-console) is an introductory guide that shows you how to use Grace Admin plugin to build flexible, extensible management console in your application.

License
---

Grace framework is Open Source software released under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).