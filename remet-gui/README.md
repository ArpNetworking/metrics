ReMet Gui
=========

Specialization of [Metrics Portal](https://github.com/ArpNetworking/metrics-portal).

Setup
-----

### Building

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Play 2.4.3](http://www.playframework.com/download)

Building:
    remet-gui> activator stage

### Installing

The artifacts from the build are in *remet-gui/target/universal/stage* and should be copied to an appropriate directory on the ReMet Gui host(s).

### Execution

In the installation's *bin* directory there are scripts to start ReMet Gui: *remet-gui* (Linux/Mac) and *remet-gui.bat* (Windows).  One of these should be executed on system start with appropriate parameters; for example:

    /usr/local/lib/remet_gui/bin/remet-gui -J-Xmn150m -J-XX:+UseG1GC -J-XX:MaxGCPauseMillis=20 -Dhttp.port=80 -Dpidfile.path=/usr/local/var/REMETGUI_PID

### Configuration

Aside from the command line arguments, the only configuration you may provide at this time is a [LogBack](http://logback.qos.ch/) configuration file.  To use a custom logging configuration simply add the following argument to the command line above:

    -Dlogger.file=/usr/local/lib/remet_gui/logger.xml

Where */usr/local/lib/remet_gui/logger.xml* is the path to your logging configuration file.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Groupon Inc., 2014
