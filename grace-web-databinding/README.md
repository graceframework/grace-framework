# Grace Web Databinding

`grace-web-databinding` is the web-layer data binding module that bridges the low-level `grace-databinding` infrastructure with the servlet/web layer. It provides `GrailsWebDataBinder` — the primary binder used in Grace applications — along with the `WebDataBinding` trait, `DataBindingUtils`, and web-specific converters.


## Module Purpose

The `grace-databinding` README describes the relationship clearly: `GrailsWebDataBinder` extends `SimpleDataBinder` and adds all the GORM-specific handling (loading domain objects by ID, handling bidirectional associations, etc.) that is specific to a Grace application context.


## Important APIs

### 1. `GrailsWebDataBinder` (`grails.web.databinding.GrailsWebDataBinder`)

The central class. Extends `SimpleDataBinder` and is registered as the Spring bean `grailsWebDataBinder`. Key capabilities:

- Binds HTTP request parameters, Maps, XML (`GPathResult`), and JSON to domain objects and command objects
- Handles GORM associations: loading existing domain objects by `id`, updating bidirectional associations
- Supports `trimStrings`, `convertEmptyStringsToNull`, `autoGrowCollectionLimit` configuration
- Auto-discovers `ValueConverter`, `FormattedValueConverter`, and `StructuredBindingEditor` beans from the Spring context
- Supports `DataBindingListener` callbacks (`beforeBinding`, `afterBinding`, `bindingError`)

```groovy
def binder = new GrailsWebDataBinder(grailsApplication)
binder.bind author, [name: 'Jeff', birthDate: '11151969'] as SimpleMapDataBindingSource
```

### 2. `DataBinder` interface (`grails.web.databinding.DataBinder`)

The web-layer binder interface (distinct from `grails.databinding.DataBinder`). Provides `bindData()` overloads with `include`/`exclude` lists and prefix filters. This is the interface implemented by the `Controller` trait.

### 3. `DataBindingUtils` (`grails.web.databinding.DataBindingUtils`)

Static utility class used throughout the framework:

- `DATA_BINDER_BEAN_NAME = "grailsWebDataBinder"` — the Spring bean name
- `bindObjectToInstance(instance, source)` — binds to any POGO
- `bindObjectToDomainInstance(dc, instance, source)` — binds to a GORM domain class
- `assignBidirectionalAssociations(instance, source, dc)` — fixes up bidirectional GORM associations
- `bindToCollection(targetType, collection, request)` — binds a collection from a request

Used by `ControllersDomainBindingApi` to support the `new Book(params)` constructor pattern on domain classes:

### 4. `WebDataBinding` trait (`grails.web.databinding.WebDataBinding`)

A Groovy trait that provides `bindData()` methods. Injected into domain classes and command objects by `WebDataBindingTraitInjector` at compile time. Also used directly in test specs.

### 5. `AbstractStructuredBindingEditor` (`org.grails.web.databinding.converters.AbstractStructuredBindingEditor`)

Base class for custom structured binding editors. Extend this to bind multi-field form inputs (e.g., `president_firstName`, `president_lastName`) into a single object. Beans of this type are auto-discovered by `GrailsWebDataBinder`.

### 6. `DefaultASTDatabindingHelper`

AST transformation helper used by `ControllerDomainTransformer` to inject databinding code into domain classes at compile time.


## How It Works

```
HTTP Request (params / JSON body / XML)
  ↓
GrailsParameterMap (grace-web) — parses nested keys like "book.author.name"
  ↓
DataBindingUtils.bindObjectToInstance() / bindObjectToDomainInstance()
  ↓
GrailsWebDataBinder.bind(obj, source)
  ↓ (extends SimpleDataBinder)
  - Checks ValueConverter beans for type conversion
  - Checks StructuredBindingEditor beans for multi-field types
  - For GORM domain objects: loads by id, handles associations
  - Fires DataBindingListener callbacks
  ↓
Object properties populated
```

The `Controller` trait implements `DataBinder` which exposes `bindData(target, source, [include:..., exclude:...])` to controller actions.


## Usage in Other Modules

| Module | Usage |
|--------|-------|
| `grace-plugin-databinding` | Declares it as `api` — provides `DataBindingConfiguration` auto-configuration that registers `grailsWebDataBinder` bean |
| `grace-web-mvc` | Declares it as `api` — `ControllersDomainBindingApi` uses `DataBindingUtils` for the `new Book(params)` constructor pattern |
| `grace-plugin-controllers` | Uses `DataBinder` trait and `DataBindingUtils` in the `Controller` trait for `bindData()` |

