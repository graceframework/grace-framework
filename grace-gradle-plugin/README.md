# Grace Gradle Plugin

`grace-gradle-plugin` is the Gradle plugin module that provides build system integration for Grace Framework applications. It contains multiple specialized Gradle plugins that configure the build lifecycle, dependency management, source compilation, and packaging for Grace projects.


### Core Plugin Implementations

`GrailsGradlePlugin` - The main Grace Gradle plugin implementation that serves as the base for all Grace application builds. It:

- Applies Spring Boot and Dependency Management plugins
- Registers the `grails` extension for project configuration
- Configures Grace source directories (controllers, domain, services, etc.)
- Registers the `findMainClass` task for Spring Boot's `bootRun`

`GrailsPluginGradlePlugin` - Extends `GrailsGradlePlugin` for building Grace plugins (as opposed to applications). It:

- Configures an `ast` source set for AST transformation classes
- Adds `ProcessPluginResourcesTask` to package CLI commands and templates
- Disables `bootJar` and `bootRun` tasks since plugins are libraries

### Published Gradle Plugins

The module publishes multiple Gradle plugins for different use cases:

| Plugin ID                          | Implementation Class | Description |
|------------------------------------|----------------------|-------------|
| `org.graceframework.grace-core`    | `GrailsGradlePlugin` | The core Grace gradle plugin implementation |
| `org.graceframework.grace-app`     | `GrailsWebGradlePlugin` | Adds web specific extensions (same as `grace-web`) |
| `org.graceframework.grace-doc`     | `GrailsDocGradlePlugin` | Adds Grace doc publishing support |
| `org.graceframework.grace-gsp`     | `GroovyPagePlugin` | Adds support for compiling Groovy Server Pages (GSP) |
| `org.graceframework.grace-plugin`  | `GrailsPluginGradlePlugin` | A Gradle plugin for Grace plugins |
| `org.graceframework.grace-profile` | `GrailsProfileGradlePlugin` | A plugin that is capable of compiling a Grace profile into a JAR file for distribution |
| `org.graceframework.grace-web`     | `GrailsWebGradlePlugin` | Adds web specific extensions |
| `org.graceframework.grace-json`    | `GrailsJsonViewsPlugin` | The Gradle plugin for Json Views |
| `org.graceframework.grace-markup`  | `GrailsMarkupViewsPlugin` | The Gradle plugin for Markup Views |

You can find them in the [Gradle Plugin Portal](https://plugins.gradle.org/u/grace).
