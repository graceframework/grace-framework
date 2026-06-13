# Grace Web

The `grace-web` module is the foundational web layer of the Grace framework. It provides the core servlet/web infrastructure — request context, parameter handling, flash scope, view resolution, MIME type utilities, and the traits injected into all controllers and taglibs.


## Module Structure

```
grace-web/src/main/groovy/
├── grails/web/
│   ├── api/          ← WebAttributes, ServletAttributes traits
│   ├── mvc/          ← FlashScope interface
│   ├── mime/         ← MimeTypeUtils, AcceptHeaderParser interfaces
│   ├── servlet/mvc/  ← GrailsParameterMap, GrailsHttpSession
│   ├── http/         ← HttpHeaders constants
│   ├── controllers/  ← @Action, @RequestParameter
│   ├── UrlConverter.java, CamelCaseUrlConverter, HyphenatedUrlConverter
└── org/grails/web/
    ├── servlet/mvc/  ← GrailsWebRequest, SynchronizerTokensHolder
    ├── servlet/view/ ← CompositeViewResolver
    ├── servlet/      ← DefaultGrailsApplicationAttributes, GrailsFlashScope, extension methods
    ├── util/         ← WebUtils, GrailsApplicationAttributes (constants interface)
    ├── mime/         ← DefaultAcceptHeaderParser, DefaultMimeUtility
    ├── context/      ← ServletEnvironmentGrailsApplicationDiscoveryStrategy
    └── binding/      ← StructuredDateEditor
```


## Important APIs

### 1. `GrailsWebRequest` (`org.grails.web.servlet.mvc`)

The central per-request context object. Extends Spring's `DispatcherServletWebRequest` and is bound to the current thread via `RequestContextHolder`. Every controller action runs with one of these on the thread.

Key methods:

- `GrailsWebRequest.lookup()` / `lookup(HttpServletRequest)` — static factory to retrieve the current instance
- `getParams()` → `GrailsParameterMap`
- `getFlashScope()` → `FlashScope`
- `getSession()` → `GrailsHttpSession`
- `getCurrentRequest()` / `getCurrentResponse()`
- `getApplicationContext()`
- `getControllerName()`, `getActionName()`, `getControllerNamespace()`
- `getBaseUrl()`, `getContextPath()`

### 2. `WebAttributes` trait (`grails.web.api.WebAttributes`)

Injected into all controllers and taglibs. Provides convenient accessors that delegate to `GrailsWebRequest`:

- `getParams()`, `getFlash()`, `getWebRequest()`
- `getControllerName()`, `getActionName()`, `getControllerNamespace()`
- `getGrailsApplication()`, `getPluginContextPath()`

### 3. `ServletAttributes` trait (`grails.web.api.ServletAttributes`)

Extends `WebAttributes` and adds servlet-specific accessors:

- `getRequest()` → `HttpServletRequest`
- `getResponse()` → `HttpServletResponse`
- `getSession()` → `HttpSession`
- `getServletContext()` → `ServletContext`
- `getApplicationContext()` → `ApplicationContext`

### 4. `GrailsParameterMap` (`grails.web.servlet.mvc`)

A `TypeConvertingMap` wrapping `HttpServletRequest` parameters. Supports:

- Dot-notation nested keys (`params.company.department.name`)
- Multipart file entries
- Auto-parsing of PUT/PATCH request bodies
- Type conversion helpers: `.int()`, `.long()`, `.boolean()`, `.date()`, `.list()`
- `toQueryString()`

### 5. `FlashScope` (`grails.web.mvc.FlashScope`)

A `Map<String, Object>` that survives exactly one redirect. Implements `next()` to advance state and `getNow()` to access current-request-only values.

### 6. `GrailsApplicationAttributes` (`org.grails.web.util`)

Interface defining all Grace-specific request attribute name constants (`RESPONSE_FORMAT`, `FLASH_SCOPE`, `CONTROLLER`, `WEB_REQUEST`, `ACTION_NAME_ATTRIBUTE`, etc.) and methods to retrieve controllers, URIs, and template paths from a request.

### 7. `WebUtils` (`org.grails.web.util`)

Static utility class extending Spring's `WebUtils`:
- `lookupViewResolver(ApplicationContext)` → delegates to `CompositeViewResolver`
- `lookupHandlerInterceptors(ServletContext)`
- `storeGrailsWebRequest(GrailsWebRequest)` / `retrieveGrailsWebRequest()`
- `lookupApplication(ServletContext)`, `findApplicationContext()`
- `resolveView(...)`, `toQueryString(...)`, `fromQueryString(...)`

### 8. `CompositeViewResolver` (`org.grails.web.servlet.view`)

Iterates all registered Spring `ViewResolver` beans and returns the first matching view. Used by `WebUtils.lookupViewResolver()` as the single view resolution entry point.

### 9. `UrlConverter` / `CamelCaseUrlConverter` / `HyphenatedUrlConverter` (`grails.web`)

Interface and two implementations for converting controller/action names to URL segments. `CamelCaseUrlConverter` keeps names as-is; `HyphenatedUrlConverter` converts `camelCase` → `camel-case`.

### 10. `MimeTypeUtils` (`grails.web.mime`)

Static utility for MIME type resolution from the current request — resolves `MimeType[]` from the Accept header, determines `responseFormat`, and manages the `disableForUserAgents` pattern.

### 11. `DefaultAcceptHeaderParser` (`org.grails.web.mime`)

Parses the HTTP `Accept` header into an ordered `MimeType[]` array, respecting `q` quality factors and normalizing XML types.

### 12. Servlet Extension Methods

Groovy extension methods added to standard servlet types:

- `HttpServletRequestExtension` — adds `getFormat()`, `withFormat {}`, `getMimeType()`, `getXml()`, `getJSON()`
- `HttpServletResponseExtension` — adds `format(String)`, `withFormat {}`
- `HttpSessionExtension`, `ServletContextExtension`


## Usage in Other Modules

`grace-web` is one of the most widely depended-upon modules in the framework:

| Module | Why |
|--------|-----|
| `grace-web-mvc`, `grace-web-gsp`, `grace-web-taglib`, `grace-web-url-mappings`, `grace-web-rest`, `grace-web-async`, `grace-web-databinding` | All web sub-modules build on top of it |
| `grace-plugin-mimetypes`, `grace-plugin-converters`, `grace-plugin-databinding`, `grace-plugin-fields` | Plugins use `GrailsWebRequest`, `WebAttributes`, `MimeTypeUtils` |
| `grace-views-core` | View rendering uses `GrailsWebRequest` and `CompositeViewResolver` |
| `grace-test` | Test support needs `GrailsWebRequest`, `GrailsParameterMap`, `FlashScope` |
| `grace-boot-web`, `grace-boot-rest` | Boot aggregator modules pull it in transitively |
| `grace-test-suite-base` | Base test suite depends on it directly |
