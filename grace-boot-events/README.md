# Grace Boot Events

`grace-boot-events` is a Spring Boot auto-configuration module that provides Event Bus integration for Grace applications.

> [!NOTE]
> It was created as part of the 2024.x modularization effort to make Grace's event functionality more focused and better integrated with Spring Boot's module structure.

The module depends on `grace-plugin-events` (which contains the core event functionality) and includes RxJava3 by default, Spring, and transform integration libraries from the Grace Events ecosystem.

### EventBus Abstraction

Grace Events introduces an `EventBus` abstraction that replaces the previous Reactor 2.x-based implementation (which is no longer maintained). Like the `PromiseFactory` notion, there are implementations of the `EventBus` interface for common asynchronous frameworks like GPars and RxJava.

### Auto-Configuration

`EventsAutoConfiguration` - Spring Boot auto-configuration that registers the following beans:

- `EventBus` - The core event bus bean created via `EventBusFactoryBean`
- `GormDispatcherRegistrar` - Integrates GORM events with the event bus
- `SpringEventTranslator` - Translates Spring ApplicationEvents into Grace events (conditional on `grails.events.spring=true`)
