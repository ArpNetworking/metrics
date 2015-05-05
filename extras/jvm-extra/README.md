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
    .setMetricsFactory(metricsFactory)
    .setSwallowException(false)
    .build();
```

Depending on what metrics you need to collect, you may enable or disable individual collectors. For example, to not
collect any metrics, you can specify the all collectors to be false, as shown below:

```java
JvmMetricsRunnable.Builder()
    .newInstance()
    .setMetricsFactory(metricsFactory)
    .setCollectGarbageCollectionMetrics(false) //It is true by default. Need not specify this.
    .setCollectThreadMetrics(false) //It is true by default. Need not specify this.
    .setCollectNonHeapMemoryMetrics(false) //It is true by default. Need not specify this.
    .setCollectHeapMemoryMetrics(false) //It is true by default. Need not specify this.
    .build();
```

Integration
-----------

Following are examples of scheduling the JvmMetricsRunnable on various platforms.

##### Using JAVA ScheduledExecutorService
Using [ScheduledExecutorService](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html), you will only need to schedule the JvmMetricsRunnable with an initial delay and a collection interval.

```java
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.Sink;
import com.arpnetworking.metrics.impl.TsdMetricsFactory;
import com.arpnetworking.metrics.impl.TsdQueryLogSink;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JvmMetricsCollector {

    public static final void main(String[] args) {
        // Pass the path for the sink as the first arg
        final Sink sink = new TsdQueryLogSink.Builder()
                .setPath(args[0])
                .build();
        final MetricsFactory metricsFactory = new TsdMetricsFactory.Builder()
                .setSinks(Arrays.asList(sink))
                .build();
        final Runnable runnable = new JvmMetricsRunnable.Builder()
                .setMetricsFactory(metricsFactory)
                .build();
        final ScheduledExecutorService jvmMetricsCollector = Executors.newSingleThreadScheduledExecutor();
        jvmMetricsCollector.scheduleAtFixedRate(
                runnable,
                0, //Initial delay in milliseconds
                1000, //Collection interval in milliseconds
                TimeUnit.MILLISECONDS);
        // Let the collector run for a while
        Thread.sleep(60000);
        // Shutdown the collector
        jvmMetricsCollector.shutdown();
    }
}
```

To shutdown the executor, simply call the shutdown method on the ScheduledExecutorService instance, as shown below.
Once the executor is shutdown, it cannot be restarted. To restart collecting metrics, another instance of the ScheduledExecutorService should be created.

```java
jvmMetricsCollector.shutdown();
```

##### Using AKKA Actor
To create an actor that extends from the UntypedActor class, you will need to schedule the "collect" message on the actor with an initial delay and a collection interval. Overriding the preStart hook to do the scheduling ensures that the scheduling happens as soon as the actor starts. Do not forget to cancel the scheduling when the actor stops. In the example below, we use a string message "COLLECT" to trigger the JVM metrics collection by the actor.

```java
import akka.actor.UntypedActor;
import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.metrics.jvm.JvmMetricsRunnable;
import scala.concurrent.duration.FiniteDuration;
import java.util.concurrent.TimeUnit;

public final class JvmMetricsCollector extends UntypedActor {

    public JvmMetricsCollector(
        final Period interval,
        final MetricsFactory metricsFactory) {
        interval = FiniteDuration.create(
            interval.toStandardDuration().getMillis(),
            TimeUnit.MILLISECONDS);
        jvmMetricsRunnable = new JvmMetricsRunnable.Builder()
            .setMetricsFactory(metricsFactory)
            .setSwallowException(false) // Relying on the default akka supervisor strategy here.
            .build();
    }

    @Override
    public void preStart() {
        cancellable = getContext().system().scheduler().schedule(
            FiniteDuration.Zero(), //Initial delay
            FiniteDuration.create(1000, TimeUnit.MILLISECONDS), //Collection interval
            self(),
            COLLECT_MESSAGE, //Message that gets the actor to start collecting
            getContext().system().dispatcher(),
            self());
    }

    @Override
    public void postStop() {
        cancellable.cancel();
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (COLLECT_MESSAGE.equals(message)) {
            jvmMetricsRunnable.run();
        } else {
            unhandled(message);
        }
    }

    private Cancellable cancellable;
    private final FiniteDuration interval;
    private final Runnable jvmMetricsRunnable;

    private static final String COLLECT_MESSAGE = "COLLECT";
}
```

Please refer to the Java metrics client documentation [client-java/README.md](../../client-java/README.md) for more information on using Metrics and MetricsFactory.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2015
