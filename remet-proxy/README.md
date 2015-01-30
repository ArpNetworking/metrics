ReMet Proxy
===========

Acts as an intermediary between [Tsd Aggregator](../tsd/README.md) and [ReMet Gui](../remet-gui/README.md) to provide streaming metrics via web sockets. 


Setup
-----

### Building

Links to prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Play 2.3.0](http://www.playframework.com/download)

Building:
  remet-proxy> activate stage

### Installing

The artifacts from the build are in *remet-proxy/target/universal/stage* and should be copied to an appropriate directory on your application host(s).

### Execution

In the installation's *bin* directory there are scripts to start ReMet Proxy: *remet-proxy* (Linux) and *remet-proxy.bat* (Windows).  One of these should be executed on system start with appropriate parameters; for example:

    /usr/local/lib/remet_proxy/bin/remet-proxy -J-Xmn150m -J-XX:+UseG1GC -J-XX:MaxGCPauseMillis=20 -Dhttp.port=7090 -Dpidfile.path=/usr/local/var/REMETPROXY_PID

### Configuration

Aside from the command line arguments, the only configuration you may provide at this time is a [LogBack](http://logback.qos.ch/) configuration file.  To use a custom logging configuration simply add the following argument to the command line above:

    -Dlogger.file=/usr/local/lib/remet_proxy/logger.xml

Where */usr/local/lib/remet_proxy/logger.xml* is the path to your logging configuration file.

License
-------

Published under Apache Software License 2.0, see LICENSE
