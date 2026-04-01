## Grace Plugin Events

[Grace Events](https://github.com/graceframework/grace-events) introduces a new Events API that replaces the previous implementation that was based on Reactor 2.x (which is no longer maintained and deprecated), which integrate Grace with various asynchronous libraries and frameworks such as GPars and RxJava.

The Events framework introduces a new `EventBus` abstraction.
Like the `PromiseFactory` notion, there are implementations of the `EventBus` interface for common asynchronous frameworks like GPars and RxJava.

To use the Events abstraction you should add a dependency on the `events` plugin to your `build.gradle` file:

```
implementation "org.graceframework:grace-plugin-events"
```
