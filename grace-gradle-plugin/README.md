## Grace Gradle Plugin

This is a Gradle plugin for Grace which provides a bunch of useful plugins.

The Grace Gradle Plugin provides Grace support in [Gradle](https://gradle.org).
It allows you to package Grace plugins or profiles, run Grace applications, and use the dependency management provided by `grace-bom`.
Grace's Gradle plugin requires Gradle 7.x (7.6.4 or later) or 8.x (8.4 or later) and can be used with Gradle's [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html).


| Plugin ID                          | Description |
|------------------------------------|-------------|
| `org.graceframework.grace-core`    |The core Grace gradle plugin implementation |
| `org.graceframework.grace-app`     | Adds web specific extensions (same as `grace-web`) |
| `org.graceframework.grace-doc`     | Adds Grace doc publishing support |
| `org.graceframework.grace-gsp`     | Adds support for compiling Groovy Server Pages (GSP) |
| `org.graceframework.grace-plugin`  | A Gradle plugin for Grace plugins |
| `org.graceframework.grace-profile` | A plugin that is capable of compiling a Grace profile into a JAR file for distribution |
| `org.graceframework.grace-web`     | Adds web specific extensions |
| `org.graceframework.grace-json`    | The Gradle plugin for Json Views |
| `org.graceframework.grace-markup`  | The Gradle plugin for Markup Views |
