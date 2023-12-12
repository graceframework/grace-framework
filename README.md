Grace Framework
===

[Grace](https://graceframework.org/) is a framework used to build web applications with the [Groovy](https://groovy-lang.org/) programming language. The core framework is very extensible and there are numerous [plugins](https://plugins.graceframework.org/) available that provide easy integration of add-on features.

Getting Started
---

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grace distribution.

To install Grace, visit https://graceframework.org/download.html and download the version you would like to use. Set a `GRACE_HOME` environment variable to point to the root of the extracted download and add `GRACE_HOME/bin` to your executable `PATH`. Then in a shell, type the following:

	grace create-app sampleapp
	cd sampleapp
	grace run-app

To build Grace, clone this GitHub repository and execute the install Gradle target:

    git clone https://github.com/graceframework/grace-core.git
    cd grace-core
    ./gradlew install

If you encounter out of memory errors when trying to run the install target, try adjusting Gradle build settings. For example:

    export GRADLE_OPTS="-Xmx2G -Xms2G -XX:NewSize=512m -XX:MaxNewSize=512m"

Performing a Release
---

See [RELEASE.md](RELEASE.md).

License
---

Grace is licensed under the terms of the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
