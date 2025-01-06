[![Main branch build status](https://github.com/graceframework/grace-database-migration/workflows/Grace%20CI/badge.svg?style=flat)](https://github.com/graceframework/grace-database-migration/actions?query=workflow%3A%Grace+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=flat)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.graceframework.plugins/database-migration.svg?label=Maven%20Central&logo=apache-maven&style=flat)](https://search.maven.org/search?q=g:org.graceframework.plugins)
[![Grace on X](https://img.shields.io/twitter/follow/graceframework?style=social)](https://twitter.com/graceframework)

[![Groovy Version](https://img.shields.io/badge/Groovy-4.0.24-blue?style=flat&color=4298b8)](https://groovy-lang.org/releasenotes/groovy-4.0.html)
[![Grace Version](https://img.shields.io/badge/Grace-2023.2.0-blue?style=flat&color=f49b06)](https://github.com/graceframework/grace-framework/releases/tag/v2023.2.0-RC1)
[![Spring Boot Version](https://img.shields.io/badge/Spring_Boot-3.2.12-blue?style=flat&color=6db33f)](https://github.com/spring-projects/spring-boot/releases/tag/v3.2.12)

# Grace Database Migration Plugin

## Overview

The Database Migration plugin helps you manage database changes while developing Grails applications. The plugin uses the Liquibase library. Using this plugin (and Liquibase in general) adds some structure and process to managing database changes. It will help avoid inconsistencies, communication issues, and other problems with ad-hoc approaches.

Database migrations are represented in text form, either using a Groovy DSL or native Liquibase XML, in one or more changelog files. This approach makes it natural to maintain the changelog files in source control and also works well with branches. Changelog files can include other changelog files, so often developers create hierarchical files organized with various schemes.

One popular approach is to have a root changelog named changlog.groovy (or changelog.xml) and to include a changelog per feature/branch that includes multiple smaller changelogs. Once the feature is finished and merged into the main development tree/trunk the changelog files can either stay as they are or be merged into one large file. Use whatever approach makes sense for your applications, but keep in mind that there are many options available for changelog management.

## Versions

To make it easier for users to use and upgrade, Plugin adopts a version policy consistent with the [Grace Framework](https://github.com/graceframework/grace-framework).

| Plugin Version | Grace Version |
|----------------|---------------|
| 6.2.x          | 2023.2.x      |
| 6.1.x          | 2023.1.x      |
| 6.0.x          | 2023.0.x      |
| 5.2.x          | 2022.2.x      |
| 5.1.x          | 2022.1.x      |
| 5.0.x          | 2022.0.x      |

## Links

- [Grace Framework](https://github.com/graceframework/grace-framework)
- [Grace Plugins](https://github.com/grace-plugins)
