# Grace Boot REST

`grace-boot-rest` is a Spring Boot auto-configuration module that provides REST API functionality for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's REST functionality more focused and better integrated with Spring Boot's module structure.


The module aggregates multiple REST-related plugins and web modules:

- `grace-plugin-codecs` - Encoding/decoding support
- `grace-plugin-controllers` - Controller functionality
- `grace-plugin-converters` - JSON/XML converters
- `grace-plugin-databinding` - Data binding
- `grace-plugin-interceptors` - Interceptor support
- `grace-plugin-mimetypes` - MIME type handling
- `grace-plugin-rest` - Core REST plugin
- `grace-plugin-url-mappings` - URL mapping
- `grace-views-json` - JSON view rendering
- `grace-views-markup` - Markup view rendering
- `grace-web` - Core web functionality


The REST capabilities are primarily provided through the `grace-plugin-rest` module, which `grace-boot-rest` wraps. The `grace-plugin-rest` module itself depends on:

- Core Grace modules (`grace-api`, `grace-bootstrap`, `grace-core`)
- Controller and converter plugins
- Web REST and URL mapping modules
- Optional GORM support for data binding
