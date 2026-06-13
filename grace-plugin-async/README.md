# Grace Plugin Async

`grace-plugin-async` is a plugin module that provides asynchronous, parallel programming capabilities for Grace applications, integrating with various asynchronous libraries and frameworks such as GPars and RxJava.


### Controller Async Support

`AsyncController` - A trait that exposes a `startAsync()` method for raw access to the Servlet 3.x API, allowing controllers to manually control asynchronous request processing.

`AsyncActionResultTransformer` - Handles `Promise` return types from controller actions by managing the Servlet 3.x `AsyncContext` lifecycle, releasing the container thread while background tasks execute.

### Promise Decorators

`AsyncWebRequestPromiseDecorator` - A `PromiseDecorator` that binds the `GrailsWebRequest` to the promise thread, ensuring request attributes are available in background threads.

`TransactionalAsyncTransformUtils` - Utility class for creating `TransactionalPromiseDecorator` instances that wrap async method execution in Spring transactions.

### Web Promises

`WebPromises` - A web-specific promises factory class designed for use in controllers and other web contexts, automatically adding `AsyncWebRequestPromiseDecorator` to the promise factory.

### Auto-Configuration

`ControllersAsyncAutoConfiguration` - Spring Boot auto-configuration that registers the `AsyncActionResultTransformer` bean and the `PromiseFactory` bean.


## Usage in Other Modules

### grace-boot-async

The `grace-boot-async` module depends on `grace-plugin-async` and adds RxJava3 support, serving as the Spring Boot starter for async functionality.

### grace-plugin-events

The `grace-plugin-events` module depends on `grace-plugin-async` to leverage async capabilities for event handling.

### Module Dependencies

The `grace-plugin-async` module itself depends on:

- `grace-plugin-controllers` for controller integration 
- `grace-web-async` for core async web infrastructure 
- `grace-async-core` for the core async framework 


## Notes

The `grace-plugin-async` module provides the core async functionality that enables Grace applications to handle asynchronous request processing through Servlet 3.x async support. It integrates with the external [Grace Async](https://github.com/graceframework/grace-async) library which provides integration with various async libraries like GPars and RxJava.
