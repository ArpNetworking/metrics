# Copyright 2014 Groupon.com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
#     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#     See the License for the specific language governing permissions and
#     limitations under the License.

require_relative 'timer_sample'
require_relative 'timer'
require_relative 'counter'
require_relative 'exceptions'

module TsdMetrics
  class TsdMetric2c
    # Implements TsdMetricStruct interface
    attr_reader :annotations, :gauges

    def initialize(startTime, metricStructReciever, mutexStrategy)
      @metricStructReciever = metricStructReciever
      @annotations = {initTimestamp: startTime}
      @mutexStrategy = mutexStrategy
      @gauges = {}
      @metrics = {timers: {}, counters: {}}
      @metricClasses = {timers: Timer, counters: Counter}
      @staticSamples = {timers: {}, counters: {}}
      @closed = false
    end

    def setGauge(name, value)
      @mutexStrategy.synchronize do
        assertNotClosed
        @gauges[name] ||= []
        @gauges[name].push(value)
      end
    end

    def startTimer(name)
      @mutexStrategy.synchronize do
        assertNotClosed
        # Timer sample is started on creation
        pushNewStaticSample(:timers, name)
      end
    end

    def stopTimer(name)
      @mutexStrategy.synchronize do
        assertNotClosed
        sample = getStaticSample(:timers, name)
        sample.stop
      end
    end

    def setTimer(name, duration)
      @mutexStrategy.synchronize do
        assertNotClosed
        pushNewStaticSample(:timers, name)
        sample = getStaticSample(:timers, name)
        sample.stop()
        sample.duration = duration
      end
    end

    def createTimer(name)
      @mutexStrategy.synchronize do
        assertNotClosed
        ensureMetricExists(:timers, name)
        sample = getMetric(:timers, name).createNewSample
        sample
      end
    end

    def resetCounter(name)
      @mutexStrategy.synchronize do
        assertNotClosed
        ensureCounterExists(name)
        @staticSamples[:counters][name] = getMetric(:counters, name).createNewSample
      end
    end

    def incrementCounter(name, magnitude=1)
      @mutexStrategy.synchronize do
        assertNotClosed
        ensureStaticCounterSampleExists(name)
        getStaticSample(:counters, name).increment(magnitude)
      end
    end

    def decrementCounter(name, magnitude=1)
      @mutexStrategy.synchronize do
        assertNotClosed
        incrementCounter(name, -1*magnitude)
      end
    end

    def createCounter(name)
      @mutexStrategy.synchronize do
        assertNotClosed
        ensureMetricExists(:counters, name)
        getMetric(:counters, name).createNewSample
      end
    end

    def annotate(name, value)
      @mutexStrategy.synchronize do
        assertNotClosed
        @annotations[name] = value
      end
    end

    def close
      @mutexStrategy.synchronize do
        assertNotClosed
        @closed = true
        @annotations[:finalTimestamp] = Time.now()
        @metricStructReciever.receive(self)
      end
    end

    def timers
      durationHash = {}
      getMetricsOfType(:timers).each do |k,v|
        durations = v.durations
        # Drop timer metrics with no stopped timers
        durationHash[k] = durations if durations.length > 0
      end
      durationHash
    end

    def counters
      countersHash = {}
      getMetricsOfType(:counters).each do |k,v|
        countersHash[k] = v.values
      end
      countersHash
    end

    # "Implements" metricStatusSupplier
    def metricIsClosed
      return @closed
    end

    private

    def ensureCounterExists(name)
      ensureMetricExists(:counters, name)
    end

    def ensureMetricExists(metricType, name)
      @metrics[metricType][name] ||= @metricClasses[metricType].new(self)
    end

    def ensureStaticSampleExists(sampleType, name)
      ensureMetricExists(sampleType, name)
      @staticSamples[sampleType][name] ||= getMetric(sampleType, name).createNewSample
    end

    def ensureStaticCounterSampleExists(name)
      ensureStaticSampleExists(:counters, name)
    end

    def pushNewStaticSample(metricType, name)
      ensureMetricExists(metricType, name)
      @staticSamples[metricType][name] = getMetric(metricType, name).createNewSample
    end

    def getMetric(metricType, name)
      @metrics[metricType][name]
    end

    def getMetricsOfType(metricType)
      @metrics[metricType]
    end

    def getStaticSample(sampleType, name)
      @staticSamples[sampleType][name]
    end

    def assertNotClosed
      raise MetricClosedError if @closed
    end
  end
end
