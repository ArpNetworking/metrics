Yammer Extra
============

Extension for clients migrating from Yammer that allows migration to our client library while retaining publication to Yammer.


Setup
-----

### Building

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven 3.2.5+](http://maven.apache.org/download.cgi)

Building:
    extras/yammer-extra> mvn package


### Add Dependency

Determine the latest version of the Yammer extra in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.arpnetworking.metrics.extras%22%20a%3A%22yammer-extra%22).  Alternatively, install the current version locally:

    extras/yammer-extra> mvn install

Using the local version is intended only for testing or development.

#### Maven

Add a dependency to your pom:

```xml
<dependency>
    <groupId>com.arpnetworking.metrics.extras</groupId>
    <artifactId>yammer-extra</artifactId>
    <version>VERSION</version>
</dependency>
```

Add the Maven Central repository either to your ~/.m2/settings.xml or into your project's pom.  Alternatively, if using the local version no changes are necessary as the local repository is enabled by default.

#### Gradle

Add a dependency to your build.gradle:

    compile group: 'com.arpnetworking.metrics.extras', name: 'yammer-extra', version: 'VERSION'

Add at least one of the Maven Central Repository and/or Local Repository into build.gradle:
 
    mavenCentral()
    mavenLocal()

#### Play

Add a dependency to your project/Build.scala:

```scala
val appDependencies = Seq(
    ...
    "com.arpnetworking.metrics.extras" % "yammer-extra" % "VERSION"
    ...
)
```

The Maven Central repository is included by default.  Alternatively, if using the local version add the local repository into project/plugins.sbt:

    resolvers += Resolver.mavenLocal

### Publishing to Yammer

When your application instantiates the MetricsFactory you should also supply an instance of YammerMetricsSink as part of the sinks collection.  For example: 

```java
final MetricsFactory metricsFactory = new MetricsFactory.Builder()
    .setSinks(Arrays.asList(
        new TsdQueryLogSink.Builder()
            .setPath("/var/logs")
            .setName("myapp-query")
            .build(),
        // Additional Yammer Sink:
        new YammerMetricsSink.Builder()
            .setMetricsRegistry(metricsRegistry)
            .build()));
    .build();
```

The Yammer MetricsRegistry on YammerMetricsSink.Builder is optional and defaults to Metrics.defaultRegistry() if not set (or set to null).

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2014
