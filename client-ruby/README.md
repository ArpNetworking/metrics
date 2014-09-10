Metrics
=======

Client implementation for publishing metrics from Ruby applications. 


Instrumenting Your Application
------------------------------

### Add Dependency

Determine the latest version of the Ruby client in [rubygems.org](https://rubygems.org/gems/tsd_metrics).  Modify your Gemfile to include the Ruby Gems repository:

    source "http://rubygems.org"

Add a dependency on the metrics client in your Gemfile:

    gem "tsd_metrics" "VERSION"

### Initialization

The metrics library must be initialized before being used. The following example will configure the library to output metrics to the relative directory "./log/query.log":

```ruby
TsdMetrics.init(Rails.root.join 'log', 'query.log')
```

### Metrics

A new Metrics instance should be created for each unit of work.  For example:

```ruby
metrics = TsdMetrics.buildMetric
```

Counters, timers and gauges are recorded against a metrics instance which must be closed at the end of a unit of work.  After the Metrics instance is closed no further measurements may be recorded against that instance.
 
 ```ruby
metrics.incrementCounter("foo");
metrics.startTimer("bar");
// Do something that is being timed
metrics.stopTimer("bar");
metrics.setGauge("temperature", 21.7);
metrics.close();
```

### Counters

Surprisingly, counters are probably more difficult to use and interpret than timers and gauges.  In the simplest case you can just starting counting, for example, iterations of a loop:

```ruby
arrayOfStrings.each { |string|
    metrics.incrementCounter("strings");
    ...
}
```

However, what happens if listOfString is empty? Then no sample is recorded. To always record a sample the counter should be reset before the loop is executed:

```ruby
metrics.resetCounter("strings");
arrayOfStrings.each { |string|
    metrics.incrementCounter("strings");
    ...
}
```

Next, if the loop is executed multiple times:

```ruby
arrayOfArrayOfStrings.each { |arrayOfStrings|
    metrics.resetCounter("strings");
    arrayOfStrings.each { |string|
        metrics.incrementCounter("s");
        ...
    }
}
```

The above code will produce a number of samples equal to the size of listOfListOfStrings (including no samples if listOfListOfStrings is empty).  If you move the resetCounter call outside the outer loop the code always produces a single sample (even if listOfListOfStrings is empty).  There is a significant difference between counting the total number of strings and the number of strings per list; especially, when computing and analyzing statistics such as percentiles. 

Finally, if the loop is being executed concurrently for the same unit of work, that is for the same Metrics instance, then you could use a Counter object:

```ruby 
final Counter counter = metrics.createCounter("strings");
arrayOfStrings.each { |string|
    counter.increment();
    ...
}
```

The Counter object example extends in a similar way for nested loops.

### Timers

Timers are very easy to use. The only note worth making is that when using timers - either procedural or via objects - do not forget to stop/close the timer!  If you fail to do this the client will log a warning and suppress any unstopped/unclosed samples.

The timer object allows a timer sample to be detached from the Metrics instance.  For example:  

```ruby
def threadFunc
    final Timer t = metrics.createTimer("operation");
    // Perform your operation
    t.stop();
}
```

This is very useful in a concurrent system when executing and thus measuring the same event multiple times concurrently.  The one caveat is to ensure the timer objects are stopped/closed before the Metrics instance is closed.  Failing to do so will log a warning and suppress any samples stopped/closed after the Metrics instance is closed.
 
### Gauges

Gauges are the simplest metric to record.  Samples for a gauge represent spot measurements. For example, the length of a queue or the number of active threads in a thread pool.  Gauges are often used in separate units of work to measure the state of system resources, for example the row count in a database table.  However, gauges are also useful in existing units of work, for example recording the memory in use at the beginning and end of each service request.

Building
--------

Links to prerequisites:
* [RVM](https://rvm.io/rvm/install) (w/ Ruby 1.9.3+)

Install Ruby version with RVM:

    client-ruby> rvm install 1.9.3

Select Ruby version with RVM:

    client-ruby> rvm 1.9.3

Building the gem:

    client-ruby> gem build tsd_metrics.gemspec

Testing the gem:

    client-ruby> rspec

Install the current version locally:

    gem install --local /path_to_gem/tsd_metrics.gem

Using the local version is intended only for testing or development. 

License
-------

Published under Apache Software License 2.0, see LICENSE
