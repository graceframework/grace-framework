# Grace Plugin Mimetypes

The `grace-plugin-mimetypes` module provides content negotiation capabilities to Grace applications, enabling MIME type detection, header parsing, and format resolution for HTTP requests and responses.

---

## Important APIs

### Core Interfaces and Implementations

| API | Purpose | Location |
|-----|---------|----------|
| `MimeUtility` | Resolves MimeType from file extensions or URIs | `grails.web.mime.MimeUtility` |
| `DefaultMimeUtility` | Default implementation maintaining extension-to-MimeType map | `org.grails.web.mime.DefaultMimeUtility` |
| `AcceptHeaderParser` | Parses HTTP Accept header into ordered MimeType array | `grails.web.mime.AcceptHeaderParser` |
| `DefaultAcceptHeaderParser` | Implementation with quality factor and XML normalization | `org.grails.web.mime.DefaultAcceptHeaderParser` |
| `MimeTypeResolver` | Resolves MimeType for request/response | `grails.web.mime.MimeTypeResolver` |
| `DefaultMimeTypeResolver` | Default implementation using GrailsWebRequest | `org.grails.web.mime.DefaultMimeTypeResolver` |

### Extension Methods

The module adds extension methods to `HttpServletRequest` and `HttpServletResponse` via Groovy extension modules:

**HttpServletRequestExtension**:
- `getFormat()` - Obtains request format from CONTENT_TYPE header
- `getMimeTypes()` - Gets configured MimeType instances for the request
- `withFormat(Closure)` - Enables `request.withFormat { }` syntax

**HttpServletResponseExtension**:
- `getFormat()` - Obtains response format using extension or ACCEPT header
- `getMimeType()` - Obtains MimeType for the response
- `withFormat(Closure)` - Enables `response.withFormat { }` syntax

## Usage in Other Modules

### Direct Dependencies

The `grace-plugin-controllers` module directly depends on `grace-plugin-mimetypes`:

The `ResponseRenderer` trait in controllers uses `MimeUtility` to handle content negotiation:

### 2024.0 Refactoring

In Grace 2024.0.0-RC1, the core MIME type APIs were relocated from `grace-plugin-mimetypes` to `grace-web` as part of the modularization effort:

- `AcceptHeaderParser`, `MimeUtility`, `DefaultMimeUtility`, `DefaultAcceptHeaderParser` moved to `grace-web`
- Utility methods from `HttpServletResponseExtension` moved to `MimeTypeUtils`

### Configuration

The module uses Spring Boot auto-configuration via `MimeTypesConfiguration`:

The legacy plugin classes (`AbstractMimeTypesGrailsPlugin` and `MimeTypesGrailsPlugin`) are deprecated in favor of the new configuration approach.

## Notes

The `grace-plugin-mimetypes` module is being refactored as part of Grace 2024.0's modularization effort. Core MIME type detection and parsing APIs have been moved to the `grace-web` module to reduce coupling and improve modularity. The plugin itself is now deprecated, with functionality provided through `MimeTypesConfiguration` auto-configuration instead of the traditional Grails plugin mechanism.
