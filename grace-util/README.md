# Grace Utilities

`grace-util` is the **foundational utility library** of the Grace Framework. It has the lightest dependency footprint of any module — only `spring-beans` and `spring-core` — making it safe to use at every layer of the framework, including the earliest bootstrap phases.


## Key APIs and Classes

### 1. `GrailsNameUtils` — Naming Convention Engine

The most widely used class in the entire framework. Its Javadoc explicitly states: *"The key aspect of this class is that it has no dependencies outside the JDK!"* It converts between all the naming forms Grace uses:

| Method | Example input → output |
|--------|------------------------|
| `getPropertyName(Class)` | `PersonController` → `personController` |
| `getLogicalName(Class, "Controller")` | `PersonController` → `Person` |
| `getLogicalPropertyName(Class, "Controller")` | `PersonController` → `person` |
| `getShortName(Class)` | `com.example.Person` → `Person` |
| `getScriptName(Class)` | `MyFunkyGrailsScript` → `my-funky-grails-script` |
| `getSnakeCaseName(Class)` | `MyFunkyGrailsScript` → `my_funky_grails_script` |
| `getNaturalName(String)` | `firstName` → `First Name` |
| `getClassName(String, String)` | `"person"`, `"Controller"` → `PersonController` |
| `getPropertyNameForLowerCaseHyphenSeparatedName(String)` | `"my-command"` → `myCommand` |
| `getPluginName(String)` | `DbUtilsGrailsPlugin.groovy` → `db-utils` |
| `getGetterName(String)` / `getSetterName(String)` | `"name"` → `getName` / `setName` |
| `isGetter(String, Class, Class[])` | Validates JavaBean getter conventions |
| `isValidJavaPackage(String)` | Validates package name syntax |

### 2. `GrailsClassUtils` — Reflection and Property Utilities

A large utility class for working with Java/Groovy class metadata. Key methods: 

| Method | Purpose |
|--------|---------|
| `getStaticPropertyValue(Class, String)` | Gets a static property value (getter or field) |
| `getPropertyOrStaticPropertyOrFieldValue(Object, String)` | Unified property lookup: instance → static → field |
| `isStaticProperty(Class, String)` | Checks if a property has a public static getter |
| `isAssignableOrConvertibleFrom(Class, Class)` | Handles primitive/wrapper type equivalence |
| `isGroovyAssignableFrom(Class, Class)` | Groovy-aware assignability check |
| `getPropertiesOfType(Class, Class)` | Gets all `PropertyDescriptor`s of a given type |
| `getExpandoMetaClass(Class)` | Retrieves or creates an `ExpandoMetaClass` |
| `createConcreteCollection(Class)` | Creates `ArrayList`/`TreeSet`/`HashSet` for a collection interface |
| `isClassBelowPackage(Class, List)` | Checks if a class is in one of the given packages |
| `PRIMITIVE_TYPE_COMPATIBLE_CLASSES` | Static map of primitive ↔ wrapper type pairs |

### 3. `GrailsMetaClassUtils` — Groovy MetaClass Utilities

Utilities for working with Groovy's `MetaClassRegistry` and `ExpandoMetaClass`:

| Method | Purpose |
|--------|---------|
| `getExpandoMetaClass(Class)` | Gets or creates an `ExpandoMetaClass` for a class |
| `getMetaClass(Object)` | Gets the `ExpandoMetaClass` for an instance |
| `copyExpandoMetaClass(Class, Class, boolean)` | Copies dynamic methods/properties from one class to another (used during hot reload) |
| `getPropertyIfExists(Object, String)` | Reads a property via MetaClass without throwing |
| `invokeMethodIfExists(Object, String, Object[])` | Invokes a method via MetaClass if it exists |

### 4. `AbstractTypeConvertingMap` / `TypeConvertingMap` — Type-Converting Map

A `Map` wrapper that adds typed accessor methods for all primitive types, `Date`, and `List`. This is the base class for `GrailsParameterMap` (HTTP request parameters) and `GrailsFlashScope`.

Key accessors: `getInt(name)`, `getLong(name)`, `getBoolean(name)`, `getDate(name)`, `getDate(name, format)`, `getList(name)`, plus Groovy-syntax aliases `int(name)`, `long(name)`, `boolean(name)`, `date(name)`.

All conversions are null-safe — they return `null` (not an exception) when the value is absent or unparseable.

### 5. `CacheEntry<V>` — Thread-Safe Cache Entry

A generic cache entry that prevents **cache storms** (thundering herd) using a `ReadWriteLock`. It stores a value with a timestamp and an expiry timeout. When the value expires, only one thread recomputes it; other threads can optionally receive the stale value while the update is in progress.

The static `getValue(ConcurrentMap, key, timeoutMillis, updater)` factory method is the primary API — it handles the full lookup-or-create lifecycle on a `ConcurrentMap<K, CacheEntry<V>>`.

### 6. `GrailsStringUtils` — String Utilities

Extends Spring's `StringUtils` with Grace-specific helpers:

| Method | Purpose |
|--------|---------|
| `toBoolean(String)` | `"true"`, `"on"`, `"yes"`, `"1"` → `true` |
| `substringBefore(String, String)` | Substring before first occurrence of token |
| `substringBeforeLast(String, String)` | Substring before last occurrence of token |
| `substringAfter(String, String)` | Substring after first occurrence of token |
| `substringAfterLast(String, String)` | Substring after last occurrence of token |
| `isBlank(String)` / `isNotBlank(String)` | Null-safe blank check |
| `getFileBasename(String)` | Strips path and extension from a file path |

### 7. `GrailsArrayUtils` — Array Utilities

Provides array manipulation operations that work on any array type via `java.lang.reflect.Array`:

Key methods: `addToEnd(array, obj)`, `addToStart(array, obj)`, `add(array, pos, obj)`, `addAll(array, otherArray)`, `subarray(array, start, end)`, `contains(array, element)`.

### 8. `CollectionUtils` — Collection Factories

Varargs factory methods for creating collections inline:

`newMap(k1, v1, k2, v2, ...)`, `newSet(values...)`, `newList(values...)`, `getOrCreateChildMap(parent, key)`.

### 9. `ClosureToMapPopulator` — Closure → Map Bridge

Executes a Groovy closure with `DELEGATE_FIRST` strategy, capturing all property assignments and method calls as map entries. Used extensively in the Spring DSL (`BeanBuilder`) and plugin `doWithSpring` closures.

### 10. `Pair<A,B>` / `Triple<A,B,C>` — Tuples

Immutable generic tuples with `equals`/`hashCode`. Used as cache keys and return values throughout the framework.

### 11. `IncludeExcludeSupport<T>` — Include/Exclude Filter

A generic helper for implementing include/exclude list logic. Used by data binding, JSON rendering, and field rendering to decide which properties to include/exclude.

### 12. `DomainBuilder` — Domain Object Graph Builder

Extends Groovy's `ObjectGraphBuilder` to support Grace domain class conventions. Its `DefaultGrailsChildPropertySetter` calls `addToXxx()` for collection associations instead of direct property assignment.

### 13. `ExtendedProxy` — Groovy Proxy with Property Support

Extends Groovy's `Proxy` to also proxy property `get`/`set` operations through to the adaptee, not just method calls.

## How It Is Used in Other Modules

`grace-util` is a direct `api` dependency of **17 modules**, and since `grace-api`, `grace-bootstrap`, `grace-core`, `grace-plugin-api`, and `grace-spring` all depend on it, virtually every module in the framework uses it transitively.

| Module | Primary usage |
|--------|---------------|
| `grace-api` | `GrailsNameUtils` for artefact naming conventions |
| `grace-bootstrap` | `GrailsNameUtils`, `GrailsStringUtils` for config key resolution |
| `grace-core` | `GrailsNameUtils` in `DefaultGrailsApplication.getProperty()`, `GrailsClassUtils` in artefact handlers, `GrailsMetaClassUtils` in `AbstractGrailsClass`, `CacheEntry` in GSP/URL mapping caches |
| `grace-plugin-api` | `GrailsNameUtils` for plugin name derivation |
| `grace-web` | `TypeConvertingMap` as base for `GrailsParameterMap` and `GrailsFlashScope` |
| `grace-plugin-controllers` | `GrailsClassUtils` for controller property inspection |
| `grace-plugin-url-mappings` | `CacheEntry` for URL mapping cache, `GrailsNameUtils` for action name resolution |
| `grace-plugin-validation` | `IncludeExcludeSupport` for constrained property filtering |
| `grace-plugin-rest` | `IncludeExcludeSupport` for JSON rendering include/exclude |
| `grace-web-databinding` | `IncludeExcludeSupport` for binding include/exclude |
| `grace-encoder` | `GrailsStringUtils` for codec utilities |
| `grace-shell` | `GrailsNameUtils` for CLI command name resolution |

In summary, `grace-util` provides four pillars that everything else builds on: the **naming convention engine** (`GrailsNameUtils`), the **reflection toolkit** (`GrailsClassUtils` + `GrailsMetaClassUtils`), the **type-converting map** (`AbstractTypeConvertingMap`), and the **concurrency-safe cache** (`CacheEntry`).
