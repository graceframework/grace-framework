# Grace Plugin Events

The `grace-plugin-events` module is a Grace Framework plugin that provides an event-driven architecture with an EventBus abstraction, integrating Grace with asynchronous libraries like GPars and RxJava, and bridging Spring and GORM events into a unified event system.


## Module Overview

The `grace-plugin-events` module depends on core modules including `grace-core`, `grace-plugin-api`, and `grace-plugin-async`. 

It has API dependencies on Grace Events libraries (core, spring, transform) and Spring Boot autoconfigure.

## Important APIs

### EventBusGrailsPlugin

The main plugin class `EventBusGrailsPlugin` extends `Plugin` and integrates the event system into Grace.

### EventsAutoConfiguration

Spring Boot auto-configuration class that provides essential event infrastructure beans.

**Provided Beans:**

- `eventBus` - The core EventBus instance created via EventBusFactoryBean
- `gormDispatchEventRegistrar` - Registers GORM event dispatchers with the EventBus
- `springEventTranslator` - Translates Spring ApplicationEvents to EventBus events (conditional on `grails.events.spring=true`)

### SpringEventTranslator

Translates Spring ApplicationEvents into EventBus events with logical naming conventions.

**Event Naming Logic:**

- GORM events (org.grails.datastore.*) → `gorm:logicalName`
- Spring events (org.springframework.*) → `spring:logicalName`
- Other events → `grails:logicalName`

The translator excludes `ContextClosedEvent` to prevent issues during shutdown.

## Usage in Other Modules

### Boot Modules

- **grace-boot-events**: Uses events as an API dependency and adds RxJava3 implementation support

### Async Integration

The events module is configured to run after `ControllersAsyncAutoConfiguration`, ensuring proper integration with async controller functionality.

## Notes

**Refactoring History:**

In the 2024.x release, the events plugin was merged into the framework from a separate repository as part of the core plugins consolidation effort.

**Event Bus Abstraction:**

The module replaces the previous Reactor 2.x-based implementation (which is no longer maintained) with a new EventBus abstraction that supports multiple asynchronous frameworks like GPars and RxJava.
