Java Client
===========

Client implementation for publishing metrics from Java applications. 


Instrumenting Your Application
------------------------------

### Add Dependency

Determine the latest version of the Java client in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.arpnetworking.metrics%22%20a%3A%22metrics-client%22).

#### Maven

Add a dependency to your pom:

```xml
<dependency>
    <groupId>com.arpnetworking.metrics</groupId>
    <artifactId>metrics-client</artifactId>
    <version>VERSION</version>
</dependency>
```

You may also need to add the Maven Central repository either to your *~/.m2/settings.xml* or into your project's *pom.xml*.

#### Gradle

Add a dependency to your build.gradle:

    compile group: 'com.arpnetworking.metrics', name: 'metrics-client', version: 'VERSION'

Add the Maven Central Repository into your *build.gradle*:

```groovy
repositories {
    mavenCentral()
}
```

#### SBT

Add a dependency to your project/Build.scala:

```scala
val appDependencies = Seq(
    "com.arpnetworking.metrics" % "metrics-client" % "VERSION"
)
```

The Maven Central repository is included by default.

#### Vertx

Users of Vertx need to depend on the vertx-extra package instead of the metrics-client package.  The vertx-extra provides the necessary wrappers around the standard Java client to work with the shared data model in Vertx.  Special thanks to Gil Markham for contributing this work.  For more information please see [extras/vertx-extra/README.md](../extras/vertx-extra/README.md).

### MetricsFactory

Your application should instantiate a single instance of MetricsFactory.  For example: 

```java
final MetricsFactory metricsFactory = new MetricsFactory.Builder()
    .setSinks(Collections.singletonList(
        new TsdQueryLogSink.Builder()
            .setPath("/var/logs")
            .setName("myapp-query")
            .build()));
    .build();
```

### Metrics

The MetricsFactory is used to create a Metrics instance for each unit of work.  For example:
 
 ```java
final Metrics metrics = metricsFactory.create();
```

Counters, timers and gauges are recorded against a metrics instance which must be closed at the end of a unit of work.  After the Metrics instance is closed no further measurements may be recorded against that instance.

```java
metrics.incrementCounter("foo");
metrics.startTimer("bar");
// Do something that is being timed
metrics.stopTimer("bar");
metrics.setGauge("temperature", 21.7);
metrics.close();
```

### Injection
 
Passing the MetricsFactory instance around your code is far from ideal.  We strongly recommend a combination of two techniques to keep metrics from polluting your interfaces.  First, use a dependency injection framework like Spring or Guice to create your TsdMetricsFactory instance and inject it into the parts of your code that initiate units of work.

Next, the unit of work entry points can leverage thread local storage to distribute the Metrics instance for each unit of work transparently throughout your codebase.  Log4J calls this a Mapped Diagnostics Context (MDC) and it is also available in LogBack although you will have to create a static thread-safe store (read ConcurrentMap) of Metrics instances since the LogBack implementation is limited to Strings.

One important note, if your unit of work leverages additional worker threads you need to pass the Metrics instance from the parent thread's MDC into the child thread's MDC.

### Counters

Surprisingly, counters are probably more difficult to use and interpret than timers and gauges.  In the simplest case you can just starting counting, for example, iterations of a loop:

```java
for (String s : listOfStrings) {
    metrics.incrementCounter("strings");
    ...
}
```

However, what happens if listOfString is empty? Then no sample is recorded. To always record a sample the counter should be reset before the loop is executed:

```java
metrics.resetCounter("strings");
for (String s : listOfStrings) {
    metrics.incrementCounter("strings");
    ...
}
```

Next, if the loop is executed multiple times:

```java
for (List<String> listOfString : listOfListOfStrings) {
    metrics.resetCounter("strings");
    for (String s : listOfStrings) {
        metrics.incrementCounter("s");
        ...
    }
}
```

The above code will produce a number of samples equal to the size of listOfListOfStrings (including no samples if listOfListOfStrings is empty).  If you move the resetCounter call outside the outer loop the code always produces a single sample (even if listOfListOfStrings is empty).  There is a significant difference between counting the total number of strings and the number of strings per list; especially, when computing and analyzing statistics such as percentiles. 

Finally, if the loop is being executed concurrently for the same unit of work, that is for the same Metrics instance, then you could use a Counter object:

```java
final Counter counter = metrics.createCounter("strings");
for (String s : listOfStrings) {
    counter.increment();
    ...
}
```

The Counter object example extends in a similar way for nested loops.

### Timers

Timers are very easy to use. The only note worth making is that when using timers - either procedural or via objects - do not forget to stop/close the timer!  If you fail to do this the client will log a warning and suppress any unstopped/unclosed samples.

The timer object allows a timer sample to be detached from the Metrics instance.  For example:  

```java
public void run() {
    final Timer t = metrics.createTimer("operation");
    // Perform your operation
    t.stop();
}
```

This is very useful in a concurrent system when executing and thus measuring the same event multiple times concurrently.  The one caveat is to ensure the timer objects are stopped/closed before the Metrics instance is closed.  Failing to do so will log a warning and suppress any samples stopped/closed after the Metrics instance is closed.
 
### Gauges

Gauges are the simplest metric to record.  Samples for a gauge represent spot measurements. For example, the length of a queue or the number of active threads in a thread pool.  Gauges are often used in separate units of work to measure the state of system resources, for example the row count in a database table.  However, gauges are also useful in existing units of work, for example recording the memory in use at the beginning and end of each service request.

### Closeable

The Metrics interface as well as the Timer and Counter interfaces extend [Closeable](http://docs.oracle.com/javase/7/docs/api/java/io/Closeable.html) which allows you to use Java 7's try-with-resources pattern.  For example:

```java
try (final Timer timer = metrics.createTimer("timer")) {
    // Time unsafe operation (e.g. this may throw)
}
```

The timer instance created and started in the try statement is automatically closed (e.g. stopped) when the try is exited either by completion of the block or by throwing an exception. 

Building
--------

Prerequisites:
* [JDK7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Maven 3.0.5+](http://maven.apache.org/download.cgi)

Building:

    client-java> mvn package

To use the local version you must first install it locally:

    client-java> mvn install

You can determine the version of the local build from the pom file.  Using the local version is intended only for testing or development.

You may also need to add the local repository to your build in order to pick-up the local version:

* Maven - Included by default.
* Gradle - Add *mavenLocal()* to *build.gradle* in the *repositories* block.
* SBT - Add *resolvers += Resolver.mavenLocal* into *project/plugins.sbt*.

License
-------

Published under Apache Software License 2.0, see LICENSE
