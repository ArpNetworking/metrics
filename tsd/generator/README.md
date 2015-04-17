Generator
=========

Generates realistic-looking metrics data for use in system testing.

Setup
-----

### Building ###

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

Building:
    generator> ./gradlew installApp

### Installing ###

The artifacts from the build are in *tsd/generator/build/install/generator/* and should be copied to an appropriate directory on your application host(s).

### Execution ###

In the installation's *bin* directory there are scripts to start the generator: *generator* (Linux) and *generator.bat* (Windows).  One of these should be executed on system start with appropriate parameters; for example:

    /usr/local/lib/generator/bin/generator-aggregator --continuous

### Configuration ###

All configuration is provided on the command line.

Run without command line arguments, the generator will produce a set of test files in the current directory.
Running with "--continuous", the generator will produce a continuous stream of metrics into a file.

#### Logging ####

To customize logging you may provide a [LogBack](http://logback.qos.ch/) configuration file.  To use a custom logging configuration you need to define and export an environment variable before executing *generator*:

    GENERATOR_OPTS="-Dlogback.configurationFile=/usr/local/lib/generator/config/logger.xml"
    export GENERATOR_OPTS

Where */usr/local/lib/generator/config/logger.xml* is the path to your logging configuration file.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2014
