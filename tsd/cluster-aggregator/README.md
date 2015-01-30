Cluster Aggregator
==================

Combines aggregate values from multiple TSD Aggregators into an aggregate.  Simply,
this means combining the values from your fleet.


Setup
-----

### Building ###

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Protobuf 2.5.0](https://code.google.com/p/protobuf/downloads/list)

Building:
    cluster-aggregator> ./gradlew installApp

### Installing ###

The artifacts from the build are in *tsd/cluster-aggregator/build/install/cluster-aggregator/* and should be copied to an appropriate directory on your application host(s).

### Execution ###

In the installation's *bin* directory there are scripts to start ReMet Proxy: *cluster-aggregator* (Linux) and *cluster-aggregator.bat* (Windows).  One of these should be executed on system start with appropriate parameters; for example:

    /usr/local/lib/cluster_aggregator/bin/cluster-aggregator --config file:/usr/local/lib/tsd_aggregator/config/config.json

### Configuration ###

#### Logging ####

To customize logging you may provide a [LogBack](http://logback.qos.ch/) configuration file.  To use a custom logging configuration you need to define and export an environment variable before executing *cluster-aggregator*:

    CLUSTER_AGGREGATOR_AKKA_OPTS="-Dlogback.configurationFile=/usr/local/lib/cluster_aggregator/config/logger.xml"
    export CLUSTER_AGGREGATOR_AKKA_OPTS

Where */usr/local/lib/cluster_aggregator/config/logger.xml* is the path to your logging configuration file.

#### Daemon ####

The Cluster Aggregator daemon configuration is specified in a HOCON file.  The location of the configuration file is 
passed to *cluster-aggregator* as a command line argument:

    -Dconfig.file=/usr/local/etc/cluster_aggregator/config/prod.conf

Each of the pipeline configuration files should be placed in the *pipelinesDirectory* defined as part of the daemon configuration above.

License
-------

Published under Apache Software License 2.0, see LICENSE
