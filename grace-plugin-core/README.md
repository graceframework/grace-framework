# Grace Plugin Core

The `grace-plugin-core` module is a foundational Grace Framework plugin that provides core Spring bean configuration, custom property editors, development shutdown hooks, and Groovy AOP integration. It has the highest priority (order 0) among plugins to ensure it loads first.


## Module Overview

The `grace-plugin-core` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-api`, `grace-core`, `grace-plugin-api`, and `grace-spring`.

It has API dependencies on Spring AOP, beans, context, core, and Spring Boot autoconfigure, with optional compile-only dependencies on AspectJ runtime and Spring TX.

## Important APIs

### CoreGrailsPlugin

The main plugin class `CoreGrailsPlugin` extends `Plugin` and implements `PriorityOrdered` with order 0, ensuring it loads before all other plugins.

**Key Features:**

- Watched Resources: Monitors configuration files in `app/conf/spring/resources.xml`, `app/conf/spring/resources.groovy`, `app/conf/application.groovy`, `app/conf/application.yml`
- doWithSpring(): Registers custom property editors and development shutdown hooks
- onChange(): Handles hot reloading of XML and Groovy bean definitions

**Registered Beans:**

- `shutdownHook` - Development shutdown hook for graceful shutdown in dev mode
- `customEditors` - Custom property editors for Class and Properties types

### Auto-Configuration Classes

**CoreConfiguration**: Defines essential Spring beans including ClassLoader, ConfigProperties, and ResourceLocator.

**GroovyAopAutoConfiguration**: Patches Spring's `AopConfigUtils` via reflection to inject Groovy-aware proxy creators (`GroovyAwareAspectJAwareAdvisorAutoProxyCreator`), ensuring AOP works correctly with Groovy's dynamic dispatch.
