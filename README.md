Metrics
=======

Provides a platform for recording, aggregating, publishing and accessing metrics produced by your host system, shared resource, or application. 


Summary
-------

Please refer to the README under each project for information on how to build and run that component.

|Project           |Build System                                                                                        |Documentation                            |Prerequisites                          |
|------------------|----------------------------------------------------------------------------------------------------|-----------------------------------------|---------------------------------------|
|Java Client       |[Maven](http://maven.apache.org/)                                                                   |[README](client-java/README.md)          |JDK7, Maven 3.0.5+,                    |
|Ruby Client       |[Gem](https://rubygems.org/)                                                                        |[README](client-ruby/README.md)          |RVM (w/ Ruby 1.9.3+)                   |
|Node Client       |[Npm](https://www.npmjs.org/)                                                                       |[README](client-nodejs/README.md)        |NodeJs 0.10.26+, Typescript 1.0.1.0+   |
|Tsd Core          |[Gradle](http://www.gradle.org/)                                                                    |[README](tsd/README.md)                  |JDK7, Protobuf 2.5.0                   |
|Tsd Aggregator    |[Gradle](http://www.gradle.org/)                                                                    |See above.                               |See above.                             |
|Cluster Aggregator|[Gradle](http://www.gradle.org/)                                                                    |See above.                               |See above.                             |
|ReMet Proxy       |[Play](http://www.playframework.com/)/[SBT](http://www.scala-sbt.org/)                              |[README](remet-proxy/README.md)          |JDK7, Play 2.3.0                       |
|ReMet Gui         |[Play](http://www.playframework.com/)/[SBT](http://www.scala-sbt.org/)                              |[README](remet-gui/README.md)            |JDK7, Play 2.3.0                       |
|CollectD Extra    |[Autoconf](https://www.gnu.org/software/autoconf/)+[Automake](http://www.gnu.org/software/automake/)|[README](extras/collectd-extra/README.md)|Autoconf, Automake, C-compiler         |
|Vertx Extra       |[Maven](http://maven.apache.org/)                                                                   |[README](extras/vertx-extra/README.md)   |JDK7, Maven 3.0.5+                     |
|Yammer Extra      |[Maven](http://maven.apache.org/)                                                                   |[README](extras/yammer-extra/README.md)  |JDK7, Maven 3.0.5+                     |
|JVM Extra         |[Maven](http://maven.apache.org/)                                                                   |[README](extras/jvm-extra/README.md)     |JDK7, Maven 3.0.5+                     |

### Prerequisites

* [JDK7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Maven 3.2.5+](http://maven.apache.org/download.cgi)
* [Protobuf 2.5.0](https://code.google.com/p/protobuf/downloads/list)
* [Play 2.4.0](http://www.playframework.com/download)
* [RVM](https://rvm.io/rvm/install) (w/ Ruby 1.9.3+)
* [NodeJs 0.10.26+](http://nodejs.org/download/)
* [Typescript 1.0.1.0+](http://www.typescriptlang.org/#Download)
* [Autoconf](https://www.gnu.org/software/autoconf/)
* [Automake](http://www.gnu.org/software/automake/)
* [GCC] (http://gcc.gnu.org/)

Instrumenting Your Application
------------------------------

The first step to integrating metrics with your application is to incorporate one of our client libraries.  If your application language is not listed below or the libraries are not compatible with your use case, please open an issue.  Alternatively, you may build your own client library by writing a query log file in a compatible format.  If you are considering this option, please contact us to discuss how to make your work available to other users of metrics.

For more information please view client-specific documentation:
* [Java](client-java/README.md)
* [NodeJs](client-nodejs/README.md)
* [Ruby](client-ruby/README.md)

At the completion of this step your application will generate a query log file with a line for each unit of work it processes.
 
### Application Hosts

On each application host you should install, configure and run both Tsd Aggregator and ReMet Proxy.  The Tsd Aggregator is responsible for generating statistics (metrics aggregated over a period of time) per host.  The ReMet Proxy is streams one-second aggregates to the telemetry web portal, ReMet Gui.  For information on compiling and running please refer to each project's documentation:

* Tsd Aggregator: [tsd/README.md](tsd/README.md)
* ReMet Proxy: [remet-proxy/README.md](remet-proxy/README.md)

### Telemetry

The telemetry web portal, ReMet Gui, may be installed on one or more dedicated or shared hosts.  The web portal connects to the ReMet Proxy running on each of your application hosts and provides a near real time stream of fine grained statistics.  For information on compiling and running please refer to each project's documentation: [remet-gui/README.md](remet-gui/README.md)

### Cluster Aggregation

*Work In Progress*

### System Metrics

We use CollectD to collect system metrics and output them in a compatible format with a custom plugin.  To setup CollectD with our plug in please see [extras/collectd-extra/README.md](extras/collectd-extra/README.md).

Tenants
-------

### Availability

The primary purpose of this metrics project is to determine the state of a system and to help engineers determine if a problem exists and to pinpoint the cause.  The metrics system itself or the infrastructure it relies on cannot be assumed to be any more reliable than the applications that it reports on.  However, it can and should be architected in a way to degrade gracefully in the presence of bugs, outages, and large scale events (e.g. earthquake, power outage, cyber attack, etc.).  Graceful degradation provides end users with access to data at decreasing levels of convenience with fewer moving parts at each successive level without significantly increasing code complexity. 

### Targeted

There are several uses for metrics such as: i) alerts, ii) performance testing, and iii) impact analysis.  These uses present varying requirements for the granularity and accuracy - the scope - as well as the responsiveness and freshness - the timeliness - of the input data.

#### Scope

The granularity ranges from the raw samples generated by the application to aggregations of samples over increasing periods of time (1 second, 5 minutes, 1 hour, etc.).  The accuracy refers to how closely the aggregated values represent the true value (as computed if one were given unlimited time and resources).  Metrics do not need to be 100% accurate for all applications and it is a reasonable trade-off to make in many cases.  For example, sampling data or aggregating using statistical methods (e.g. percentiles from histograms or wavelets) are examples which result is less than 100% accuracy.

#### Timeliness

The responsiveness refers to how quickly end users can access data; options range from near real-time telemetry (e.g. responds in milliseconds) to complex mutli-variable analysis jobs (e.g. responds in minutes to hours; depending on the data set and query).  The freshness refers to the age of the most recent data available in the target. Some examples along these two dimensions include:
* Alerts: data must be reasonably responsive (e.g. seconds) and reasonably fresh (e.g. minutes) 
* Telemetry: data must be very responsive (e.g. milliseconds) and very fresh (e.g. seconds). 
* Analysis: data may have relaxed responsiveness (e.g. minutes) and relaxed freshness (e.g. minutes to hours).

### Scalability

Every application should produce metrics; lots of metrics.  The metrics data flow will be among the largest in the organization.  The metrics system must scale to meet the needs of the organization as well as individual applications now and for the foreseeable future.  

### Lossless

You can never go back in time and re-experience an event.  In the same way, if you don’t capture a measurement, it can never be recreated.  The metrics system supports capturing all information.  Aggregations are useful in many cases; however, sometimes it’s necessary to view raw records.  The metrics system should provide access to this raw data in an appropriate manner.

Architecture
------------

### Client Libraries

Thin libraries written for Java, Ruby and NodeJS applications that allow application software engineers to easily collect counters, timers and gauges for each unit of work the application performs and output that data stream in a supported format.

### Query File

The file contains one data entry per line in a supported format.  The file may be consumed by other logging agents for detailed offline analysis.  This file contains the only record of all samples generated by an application.  For a more information about the format please see: [doc/QUERY_LOG_FORMAT.md](doc/QUERY_LOG_FORMAT.md)

### TSD Aggregator

Daemon that consumes one or more query files and produces configurable statistics over configurable time periods (e.g. 1min, 5min, 1hour, etc.).  The aggregates are published to configured data sinks; in particular, to the ReMet Proxy and Cluster Aggregator.

### Cluster Aggregator

Shared cluster of servers that aggregate data from TSD Aggregator daemons into cluster level aggregates.  The data may be split and aggregated across several configurable dimensions.  Common use cases include data center, country, and client.  The cluster aggregator uses Redis as a temporary store for metrics to support cluster aggregation host failover.

### ReMet Proxy

Daemon that acts as a data sink for 1-second aggregated data from TSD Aggregator and makes it available as a stream to the ReMet Gui web client.

### ReMet Gui

Web front-end that allows users to connect directly to a 1-second aggregate feed of data from any number of hosts (limited by local bandwidth and computing power).

### Collectd

Pluggable open source daemon ([https://collectd.org/](https://collectd.org/)) which gathers statistics about system resources and writes data to a file in a supported format.

### Diagram

![Architecture Diagram](/doc/architecture.png)

Details
-------

### Producing Metrics

Although all metrics can be represented as a key and value in time, there are three conceptually different types of metrics an application can produce: counters, gauges and timers. 

Counters are used to track incremental events; for example, the number of calls to a method or service endpoint, or iterative operations (e.g. loops, recursion, etc.). 

Gauges are used to track point in time measurements; for example, the number of rows in a database table, the length of a queue, or the number of client connections to a web server.  The difference between a counter and a gauge is that a counter builds each sample value over time while a gauge performs one or more spot measurements.

Timers are sampled over a period of time; from the start of the timer sample to the end of the timer sample.  Common applications include method or service call latency, database calls, serialization/deserialization and so forth. 

The different types of metrics allow clients to write code that is more natural to read and understand and also adds context to each sample that suggests what kind of transformations make sense (by default).

For all three types of metrics the application produces a series of samples. For example, in a five minute span an "order service" endpoint is invoked 100 times:
* There will be 100 latency samples published for the timer around that endpoint.
* There will be 100 counter samples each with a value indicating the number of items in each order request.
* There will be 5 gauge samples, one taken each minute, each counting the number of orders in a "pending" state.  

The samples are collected on a unit of work basis.  The unit of work is any process or step in a process as defined by the application; some examples include: servicing a HTTP request, performing a scheduled background task, a step in a workflow, etc..  Even a background thread/process reporting on the state of the system can be considered to perform a unit of work.  The unit of work may be annotated with arbitrary key-value pairs that describe the context of the samples attached to it.  At the conclusion of the unit of work the samples with their context are published. 

For example, the service request annotates the request with the browser id and browser version of the client.  All samples collected into that unit of work such as the service call latency and database queries are automatically annotated with those key-value pairs decoupling the annotation and collection of samples.

The unit of work model does not prevent or in any way hinder collection of samples not tied to any "natural" unit of work.  For example, each minute the application measures the number of orders in the "pending" state as a gauge in a separate unit of work.  The application therefore produces five samples for over a five-minute period.  Much like the differentiation between counters, gauges and timers the unit of work exists to simplify the interaction for clients.

### Push vs Pull

The samples collected for each unit of work are pushed out of the application asynchronously at the close of the unit of work.  The same philosophy is applied to Tsd Aggregatgor pushing aggregates to data sinks including the Cluster Aggregator.  There are several advantages to push over pull:

* Pulling raw samples periodically can represent a non-trivial instantaneous load on the application to serialize and transmit the sample data even to a local destination.  Pushing the samples incrementally amortizes this cost over time.
* Supporting multiple sinks magnifies the impact and adds complexity to ensure each client pulling data gets the same complete set of samples which is important to ensure a consistent view of data across sinks.

One alternative to pulling all samples is to aggregate samples in the application and pull the aggregates.  However, under serveral use cases this leads to similar problems as pulling samples:
* In the presence of even a reasonable number of dimensional annotations with moderately sized domains the resulting aggregations may be proportional to the number of samples with a small enough period and/or low enough load.
* Multiple sources pulling aggregates are not unlikely to get the same data.
* Catastrophic failure of the application will result in non-trivial loss of data in this scheme versus incrementally pushing samples.

### Transformations

For most cases, raw samples are too granular to provide insight into a system as a whole.  The samples published by an application are transformed into aggregates as defined by configurable sets of operators and periods.  The operators are the statistic calculators such as mean, sum, count, min, max, median, and 90th percentile to name a few.  The periods are the time ranges over which the samples are bucketed (e.g. 1 second, 5 minutes, 1 hour).  The aggregates follow by applying the statistical operator to each period’s bucket of samples.

There exists a default configuration of operators and periods for each type of metric (e.g. counter, gauge and timer).  This is useful for clients because it rarely makes sense to compute the sum of a timer but it is often useful for a counter.  This default configuration may be augmented by each application for all metrics published by the application or for a specific metric or set of metrics published by that application.

The statistical operators supported by the system are not limited to streaming operators; although applications with very high volumes may be restricted by available resources (namely cpu and memory) to only using streaming operators.  This dual operating mode allows the system to compute an accurate 99th percentile, for example, when it is feasible to do so.  Offering streaming variations of non-streaming operators, for example using histograms or wavelets for percentiles, allows the same statistics to be emitted for both low and high volume (or resource unconstrained and resource constrained) applications, with the understanding of sacrificing some accuracy.

License
-------

Published under Apache Software License 2.0, see [LICENSE](LICENSE)

&copy; Groupon Inc., 2014
