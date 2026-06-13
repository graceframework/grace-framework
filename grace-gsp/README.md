# Grace GSP

`grace-gsp` is the core module that provides the Groovy Server Pages (GSP) template engine for Grace Framework. It serves as the foundational view rendering technology that compiles `.gsp` files into Groovy classes for generating HTML output.

## Important APIs in grace-gsp

### Core Template Engine

`GroovyPagesTemplateEngine` - The heart of the GSP system, responsible for compiling `.gsp` files into Groovy classes that generate the final output. It provides:

- Template compilation and caching
- Hot reloading support in development mode
- Integration with tag libraries
- Page cache management

### Template Renderer

`GroovyPagesTemplateRenderer` - Provides an internal service for rendering partial templates (fragments) within other views.

### URI Service

`GroovyPagesUriService` - Manages URI caching and resolution for GSP templates, cleared when changes are detected.


## Module Relationships

### grace-web-gsp

The `grace-web-gsp` module depends on `grace-gsp` to provide web-specific GSP integration, including Sitemesh layout support and JSP compatibility. It adds:

- Sitemesh integration for consistent layouts
- JSP tag library support
- Web-specific resource loading

### grace-plugin-gsp

The `grace-plugin-gsp` module depends on `grace-web-gsp` to provide Spring Boot auto-configuration and plugin lifecycle management for GSP. The `GroovyPagesGrailsPlugin` class:

- Clears the page cache when the ApplicationContext is loaded 
- Clears the URI cache after changes
- Reinitializes filtering codecs on configuration changes

### grace-web-mvc

The `grace-web-mvc` module depends on `grace-gsp` for view rendering capabilities in the MVC layer.

### Gradle Plugin Integration

The Gradle plugin provides the `org.graceframework.grace-gsp` plugin that adds support for compiling Groovy Server Pages during the build proces.


## Notes

The `grace-gsp` module was imported into the framework during the 2023.x release as part of the Jakarta EE 9 migration. During the 2024.x refactoring, the GSP plugin was refactored to remove deprecated classes like `GrailsTagLibClass`, `DefaultGrailsTagLibClass`, and `TagLibArtefactHandler`, and to use `ArtefactTypes.TAG_LIBRARY` instead. The module structure follows a layered approach where `grace-gsp` provides the core engine, `grace-web-gsp` adds web-specific features, and `grace-plugin-gsp` provides Spring Boot integration.
