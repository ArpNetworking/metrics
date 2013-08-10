tsdaggregator
=============

## Overview ##

TSDAggregator does exactly as the name implies: it takes time series data, aggregates it, and then emits the aggregations.
TSDAggregator can server as the aggregation core of your metrics pipeline.  It was designed to be flexible
to input types, allow custom aggregations based on sorted or unsorted data, and flexible to output formats.  There 
is currently only a single metrics line parser, but we're happy to see new formats.  Some suggestions are web access log 
formats for apache and nginx.  For output, there are several emitters: console, file (key/value pairs), http post,
monitord, remet, and rrdtool cluster. Aggregations supported are percentiles (0/min, 50, 90, 95, 99, 99.9, 100/max), sum, 
count, mean, first and last.  Aggregations are flexible and are assumed to start on an hour boundary.  This means you
can easily have 1 minute, 5 minute, 10 minute, 15 minute, etc. But if you do something like 7 minute metrics, things
might get a little weird, but it'll still work.

## Usage ##
<code><pre>
usage: tsdaggregator [-c <cluster>] [-cs <stat>] [-d <period>] [-e
       <extension>] [-f <input_file>] [-h <host>] [-l] [--monitord] [-o
       <output_file>] [-p <parser>] [--remet] [--rrd] [-s <service>] [-ts
       <stat>] [-u <uri>]
 -c,--cluster <cluster>       name of the cluster the host is in
 -cs,--counterstat <stat>     statistics of aggregation to record for
                              counters (multiple allowed)
 -d,--period <period>         aggregation time period in ISO 8601 standard
                              notation (multiple allowed)
 -e,--extension <extension>   extension of files to parse - uses a union
                              of arguments as a regex (multiple allowed)
 -f,--file <input_file>       file to be parsed
 -h,--host <host>             host the metrics were generated on
 -l,--tail                    "tail" or follow the file and do not
                              terminate
    --monitord                send data to a monitord server
 -o,--output <output_file>    output file
 -p,--parser <parser>         parser to use to parse log lines
    --remet                   send data to a local remet server
    --rrd                     create or write to rrd databases
 -s,--service <service>       service name
 -ts,--timerstat <stat>       statistics of aggregation to record for
                              timers (multiple allowed)
 -u,--uri <uri>               metrics server uri
 </pre></code>
