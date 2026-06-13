# Grace Plugin Scaffolding

The `grace-plugin-scaffolding` module provides scaffolding capabilities for Grace applications — both **dynamic scaffolding** (runtime-generated views/actions) and **static scaffolding** (code generation via CLI commands).


## Module Purpose

Grace Scaffolding generates basic CRUD interfaces for domain classes, including GSP views and controller actions. It works together with `grace-plugin-fields` for rendering domain properties.

The plugin is registered as `ScaffoldingGrailsPlugin` and auto-configured via two configuration classes.


## Important APIs

### 1. `static scaffold = DomainClass` (Dynamic Scaffolding)

Setting `static scaffold = Book` in a controller triggers the `ScaffoldingControllerInjector` AST transformation at compile time. It detects the `scaffold` property and makes the controller extend `RestfulController<T>`, wiring it to the specified domain class.

### 2. `ScaffoldingViewResolver` (Dynamic View Generation)

Extends `GroovyPageViewResolver`. When a view is not found on disk, it checks if the current controller has a `scaffold` property. If so, it looks up a GSP template from:

1. `src/main/templates/scaffolding/` (dev mode)
2. Classpath relative to the controller class
3. `classpath:META-INF/templates/scaffolding/`

It then renders the template using `GStringTemplateEngine` with a `Model` built from the domain class, caching the result.

### 3. `ScaffoldingAutoConfiguration`

Auto-configures the `scaffoldingViewResolver` bean (aliased as `jspViewResolver`), wiring it with the `GrailsConventionGroovyPageLocator` and `GroovyPagesTemplateEngine`. Respects `grails.gsp.reload.interval` and development mode settings.

### 4. `ScaffoldingBeanConfiguration` (Markup Rendering Beans)

Registers the scaffolding-core rendering infrastructure from `grace-scaffolding-core`:

| Bean | Interface | Purpose |
|------|-----------|---------|
| `contextMarkupRenderer` | `ContextMarkupRenderer` | Renders context-level markup (forms, tables) |
| `domainMarkupRenderer` | `DomainMarkupRenderer` | Renders markup for a full domain class |
| `propertyMarkupRenderer` | `PropertyMarkupRenderer` | Renders markup for individual properties |
| `domainPropertyFactory` | `DomainPropertyFactory` | Creates `DomainProperty` instances from GORM mapping |
| `domainModelService` | `DomainModelService` | Provides domain model metadata |
| `domainInputRendererRegistry` | `DomainInputRendererRegistry` | Registry of input renderers by property type |
| `domainOutputRendererRegistry` | `DomainOutputRendererRegistry` | Registry of output renderers by property type |
| `domainRendererRegisterer` | `DomainRendererRegisterer` | Registers default renderers into the registries |

### 5. `ScaffoldGenerator` (Code Generation via `grace generate`)

Implements `AbstractGenerator` to support the `grace generate scaffold` command. Generates a full set of files (domain class, controller, service, GSP views, and test specs) and supports `revoke()` to undo the generation.


## CLI Commands Provided

| Command | Script | What it generates |
|---------|--------|-------------------|
| `grace generate-all Book` | `GenerateAll.groovy` | Controller + views |
| `grace generate-controller Book` | `GenerateController.groovy` | Controller, Service, ControllerSpec, ServiceSpec |
| `grace generate-async-controller Book` | `GenerateAsyncController.groovy` | Async controller + spec |
| `grace generate-views Book` | `GenerateViews.groovy` | GSP views (create, edit, index, show) |
| `grace create-scaffold-controller Book` | `CreateScaffoldController.groovy` | A controller with `static scaffold = Book` |
| `grace install-templates` | `InstallTemplates.groovy` | Copies scaffolding templates to `src/main/templates/scaffolding` for customization |
