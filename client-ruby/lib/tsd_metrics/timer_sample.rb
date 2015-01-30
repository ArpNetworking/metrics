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

module TsdMetrics
  class TimerSample
    attr_reader :duration, :unit

    def initialize(metricStatusSupplier)
      @metricStatusSupplier = metricStatusSupplier
      @startTime = Time.now
      @duration = nil
      @unit = :nanosecond
    end

    def stop
      if @startTime == nil
        TsdMetrics.errorLogger.warn("Stop called on already-stopped Timer sample")
        return
      end
      if @metricStatusSupplier.metricIsClosed
        TsdMetrics.errorLogger.warn("Stop called on Timer after metric has been closed")
        return
      end
      now = Time.now
      diffSecs = now.tv_sec - @startTime.tv_sec
      diffNanoSecs = now.tv_nsec - @startTime.tv_nsec
      diff = ((10**9) * diffSecs) + diffNanoSecs
      @duration = diff
      @startTime = nil
    end

    # Deprecated: Instead use the more ruby-esque #running?
    def isRunning
      return running?
    end

    def running?
      return @startTime != nil
    end

    def stopped?
      return ! running?
    end

    def set(duration, unit)
      @duration = duration
      @unit = unit
    end

    def sampleRepresentation
      if @unit == :noUnit
        {value: @duration}
      else
        {value: @duration, unit: @unit}
      end
    end
  end
end
