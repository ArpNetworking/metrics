Jvm Extra
===========

A runnable to collect the various JVM metrics.


Setup
-----

### Building

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven 3.2.5+](http://maven.apache.org/download.cgi)

Building:
    extras/jvm-extra> mvn package


### Instrumenting Your Application

#### Add Dependency

Install the current version locally:

    extras/jvm-extra> mvn install

Using the local version is intended only for testing or development.

##### Maven

Add a dependency to your pom:

```xml
<dependency>
    <groupId>com.arpnetworking.metrics.extras</groupId>
    <artifactId>jvm-extra</artifactId>
    <version>VERSION</version>
</dependency>
```

Add the Maven Central repository either to your ~/.m2/settings.xml or into your project's pom. Alternatively, if using the local version no changes are necessary as the local repository is enabled by default.

##### Gradle

Add a dependency to your build.gradle:

    compile group: 'com.arpnetworking.metrics.extras', name: 'jvm-extra', version: 'VERSION'

Add at least one of the Maven Central Repository and/or Local Repository into build.gradle:

    mavenCentral()
    mavenLocal()

##### Play

Add a dependency to your project/Build.scala:

```scala
val appDependencies = Seq(
    ...
    "com.arpnetworking.metrics.extras" % "jvm-extra" % "VERSION"
    ...
)
```

The Maven Central repository is included by default.  Alternatively, if using the local version add the local repository into project/plugins.sbt:

    resolvers += Resolver.mavenLocal

Usage
-----

The default usage will collect all JVM metrics and will log and swallow any exceptions that may occur during the collection. Currently, we collect metrics for garbage collection, threads and heap and non-heap memory usage.

##### Building a runnable

```java
JvmMetricsRunnable.Builder
    .newInstance()
    .setMetricsFactory(_metricsFactory)
    .build();
```

If you want any exception that may occur during the execution of this to propagate to the caller, you will need to explicitly set the swallowException attribute to false.
Below, is an example.

```java
JvmMetricsRunnable.Builder
    .newInstance()
    .setMetricsFactory(_metricsFactory)
    .setSwallowException(false)
    .build();
```

Depending on what metrics you need to collect, you may enable or disable individual collectors. For example, to not
collect any metrics, you can specify the all collectors to be false, as shown below:

```java
JvmMetricsRunnable.Builder()
    .newInstance()
    .setMetricsFactory(_metricsFactory)
    .setCollectGarbageCollectionMetrics(false) //It is true by default. Need not specify this.
    .setCollectThreadMetrics(false) //It is true by default. Need not specify this.
    .setCollectNonHeapMemoryMetrics(false) //It is true by default. Need not specify this.
    .setCollectHeapMemoryMetrics(false) //It is true by default. Need not specify this.
    .build();
```

Please refer to the Java metrics client documentation [client-java/README.md](../../client-java/README.md) for more information on using Metrics and MetricsFactory.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2015
