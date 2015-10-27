Tsd Aggregator
==============

Provides aggregation of metrics samples over periods of time.


Setup
-----

### Building ###

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven 3.2.5+](https://maven.apache.org/)
* [Protobuf 2.5.0](https://code.google.com/p/protobuf/downloads/list)

Building:
    tsd> mvn install -pl tsd-aggregator -amd

### Performance Tests ###

To execute:
    tsd/tsd-aggregator> mvn -Pperformance-test test

### Installing ###

The artifacts from the build are in *tsd/tsd-aggregator/target/appassembler* and should be copied to an appropriate directory on your application host(s).

### Execution ###

In the installation's *bin* directory there are scripts to start TSD Aggregator: *tsd-aggregator* (Linux) and *tsd-aggregator.bat* (Windows).  One of these should be executed on system start with appropriate parameters; for example:

    /usr/local/lib/tsd_aggregator/bin/tsd-aggregator /usr/local/lib/tsd_aggregator/config/config.json

### Configuration ###

#### Logging ####

To customize logging you may provide a [LogBack](http://logback.qos.ch/) configuration file.  To use a custom logging configuration you need to define and export an environment variable before executing *tsd-aggregator*:

    JAVA_OPTS="-Dlogback.configurationFile=/usr/local/lib/tsd_aggregator/config/logger.xml"
    export JAVA_OPTS

Where */usr/local/lib/tsd_aggregator/config/logger.xml* is the path to your logging configuration file.

#### Daemon ####

The Tsd Aggregator daemon configuration is specified in a JSON file.  The location of the configuration file is passed to *tsd-aggregator* as a command line argument:

    /usr/local/lib/tsd_aggregator/config/config.json

The configuration specifies:

* logDirectory - The location of additional logs.  This is independent of the logging configuration.
* pipelinesDirectory - The location of configuration files for each metrics pipeline.
* httpHost - The ip address to bind the http server to.
* httpPort - The port to bind the http server to.
* jvmMetricsCollectionInterval - The JVM metrics collection interval in ISO-8601 period notation.
* limiters - Configuration of zero or more limiters by name.
* akkaConfiguration - Configuration of Akka.

For example:

```json
{
    "logDirectory": "/usr/local/lib/tsd_aggregator/logs",
    "pipelinesDirectory": "/usr/local/lib/tsd_aggregator/config/pipelines",
    "httpHost": "0.0.0.0",
    "httpPort": 6080,
    "jvmMetricsCollectionInterval": "PT.5S",
    "limiters": {
        "limiter": {
            "type": "com.arpnetworking.tsdcore.limiter.DefaultMetricsLimiter",
            "maxAggregations": 2000,
            "stateFile": "logs/limiter.state",
            "stateFlushInterval": "PT5M",
            "ageOutThreshold": "P7D"
        }
    },
    "akkaConfiguration": {
        "akka": {
            "loggers": [
                "akka.event.slf4j.Slf4jLogger"
            ],
            "loglevel": "DEBUG",
            "stdout-loglevel": "DEBUG",
            "logging-filter": "akka.event.slf4j.Slf4jLoggingFilter",
            "actor": {
                "debug": {
                    "unhandled": "on"
                }
            }
        }
    }
}
```

#### Pipelines ####

One instance of Tsd Aggregator supports multiple independent services on the same host.  The most basic single application host still typically configures two services: i) the end-user application running on the host, and ii) the system metrics captured by CollectD.  Each of these services is configured as a pipeline in Tsd Aggregator.  The pipeline defines the name of the service, one or more sources of metrics and one more destinations or sinks for the aggregated statistics.

For example:

```json
{
    "name": "MyApplicationPipeline",
    "serviceName": "MyApplication",
    "sources":
    [
        {
            "type": "com.arpnetworking.tsdcore.sources.FileSource",
            "name": "my_application_source",
            "filePath": "/var/log/my-application-query.log",
            "parser": {
                "type": "com.arpnetworking.tsdaggregator.parsers.QueryLogParser"
            }
        }
    ],
    "sinks":
    [
        {
            "type": "com.arpnetworking.tsdcore.sinks.ReMetSink",
            "name": "my_application_remet_sink",
            "uri": "http://localhost:7090/report"
        },
        {
            "type": "com.arpnetworking.tsdcore.sinks.CarbonSink",
            "name": "my_application_carbon_sink",
            "serverAddress": "192.168.0.1"
        },
        {
            "type": "com.arpnetworking.tsdcore.sinks.AggregationServerSink",
            "name": "my_application_aggregation_server_sink",
            "serverAddress": "192.168.0.2"
        }
    ]
}
```

Each of the pipeline configuration files should be placed in the *pipelinesDirectory* defined as part of the daemon configuration above.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2014
