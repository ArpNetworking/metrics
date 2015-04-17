Tsd
===

This folder contains projects associated with the processing and transmission of metrics.  The build is structured
as a Gradle project with sub-projects for the various components.

Sub-projects
------------

[TSD Aggregator](tsd-aggregator/README.md) - Responsible for on-box aggregation of metrics into intervals.
[Generator](generator/README.md) - Generates metrics data for testing the system.
[Cluster Aggregator](cluster-aggregator/README.md) - Responsible for aggregating aggregates from multiple sources.
[Performance-Test](performance-test/README.md) - Helper classes for performance testing.
TSD Core - Core library containing shared metrics functionality. 

Building All Projects
---------------------

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Protobuf 2.5.0](https://code.google.com/p/protobuf/downloads/list)

Building:
    tsd> ./gradlew installApp

Running
-------

See the sub-project README files for information regarding running the applications.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2014
