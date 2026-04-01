## grace-plugin-dynamic-modules

Grace Dynamic Modules Plugin (GDMP) offer new ways of creating modular and maintainable Grace applications.

A Grace plugin can implement one or more plugin modules to develop and extend Grace applications.
We can use Dynamic Modules to maximize the use of Grace plugins and create an open, shared, and reusable plugins and modules.

Add `dynamic-modules` plugin to your `build.gradle`,

```gradle
apply plugin: "org.graceframework.grace-gsp"

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.graceframework:grace-plugin-dynamic-modules"
}
```
