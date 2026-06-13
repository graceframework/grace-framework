# Grace Boot Web

`grace-boot-web` is a Spring Boot auto-configuration module that provides web application functionality for Grace applications, including MVC, REST, and GSP (Groovy Server Pages) support.

It was created as part of the 2024.x modularization effort to make Grace's web functionality more focused and better integrated with Spring Boot's module structure.

The module aggregates multiple web-related plugins and core web modules:

- `grace-boot` - Core boot module
- `grace-plugin-api` - Plugin API
- `grace-plugin-codecs` - Encoding/decoding support
- `grace-plugin-controllers` - Controller functionality
- `grace-plugin-converters` - JSON/XML converters
- `grace-plugin-core` - Core plugin functionality
- `grace-plugin-databinding` - Data binding
- `grace-plugin-i18n` - Internationalization
- `grace-plugin-interceptors` - Interceptor support
- `grace-plugin-mimetypes` - MIME type handling
- `grace-plugin-url-mappings` - URL mapping
- `grace-web` - Core web functionality
- Spring Boot starter, JSON starter, and Tomcat


The module is designed for building web and RESTful applications using Grace MVC, REST, and GSP, with Tomcat as the default embedded container. It provides the foundational web infrastructure that other web-specific modules build upon.
