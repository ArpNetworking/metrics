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

require 'tsd_metrics/tsd_metric_2c'
require 'timecop'
include TsdMetrics

describe "TsdMetric" do
  let(:structReceiver) { double(:structReceiver, :receive => nil) }
  let(:mutexer) do
    mock = double()
    allow(mock).to receive(:synchronize).and_yield()
    mock
  end
  let(:startTime) { Time.now }
  let(:metric) { TsdMetric2c.new(startTime, structReceiver, mutexer) }
  def closeAndGetStruct(metric=metric)
    struct = nil
    allow(structReceiver).to receive(:receive) do |metricStruct|
      struct = metricStruct
    end
    metric.close()
    struct
  end

  context "with the minumum state" do
    let(:gaugeName) { "myGauge"}
    let(:simplestMetric) do
      metric.tap{|m| m.setGauge(gaugeName, 100)}
    end
    it "outputs the minimum state for a metric" do
      struct = nil
      expect(structReceiver).to receive(:receive) do |metricStruct|
        struct = metricStruct
      end
      beforeCloseTime = Time.now()
      simplestMetric.close()
      afterCloseTime = Time.now()

      struct.should_not be_nil
      annotations = struct.annotations
      annotations.should_not be_nil
      annotations[:initTimestamp].should == startTime
      annotations[:finalTimestamp].should >= beforeCloseTime
      annotations[:finalTimestamp].should <= afterCloseTime
    end
    it "throws an exception if any operations are done after closing" do
      closeAndGetStruct(simplestMetric)
      expect { simplestMetric.setGauge("something", 1) }.to raise_error TsdMetrics::Error
    end
  end

  describe "gauges" do
    let(:gaugeName) { "myGauge"}
    let(:gaugeName2) { "myOtherGauge"}
    it "can be set" do
      metric.setGauge(gaugeName, 100)
      metric.setGauge(gaugeName, 307)

      metric.setGauge(gaugeName2, 50)
      metric.setGauge(gaugeName2, 23)

      struct = closeAndGetStruct
      struct.gauges[gaugeName].should =~ [100, 307]
      struct.gauges[gaugeName2].should =~ [50, 23]
    end
  end

  describe "timers" do
    let(:timerName) { :myTimer }
    it "can be started and stopped multiple times" do
      Timecop.freeze(Time.now)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 10)
      metric.stopTimer(timerName)
      Timecop.freeze(Time.now + 5)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 25)
      metric.stopTimer(timerName)
      Timecop.return

      struct = closeAndGetStruct
      struct.timers[timerName].should include(10000)
      struct.timers[timerName].should include(25000)
    end

    it "only record timers that are stopped when metric is closed" do
      Timecop.freeze(Time.now)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 10)
      metric.stopTimer(timerName)
      Timecop.freeze(Time.now + 5)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 25)

      struct = closeAndGetStruct
      struct.timers[timerName].should == [10000]

      Timecop.return
    end

    it "can be added manually" do
      metric.setTimer(timerName, 20.5)
      struct = closeAndGetStruct
      struct.timers[timerName].should include(20.5)
    end

    it "drops whole sample groups when no samples are stopped" do
      otherTimerName = "myOtherTimer"
      metric.startTimer(timerName)
      metric.startTimer(otherTimerName)

      metric.stopTimer(otherTimerName)

      struct = closeAndGetStruct
      struct.timers.should_not include(timerName)
    end

    it "orders timer samples by start time" do
      # Procedural interface first
      Timecop.freeze(Time.now)
      metric.startTimer(timerName)

      # Then start an OO one
      Timecop.freeze(Time.now + 2)
      timerSample1 = metric.createTimer(timerName)

      # And another
      Timecop.freeze(Time.now + 4)
      timerSample2 = metric.createTimer(timerName)

      # Now stop them in reverse order
      Timecop.freeze(Time.now + 8)
      timerSample2.stop()
      Timecop.freeze(Time.now + 16)
      timerSample1.stop()
      Timecop.freeze(Time.now + 32)
      metric.stopTimer(timerName)

      # Ensure the order of samples is the same as the timer-starting order
      struct = closeAndGetStruct
      struct.timers[timerName].should == [62000, 28000, 8000]

      Timecop.return

    end

    it "can be obtained as an object" do
      timerSample = nil
      expect { timerSample = metric.createTimer(timerName) }.not_to raise_error
      timerSample.should_not == nil
    end

    describe "OO interface" do
      let(:ooTimer) { metric.createTimer(timerName) }
      it "can be created and stopped to record a sample" do
        Timecop.freeze(Time.now)
        # Causes creation of the timer; 'let' is lazy
        ooTimer
        Timecop.freeze(Time.now + 12)
        ooTimer.stop
        Timecop.return
        struct = closeAndGetStruct
        struct.timers[timerName].should include(12000)
      end
    end
  end

  describe "counters" do
    let(:counterName) { :myCounter }
    it "default to start at 0" do
      metric.incrementCounter(counterName)
      struct = closeAndGetStruct
      struct.counters[counterName].should == [1]
    end
    it "can be incremented and decremented" do
      metric.resetCounter(counterName)
      12.times { metric.decrementCounter(counterName) }
      # Now at -12
      2.times { metric.decrementCounter(counterName, 3) }
      # Now at -18
      2.times { metric.incrementCounter(counterName) }
      # Now at -16
      3.times { metric.incrementCounter(counterName, 23) }
      # Now at 53
      struct = closeAndGetStruct
      struct.counters[counterName].should include 53
    end
    it "can be reset to create multiple values" do
      metric.resetCounter(counterName)
      metric.incrementCounter(counterName)
      metric.resetCounter(counterName)
      3.times { metric.incrementCounter(counterName) }
      metric.resetCounter(counterName)
      # Test for existance of array items without testing order
      closeAndGetStruct.counters[counterName].should =~ [3,0,1]
    end
    it "can be obtained as an object" do
      counterSample = nil
      expect { counterSample = metric.createCounter(:counterName) }.not_to raise_error
      counterSample.should_not == nil
    end
    describe "OO interface" do
      let(:ooCounter) { metric.createCounter(counterName) }
      it "can be incremented and decremented to produce a sample" do
        12.times { ooCounter.decrement() }
        # Now at -12
        2.times { ooCounter.decrement(3) }
        # Now at -18
        2.times { ooCounter.increment() }
        # Now at -16
        3.times { ooCounter.increment(23) }
        # Now at 53
        struct = closeAndGetStruct
        struct.counters[counterName].should include 53
      end
    end
  end

  describe "annotations" do
    let(:annotationName) { :myAnnotation }
    let(:annotationString) { "it was the worst of times, it was the blurst of times" }
    it "can be set on the metric" do
      metric.annotate(annotationName, annotationString)
      closeAndGetStruct.annotations[annotationName].should == annotationString
    end
  end
end
