## Grace GSP

This subproject is required for all Grace applications and plugins that require GSP processing.

``` gradle
apply plugin: "org.graceframework.grace-gsp"
```

It is typical of standard Grace application to use this in conjunction with `grace-web` as in the following example:

``` gradle
apply plugin: "org.graceframework.grace-web"
apply plugin: "org.graceframework.grace-gsp"
```

Dependencies
-----
To see what additional subprojects will be included with this, you can view this project's [build.gradle](https://github.com/graceframework/grace-framework/blob/2022.2.x/grace-gsp/build.gradle)
