# Grace Plugin GSP

The `grace-plugin-gsp` module is a Grace Framework plugin that provides Groovy Server Pages (GSP) template engine capabilities, enabling server-side rendering of dynamic HTML content with support for tag libraries, SiteMesh layout decoration, and JSP compatibility.


## Module Overview

The `grace-plugin-gsp` module is defined as a plugin in the Grace Framework settings and depends on core modules including `grace-core`, `grace-plugin-codecs`, `grace-plugin-taglibs`, `grace-web-gsp`, `grace-web-mvc`, and `grace-web-url-mappings`.

It has API dependencies on Spring Boot autoconfigure and compile-only dependencies on Jakarta Servlet, JSP, and JSTL for JSP compatibility.

## Important APIs

### GroovyPagesAutoConfiguration

The main Spring Boot auto-configuration class that provides GSP integration beans. This class is referenced in the CLI compiler auto-configuration to automatically add the GSP plugin dependency when needed.

### GSP Gradle Plugin

The Gradle plugin `org.graceframework.grace-gsp` provides support for compiling Groovy Server Pages, implemented by `GroovyPagePlugin`.

### Tag Libraries

The module includes tag libraries that were consolidated from `grace-web-gsp-taglib` into `grace-plugin-gsp` and `grace-web-taglib` during the 2024.x refactoring.

### GroovyPagesTemplateRenderer

The template renderer for GSP, which was refined to decouple `grace-web-gsp` from `grace-plugin-domain-class`.
