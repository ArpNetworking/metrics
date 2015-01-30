CollectD Extra
==============

Output plugin for [CollectD](https://collectd.org/) system statistics collection daemon.


Setup
-----

### Building

Prerequisites:
* [Autoconf](https://www.gnu.org/software/autoconf/) (known to work with version 2.69)
* [Automake](http://www.gnu.org/software/automake/) (known to work with version 1.14)
* [GCC](http://gcc.gnu.org/) (or other compatible C compiler)

The plugin was built for version [5.4.1 of CollectD](https://collectd.org/files/collectd-5.4.1.tar.gz). Download and extract the source code archive.

#### Add Plugin

1) Copy the [tsd.c](tsd.c) into the *collectd-5.4.1/src* directory.

2) Add the following lines to *collectd-5.4.1/configure.ac*:

```
AC_PLUGIN([tsd],         [yes],                [TSD output plugin])
tsd . . . . . . . . . $enable_tsd
```

You may do this by applying the included patch:

    collectd-5.4.1> patch configure.ac < ~/metrics/extras/collectd-extra/configure.ac.patch

3) Add the following lines to *collectd-5.4.1/src/Makefile.am*:

```
if BUILD_PLUGIN_TSD
pkglib_LTLIBRARIES += tsd.la
tsd_la_SOURCES = tsd.c
tsd_la_LDFLAGS = -module -avoid-version
collectd_LDADD += "-dlopen" tsd.la
collectd_DEPENDENCIES += tsd.la
endif
```

You may do this by applying the included patch:

    collectd-5.4.1> patch src/Makefile.am < ~/metrics/extras/collectd-extra/src.Makefile.am.patch 

#### Patch for Plugin Symlinks (Optional)

The base version of CollectD 5.4.1 does not load plugins through symlinks.  This may present problems when running through certain deployment systems.  Included in the extra is a patch which removes this restriction in CollectD.  Use the following to apply the patch:

    collectd-5.4.1> patch src/plugin.c < ~/metrics/extras/collectd-extra/src.plugin.c.patch

#### Compiling

Please refer to the README file in the CollectD package or to the [CollectD website](https://collectd.org/wiki/index.php/Build_system) for more information on compiling and install CollectD.  For most use cases the following should be sufficient:

    collectd-5.4.1> ./configure && make && make install

### Configuring

#### CollectD

Configure CollectD to collect the system metrics you require; please refer to the CollectD [configuration documentation](https://collectd.org/documentation/manpages/collectd.conf.5.shtml) for details.

Next configure the TSD plugin to write system metrics in the query log file format:

```
<LoadPlugin tsd>
</LoadPlugin>
<Plugin tsd>
    DataDir "/var/log/"
    StoreRates false       Optional: defaults to false
    Version "2C"           Optional: defaults to the latest version
    FileBufferSize "4096"  Optional: defaults to 4096 bytes
    FilesToRetain "24"     Optional: defaults to 24 (rotated hourly)
</Plugin>
```

The metrics are written to a file named *collectd-query.log* in the directory specified by *dataDir*.  The file is rotated every hour to *collectd-query.YYYYMMMDDHH.log* (e.g. collectd-query.2014062516.log).

For a complete example please see [doc/collectd_sample.conf](../../doc/collectd_sample.conf).

#### Tsd Aggregator

Configure Tsd Aggregator to include a pipeline definition for CollectD.  The pipeline defines the source, where to find metrics, and the sinks, where to publish aggregates.  For more information on Tsd Aggregator configuration please refer to [tsd/README.md](../../tsd/README.md).  Below is a minimal example of a pipeline configuration for CollectD:

```json
{
    "name": "CollectDPipeline",
    "serviceName": "CollectD",
    "sources":
    [
        {
            "type": "com.arpnetworking.tsdcore.sources.FileSource",
            "name": "collectd_source",
            "filePath": "/var/log/collectd-query.log",
            "parser": {
                "type": "com.arpnetworking.tsdaggregator.parsers.QueryLogParser"
            }
        }
    ],
    "sinks":
    [
        {
            "type": "com.arpnetworking.tsdcore.sinks.ReMetSink",
            "name": "collectd_remet_sink",
            "uri": "http://localhost:7090/report"
        }
    ]
}
```

The above configuration should be written into a configuration file (e.g. collectd-pipeline.cfg) in the pipeline definitions directory of the local Tsd Aggregator instance.

License
-------

Published under Apache Software License 2.0, see LICENSE
