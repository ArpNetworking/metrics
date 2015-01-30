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

require 'tsd_metrics/tsd_metric'
require 'timecop'
include TsdMetrics

describe "TsdMetric" do
  let(:sink) { double(:sink, :record => nil) }
  let(:mutexer) do
    mock = double()
    allow(mock).to receive(:synchronize).and_yield()
    mock
  end
  let(:startTime) { Time.now }
  let(:metric) { TsdMetric.new(startTime, sink, mutexer) }
  def closeAndGetEvent(metric=metric)
    event = nil
    allow(sink).to receive(:record) do |metricEvent|
      event = metricEvent
    end
    metric.close()
    event
  end
  def pickValuesFrom(metric)
    metric.map do |sample|
      sample[:value]
    end
  end
  def pickUnitsFrom(metric)
    metric.map do |sample|
      sample[:unit]
    end
  end

  context "with the minumum state" do
    let(:gaugeName) { "myGauge"}
    let(:simplestMetric) do
      metric.tap{|m| m.setGauge(gaugeName, 100)}
    end

    it "outputs the minimum state for a metric" do
      event = nil
      expect(sink).to receive(:record) do |metricEvent|
        event = metricEvent
      end
      beforeCloseTime = Time.now()
      simplestMetric.close()
      afterCloseTime = Time.now()

      event.should_not be_nil
      annotations = event.annotations
      annotations.should_not be_nil
      annotations[:initTimestamp].should == startTime
      annotations[:finalTimestamp].should >= beforeCloseTime
      annotations[:finalTimestamp].should <= afterCloseTime
    end

    it "throws an exception if any operations are done after closing" do
      closeAndGetEvent(simplestMetric)
      expect { simplestMetric.setGauge("something", 1) }.to raise_error TsdMetrics::Error
    end

    describe "open?" do
      it "works as expected" do
        expect { closeAndGetEvent(simplestMetric) }
          .to change(simplestMetric, :open?).from(true).to(false)
      end
    end
  end

  describe "gauges" do
    let(:gaugeName) { "myGauge"}
    let(:gaugeName2) { "myOtherGauge"}
    it "can be set" do
      metric.setGauge(gaugeName, 100, :byte)
      metric.setGauge(gaugeName, 307, :byte)

      metric.setGauge(gaugeName2, 50, :week)
      metric.setGauge(gaugeName2, 23, :week)

      event = closeAndGetEvent
      event.gauges[gaugeName].should =~ [{value: 100, unit: :byte}, {value: 307, unit: :byte}]
      event.gauges[gaugeName2].should =~ [{value: 50, unit: :week}, {value: 23, unit: :week}]
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

      event = closeAndGetEvent
      values = pickValuesFrom(event.timers[timerName])
      values.should include(10000000000)
      values.should include(25000000000)
    end

    its "default unit is nanoseconds" do
      Timecop.freeze(Time.now)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 12)
      metric.stopTimer(timerName)
      Timecop.return
      event = closeAndGetEvent
      event.timers[timerName].should == [{value: 12e9, unit: :nanosecond}]
    end

    it "only record timers that are stopped when metric is closed" do
      Timecop.freeze(Time.now)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 10)
      metric.stopTimer(timerName)
      Timecop.freeze(Time.now + 5)
      metric.startTimer(timerName)
      Timecop.freeze(Time.now + 25)

      event = closeAndGetEvent
      values = pickValuesFrom(event.timers[timerName])
      values.should == [10e9]

      Timecop.return
    end

    it "can be added manually" do
      metric.setTimer(timerName, 20.5, :second)
      event = closeAndGetEvent
      pickValuesFrom(event.timers[timerName]).should include(20.5)
    end

    it "keeps whole sample groups when no samples are stopped" do
      otherTimerName = "myOtherTimer"
      metric.startTimer(timerName)
      metric.startTimer(otherTimerName)

      metric.stopTimer(otherTimerName)

      event = closeAndGetEvent
      event.timers.should include(timerName)
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
      event = closeAndGetEvent
      values = pickValuesFrom(event.timers[timerName])
      values.should == [62e9, 28e9, 8e9]

      Timecop.return

    end

    it "can be obtained as an object" do
      timerSample = nil
      expect { timerSample = metric.createTimer(:timerName) }.not_to raise_error
      timerSample.should_not == nil
    end

    it "contains the unit for each timer" do
      metric.setTimer(timerName, 17.5, :second)
      event = closeAndGetEvent
      pickUnitsFrom(event.timers[timerName]).should include(:second)
      [{:a => "b"}].should include({:a => "b"})
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
        event = closeAndGetEvent
        pickValuesFrom(event.timers[timerName]).should include(12000000000)
      end

      describe "stopped?" do
        it "works as expected" do
          expect { ooTimer.stop }.to change(ooTimer, :stopped?).from(false).to(true)
        end
      end
    end
  end

  describe "counters" do
    let(:counterName) { :myCounter }

    it "default to start at 0" do
      metric.incrementCounter(counterName)
      event = closeAndGetEvent
      pickValuesFrom(event.counters[counterName]).should == [1]
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
      event = closeAndGetEvent
      pickValuesFrom(event.counters[counterName]).should include 53
    end

    it "can be reset to create multiple values" do
      metric.resetCounter(counterName)
      metric.incrementCounter(counterName)
      metric.resetCounter(counterName)
      3.times { metric.incrementCounter(counterName) }
      metric.resetCounter(counterName)
      # Test for existance of array items without testing order
      pickValuesFrom(closeAndGetEvent.counters[counterName]).should =~ [3,0,1]
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
        event = closeAndGetEvent
        pickValuesFrom(event.counters[counterName]).should include 53
      end
    end
  end

  describe "annotations" do
    let(:annotationName) { :myAnnotation }
    let(:annotationString) { "it was the worst of times, it was the blurst of times" }
    it "can be set on the metric" do
      metric.annotate(annotationName, annotationString)
      closeAndGetEvent.annotations[annotationName].should == annotationString
    end
  end
end
