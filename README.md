Grace Framework
===

[Grace](https://github.com/graceframework/grace-framework) is a fork of Grails 5.1.x that started development in early 2022, it's a powerful and open-source One-Person web framework used to build enterprise-grade [Spring Boot](https://spring.io/projects/spring-boot/) applications with the powerfull [Groovy](https://groovy-lang.org/) programming language. The core framework is very extensible and there are numerous [Plugins](https://github.com/grace-plugins/) available that provide easy integration of add-on features.

Getting Started
---

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grace distribution.

To install Grace, visit https://github.com/graceframework/grace-framework/releases and download the version you would like to use. Set a `GRACE_HOME` environment variable to point to the root of the extracted download and add `GRACE_HOME/bin` to your executable `PATH`. Then in a shell, type the following:

```bash
	grace create-app com.example.blog
	cd blog
	grace run-app
```

To build Grace, clone this GitHub repository and execute the install Gradle target:

```bash
    git clone https://github.com/graceframework/grace-framework.git
    cd grace-framework
    ./gradlew pTML zipDist
```

If you encounter out of memory errors when trying to run the install target, try adjusting Gradle build settings. For example:

```bash
    export GRADLE_OPTS="-Xmx2G -Xms2G -XX:NewSize=512m -XX:MaxNewSize=512m"
```

Performing a Release
---

See [RELEASE.md](RELEASE.md).

License
---

Grace is licensed under the terms of the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
