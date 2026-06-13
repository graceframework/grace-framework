# Grace Plugin URL Mappings

The `grace-plugin-url-mappings` module is the plugin/configuration layer for Grace's URL routing system. It wires together the URL mapping engine (from `grace-web-url-mappings`), the Spring MVC handler mapping, link generation, CORS support, and actuator integration.


## Module Structure

The module is split across two layers:

- `grace-plugin-url-mappings` — plugin registration, auto-configuration, CORS, reporting
- `grace-web-url-mappings` — the core URL mapping engine (interfaces, evaluator, holder)


## How It Works

### 1. Artefact Detection

`UrlMappingsArtefactHandler` (registered via `grails.factories`) detects classes named `*UrlMappings` in `app/controllers/`. The artefact type constant is `ArtefactTypes.URL_MAPPINGS = "UrlMappings"`.

In Grace 2024.0, `UrlMappingsArtefactHandler` was relocated from `grace-core` into this module.

### 2. Plugin Initialization

`UrlMappingsGrailsPlugin` loads after `controllers`. If no `UrlMappings` artefact is found, it registers `DefaultUrlMappings` as the fallback. On hot-reload, it re-registers the changed artefact and clears the `CachingLinkGenerator` cache.

### 3. Default URL Mappings

`DefaultUrlMappings` provides the conventional fallback mapping `/$controller/$action?/$id?(.$format)?()` when no application-specific `UrlMappings` class is present.

### 4. Spring Boot Auto-Configuration

`UrlMappingsPluginConfiguration` (auto-configured after `ControllersPluginConfiguration`) registers all the key beans:

| Bean | Class | Purpose |
|------|-------|---------|
| `grailsUrlConverter` | `CamelCaseUrlConverter` / `HyphenatedUrlConverter` | Converts controller/action names to URL segments (configurable via `grails.web.url.converter`) |
| `urlMappingsHandlerMapping` | `UrlMappingsHandlerMapping` | Spring MVC `HandlerMapping` that routes requests via Grace URL mappings |
| `urlMappingsInfoHandlerAdapter` | `UrlMappingsInfoHandlerAdapter` | Handles the result of URL mapping resolution, applies `ActionResultTransformer`s |
| `grailsUrlMappingsHolder` | `UrlMappingsHolderFactoryBean` → `UrlMappings` | Evaluates all `UrlMappings` artefacts into a `DefaultUrlMappingsHolder` |
| `grailsLinkGenerator` | `DefaultLinkGenerator` / `CachingLinkGenerator` | Generates links from controller/action/params; caching is disabled in dev mode |
| `urlMappingsErrorPageCustomizer` | `UrlMappingsErrorPageCustomizer` | Registers Spring Boot error pages for response-code mappings (e.g., `"500"(view:'/error')`) |
| `grailsCorsFilter` | `GrailsCorsFilter` | CORS filter (enabled by default) |
| `urlMappingsEndpoint` | `UrlMappingsEndpoint` | Spring Boot Actuator endpoint at `/actuator/urlmappings` |


## Important APIs

### `UrlMapping` interface (`grails.web.mapping.UrlMapping`)

Represents a single URL mapping. Key constants: `WILDCARD`, `DOUBLE_WILDCARD`, `ANY_HTTP_METHOD`, `ANY_VERSION`. Key method: `match(String uri)` returns a `UrlMappingInfo` or null.

### `UrlMappings` / `UrlMappingsHolder` interfaces

`UrlMappingsHolder` (bean id: `grailsUrlMappingsHolder`) provides:
- `getUrlMappings()` — all registered mappings
- `getReverseMapping(controller, action, namespace, pluginName, httpMethod, version, params)` — reverse URL generation
- `match(String uri)` — forward URL matching

`UrlMappings` extends it with `addMappings(Closure)` for runtime registration.

### `LinkGenerator` interface (`grails.web.mapping.LinkGenerator`)

The primary API for generating URLs in controllers, tag libs, and services. Key methods:
- `link(Map params)` — generates a link to a controller/action/URI
- `resource(Map params)` — generates a link to a static resource

Key attributes: `controller`, `action`, `id`, `params`, `absolute`, `mapping`, `namespace`, `plugin`, `fragment`.

### `GrailsCorsConfiguration` (`grails.web.mapping.cors.GrailsCorsConfiguration`)

`@ConfigurationProperties(prefix = 'grails.cors')` bean. Set `grails.cors.enabled = true` and configure `mappings`, `allowedOrigins`, `allowedMethods`, `allowedHeaders`, `allowCredentials`, `maxAge`.

### `UrlMappingsEndpoint`

Spring Boot Actuator endpoint (`@Endpoint(id = "urlmappings")`) that exposes all registered URL mappings as JSON via `/actuator/urlmappings`.

### `UrlMappingsReportCommand`

An `ApplicationCommand` registered via `grails.factories` that powers `./gradlew urlMappingsReport`. It renders all URL mappings to the console using `AnsiConsoleUrlMappingsRenderer`.

### `UrlMappingsHolderFactoryBean`

The factory that evaluates all `UrlMappings` artefacts using `DefaultUrlMappingEvaluator`, builds a `DefaultUrlMappingsHolder`, and wraps it in `GrailsControllerUrlMappings`. Supports cache size tuning via `grails.urlmapping.cache.maxsize` and `grails.urlcreator.cache.maxsize`.
