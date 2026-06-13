# Grace Plugin Validation

The `grace-plugin-validation` module provides the validation infrastructure for Grace applications — the `Validateable` trait, AST injection of validation code, constraint discovery, and the bridge between Grace's validation API and GORM's constraint system.


## Module Purpose

The module enables any Groovy class (command objects, POGOs, domain classes) to be validated against a `static constraints` block. It works at both compile time (AST injection) and runtime (trait-based validation).


## Important APIs

### 1. `Validateable` trait (`grails.validation.Validateable`)

The core trait. Apply it to any class to make it validateable. Provides:

| Method | Description |
|--------|-------------|
| `validate()` | Validates the object against its `static constraints` |
| `validate(List fieldsToValidate)` | Validates only the specified fields |
| `validate(Map params)` | Supports `clearErrors` and `inherit` params |
| `validate(Closure... adHocConstraints)` | Validates with additional ad-hoc constraints |
| `getErrors()` / `setErrors()` | Access the Spring `Errors` object |
| `hasErrors()` | Whether the object has validation errors |
| `clearErrors()` | Clears the errors |
| `static getConstraintsMap()` | Returns `Map<String, Constrained>` of evaluated constraints |
| `static defaultNullable()` | Returns `false` — properties are non-nullable by default |

Before validation runs, `BeforeValidateHelper.invokeBeforeValidate()` is called to fire any `beforeValidate()` hooks.

The trait looks up a `ConstraintsEvaluator` bean from the Spring context, falling back to `DefaultConstraintEvaluator` when running outside a container (e.g., in unit tests).


### 2. `ASTValidateableHelper` / `DefaultASTValidateableHelper`

`ASTValidateableHelper` is the interface for compile-time code injection:

```java
void injectValidateableCode(ClassNode classNode, boolean defaultNullable);
```

`DefaultASTValidateableHelper` implements it by injecting at compile time:

- A private static `$constraints` field
- A static initializer that nulls it out (for hot-reload)
- A `getConstraints()` static method that lazily evaluates constraints via `ValidationSupport.getConstrainedPropertiesForClass()`
- `validate()` and `validate(List)` methods that delegate to `ValidationSupport.validateInstance()`

This is used by domain class and command object AST transformers to inject validation without requiring the class to explicitly implement `Validateable`.


### 3. `ValidationSupport` (`org.grails.web.plugins.support.ValidationSupport`)

A static utility class called by AST-injected code at runtime:

- `validateInstance(object, List fieldsToValidate)` — runs validation against the object's `constraints` map, preserving binding errors
- `getConstrainedPropertiesForClass(Class<?> clazz, boolean defaultNullable)` — looks up `ConstraintsEvaluator` from the Spring context and evaluates the class's constraints, returning `Map<String, Constrained>`


### 4. `ConstrainedDelegate` (`grails.validation.ConstrainedDelegate`)

A bridge class that wraps GORM's `grails.gorm.validation.ConstrainedProperty` and implements both `Constrained` (Grace's legacy API) and `ConstrainedProperty` (GORM's API).

Exposes constraint metadata: `isNullable()`, `isBlank()`, `isEmail()`, `getMax()`, `getMin()`, `getInList()`, `getMatches()`, `getMaxSize()`, `getMinSize()`, `isUrl()`, `isPassword()`, `getFormat()`, `getOrder()`, `isDisplay()`, `isEditable()`, etc.


### 5. `DefaultConstrainedDiscovery` (`org.grails.web.plugins.support.DefaultConstrainedDiscovery`)

Implements `ConstrainedDiscovery` (from `grace-core`) for domain classes. It retrieves the GORM `ConstrainedEntity` validator from the mapping context and adapts its `ConstrainedProperty` map to Grace's `Constrained` map.

Registered automatically via `grails.factories`:

```
org.grails.validation.discovery.ConstrainedDiscovery=org.grails.web.plugins.support.DefaultConstrainedDiscovery
```


## How It Works (End-to-End)

```
static constraints = {
    name blank: false, maxSize: 100
    email email: true, nullable: true
}
```

1. At **compile time**, `DefaultASTValidateableHelper.injectValidateableCode()` adds `getConstraints()` and `validate()` methods to the class AST.
2. At **runtime**, calling `validate()` invokes `ValidationSupport.validateInstance()`.
3. `ValidationSupport` calls `getConstrainedPropertiesForClass()`, which looks up the `ConstraintsEvaluator` bean and evaluates the `static constraints` closure.
4. Each `ConstrainedProperty` is wrapped in a `ConstrainedDelegate` and validated against the object's property values.
5. Errors are collected into a `ValidationErrors` (Spring `Errors`) object and stored on the instance.
