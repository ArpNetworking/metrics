Vertx Extra
===========

Vertx compatible wrapper around the metrics Java client. 


Setup
-----

### Building

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven 3.2.5+](http://maven.apache.org/download.cgi)

Building:
    extras/vertx-extra> mvn package


### Instrumenting Your Application

#### Add Dependency

Determine the latest version of the Vertx wrapper in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.arpnetworking.metrics.extras%22%20a%3A%22vertx-extra%22).  Alternatively, install the current version locally:

    extras/vertx-extra> mvn install

Using the local version is intended only for testing or development.

##### Maven

Add a dependency to your pom:

```xml
<dependency>
    <groupId>com.arpnetworking.metrics.extras</groupId>
    <artifactId>vertx-extra</artifactId>
    <version>VERSION</version>
</dependency>
```

Add the Maven Central repository either to your ~/.m2/settings.xml or into your project's pom.  Alternatively, if using the local version no changes are necessary as the local repository is enabled by default.

##### Gradle

Add a dependency to your build.gradle:

    compile group: 'com.arpnetworking.metrics.extras', name: 'vertx-extra', version: 'VERSION'

Add at least one of the Maven Central Repository and/or Local Repository into build.gradle:
 
    mavenCentral()
    mavenLocal()

##### Play

Add a dependency to your project/Build.scala:

```scala
val appDependencies = Seq(
    ...
    "com.arpnetworking.metrics.extras" % "vertx-extra" % "VERSION"
    ...
)
```

The Maven Central repository is included by default.  Alternatively, if using the local version add the local repository into project/plugins.sbt:

    resolvers += Resolver.mavenLocal

#### Usage

To create a shareable Metrics instance in Vertx wrap the Metrics instance from the MetricsFactory in a SharedMetrics instance.  For example:

```java
final MetricsFactory metricsFactory = new MetricsFactory.Builder()
    .setSinks(Collections.singletonList(
        new TsdQueryLogSink.Builder()
            .setPath("/var/logs")
            .setName("myapp-query")
            .build()));
    .build();
final Metrics metrics = new SharedMetrics(metricsFactory.create());
```

To create a shareable MetricsFactory instance in Vertx wrap a MetricsFactory instance in a SharedMetrics instance.  For example:

```java
final MetricsFactory shareableMetricsFactory = new SharedMetricsFactory(
    new MetricsFactory.Builder()
        .setSinks(Collections.singletonList(
            new TsdQueryLogSink.Builder()
                .setPath("/var/logs")
                .setName("myapp-query")
                .build()));
        .build());
```

If you do not want to use a shared MetricsFactory instance, but still have multiple verticles write to the same sink, you will need to implement the abstract class SinkVerticle. For example:

```java
public final class MySinkVerticle extends SinkVerticle {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Sink> createSinks() {
        final Sink sink = Arrays.asList(
            new TsdQueryLogSink.Builder()
                .setPath("/var/logs")
                .setName("myapp-query")
                .build());
        return ImmutableList.of(sink);
    }
}
```

Once you have implemented the SinkVerticle, you will need to define a MetricsFactory instance that communicates with this verticle. This MetricsFactory instance will wrap a Sink instance of type EventBusSink. Example:

```java
final Sink sink = new EventBusSink.Builder()
        .setEventBus(vertx.eventBus())
        .setSinkAddress(sinkAddress) //This sink address has to be the same as the one that's configured in the SinkVerticle. The default address set is "metrics.sink.default".
        .build();
final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder()
    .setSinks(Collections.singletonList(sink))
    .build();
```

Please refer to the Java metrics client documentation [client-java/README.md](../client-java/README.md) for more information on using Metrics and MetricsFactory.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2014
