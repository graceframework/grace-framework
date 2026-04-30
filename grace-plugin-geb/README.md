# Grace Geb Plugin

Add Geb Functional Testing for Grace applications.

This plugin just provides the Geb dependencies and a `create-functional-test` command for generating Geb tests in a Grace app. For further reference please see the [Apace Geb documentation](https://groovy.apache.org/geb/)

Add `geb` plugin to your `build.gradle`,

```
dependencies {
    implementation "org.graceframework:grace-plugin-geb"
}
```

Then run the command to configure it,

```
grace geb:install
```
