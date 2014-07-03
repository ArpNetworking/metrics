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

module TsdMetrics
  class Timer
    def initialize(parentMetric)
      @parentMetric = parentMetric
      @samples = []
    end

    def createNewSample
      sample = TimerSample.new(@parentMetric)
      @samples.push sample
      sample
    end

    def addDuration(duration)
      sample = TimerSample.new(@parentMetric)
      sample.duration = duration
      @samples.push sample
    end

    def durations
      durations = []
      @samples.each do |s|
        if !(s.isRunning)
          durations.push s.duration
        else
          TsdMetrics.errorLogger.warn("Unstopped timer dropped from log")
        end
      end
      durations
    end
  end
end
