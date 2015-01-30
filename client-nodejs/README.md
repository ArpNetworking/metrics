Metrics
=======

Client implementation for publishing metrics from NodeJs applications.


Instrumenting Your Application
------------------------------

### Add Dependency

Determine the latest version of the NodeJs client:

    npm view tsd-metrics-client dist-tags.latest

Add a dependency on the metrics client in your *package.json* file:

```json
"dependencies" : {
    "tsd-metrics-client" : "VERSION",
}
```

If you are using [shrinkwrap](https://www.npmjs.org/doc/cli/npm-shrinkwrap.html) you need to re-shrink after adding the dependency.

### Initialization and Configuration

The metrics library must be included before being used.

```javascript
tsd = require("tsd-metrics-client")
```

Optionally, the client library may be configured.

```javascript
tsd = require("tsd-metrics-client") ({
    LOG_CONSOLE_ECHO : <true/false>, // additionally output metrics to console; default: false
    LOG_FILE_NAME : <log_file_name>, // name of the log file; default: "tsd-query.log"
    LOG_BACKUPS : <number>,          // maximum number of log files to retain; default: 10
    LOG_MAX_SIZE : <number_in_bytes> // log size file rotation threshold; default: 32 MB
});
```

### Metrics

A new Metrics instance should be created for each unit of work.  For example:

```javascript
var metrics = new tsd.TsdMetrics();
```

Counters, timers and gauges are recorded against a metrics instance which must be closed at the end of a unit of work.  After the Metrics instance is closed no further measurements may be recorded against that instance.

```javascript
metrics.incrementCounter("foo");
metrics.startTimer("bar");
// Do something that is being timed
metrics.stopTimer("bar");
metrics.setGauge("temperature", 21.7);
metrics.close();
```

### Counters

Surprisingly, counters are probably more difficult to use and interpret than timers and gauges.  In the simplest case you can just starting counting, for example, iterations of a loop:

```javascript
arrayOfStrings.forEach(function(item, index) {
    metrics.incrementCounter("strings");
    ...
}
```

However, what happens if listOfString is empty? Then no sample is recorded. To always record a sample the counter should be reset before the loop is executed:

```javascript
metrics.resetCounter("strings");
arrayOfStrings.forEach(function(item, index) {
    metrics.incrementCounter("strings");
    ...
}
```

Next, if the loop is executed multiple times:

```javascript
arrayOfArrayOfStrings.forEach(function(arrayOfStrings, arrayOfStringsIndex) {
    metrics.resetCounter("strings");
    arrayOfStrings.forEach(function(item, index) {
        metrics.incrementCounter("s");
        ...
    }
}
```

The above code will produce a number of samples equal to the size of listOfListOfStrings (including no samples if listOfListOfStrings is empty).  If you move the resetCounter call outside the outer loop the code always produces a single sample (even if listOfListOfStrings is empty).  There is a significant difference between counting the total number of strings and the number of strings per list; especially, when computing and analyzing statistics such as percentiles. 

Finally, if the loop is being executed concurrently for the same unit of work, that is for the same Metrics instance, then you could use a Counter object:

```javascript
final Counter counter = metrics.createCounter("strings");
arrayOfStrings.forEach(function(item, index) {
    counter.increment();
    ...
}
```

The Counter object example extends in a similar way for nested loops.

### Timers

Timers are very easy to use. The only note worth making is that when using timers - either procedural or via objects - do not forget to stop/close the timer!  If you fail to do this the client will log a warning and suppress any unstopped/unclosed samples.

The timer object allows a timer sample to be detached from the Metrics instance.  For example:  

```javascript
public void run() {
    final Timer t = metrics.createTimer("operation");
    // Perform your operation
    t.stop();
}
```

The one caveat is to ensure the timer objects are stopped/closed before the Metrics instance is closed.  Failing to do so will log a warning and suppress any samples stopped/closed after the Metrics instance is closed.
 
### Gauges

Gauges are the simplest metric to record.  Samples for a gauge represent spot measurements. For example, the length of a queue or the number of active threads in a thread pool.  Gauges are often used in separate units of work to measure the state of system resources, for example the row count in a database table.  However, gauges are also useful in existing units of work, for example recording the memory in use at the beginning and end of each service request.

### Events

In lieu of logging the TsdMetrics class provides an event emitter to which end users may add listeners. The emitter generates two types of events:

* `"error"`: Emitted when an error is detected. The parameter passed to the listener function is contains details about the error condition encountered.
* `"logEvent"`: Emitted after closing the Metrics instance and writing data to disk.  The parameter passed to the listener function is an object with the data that was written to the disk.

For example, to log all errors you could do the following:

```javascript
metrics.events().addListener("error", function (err) {
    console.log("Error: " + err);
});
```

Building
--------

Links to prerequisites:
* [NodeJs 0.10.26+](http://nodejs.org/download/)

Install the package dependencies:

    client-nodejs> npm install

Install Grunt task runner:

    client-nodejs> npm install -g grunt-cli

Build the project:

    client-nodejs> grunt

Execute the tests:
  
    client-nodejs> grunt test

Generate documentation:

    client-nodejs> grunt build jsdoc

Install the current version locally:

    npm link /path_to_package

Using the local version is intended only for testing or development. 

License
-------

Published under Apache Software License 2.0, see LICENSE
