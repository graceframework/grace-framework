# Grace Web Async

`grace-web-async` is a small, focused module created in Grace 2024.0.0-RC1 that provides the core async web infrastructure — specifically the Grace-aware wrappers around the Servlet 3.0 async API and Spring's async web request handling.


## Module Contents

The module contains exactly two source files:

| Class | Package |
|-------|---------|
| `GrailsAsyncWebRequest` | `grails.web.async` |
| `GrailsAsyncContext` | `org.grails.web.async` |


## Important APIs

### 1. `GrailsAsyncWebRequest`

Extends `GrailsWebRequest` and implements Spring's `AsyncWebRequest` and `AsyncListener` interfaces. It is the Grace-specific implementation of Spring's async web request lifecycle.

Key methods:

| Method | Purpose |
|--------|---------|
| `startAsync()` | Calls `request.startAsync()`, stores the `AsyncContext`, registers itself as an `AsyncListener` |
| `isAsyncStarted()` | Returns true if async has been started |
| `isAsyncComplete()` | Returns true if async has completed (thread-safe via `AtomicBoolean`) |
| `dispatch()` | Dispatches the async context back to the container |
| `addTimeoutHandler(Runnable)` | Registers a handler called on async timeout |
| `addCompletionHandler(Runnable)` | Registers a handler called on async completion |
| `addErrorHandler(Consumer<Throwable>)` | Registers a handler called on async error |
| `lookup(HttpServletRequest)` | Static factory — retrieves the `GrailsAsyncWebRequest` stored on the request attribute |

### 2. `GrailsAsyncContext`

Wraps the raw Servlet `AsyncContext` (via Groovy `@Delegate`) and adds Grace-specific behavior to the async thread lifecycle.

Key overrides:

**`start(Runnable)`** — The most important override. Before running the `Runnable` on the async thread, it:

1. Binds a new `GrailsWebRequest` to the async thread via `WebUtils.storeGrailsWebRequest()`
2. Initializes all `PersistenceContextInterceptor` beans (opens GORM sessions)
3. Runs the `Runnable`
4. Destroys persistence interceptors and clears the web request in a `finally` block

**`complete()`** — Before completing the async context, handles SiteMesh layout rendering if the response is a `GrailsContentBufferingResponse` (i.e., applies the GSP layout decorator to the buffered content).


## How It Works

```
Controller action returns a Promise
  ↓ (AsyncActionResultTransformer in grace-plugin-async)
new GrailsAsyncWebRequest(request, response, servletContext)
  → asyncManager.setAsyncWebRequest(asyncWebRequest)
  → asyncWebRequest.startAsync()
new GrailsAsyncContext(asyncContext, webRequest)
  → asyncContext.start { ... }
      ↓ (on async thread)
      WebUtils.storeGrailsWebRequest(webRequest)   // bind Grace context
      PersistenceContextInterceptor.init()          // open GORM session
      promise.onComplete { ... }                    // run promise callback
      PersistenceContextInterceptor.destroy()       // close GORM session
      WebUtils.clearGrailsWebRequest()              // unbind Grace context
  → asyncContext.complete() or asyncContext.dispatch()
      ↓ (on complete: SiteMesh layout applied if buffered)
```


## Usage in Other Modules

Only one module directly depends on `grace-web-async`:

| Module | Usage |
|--------|-------|
| `grace-plugin-async` | Declares it as `api`. Uses `GrailsAsyncWebRequest` and `GrailsAsyncContext` in `AsyncActionResultTransformer` (handles `Promise` return values from controller actions) and in the `AsyncController` trait's `startAsync()` method. |

The `AsyncController` trait (in `grace-plugin-async`) exposes `startAsync()` to controller classes, which internally creates a `GrailsAsyncWebRequest` and wraps the result in a `GrailsAsyncContext`.

The `AsyncActionResultTransformer` intercepts controller action results that are `Promise` instances and uses `GrailsAsyncWebRequest`/`GrailsAsyncContext` to handle them asynchronously, dispatching back to the container when the promise completes.
