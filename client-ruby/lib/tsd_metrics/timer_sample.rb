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
    attr_accessor :duration
    def initialize(metricStatusSupplier)
      @metricStatusSupplier = metricStatusSupplier
      @startTime = Time.now
      @duration = nil
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
      diff = (1000*(now-@startTime))
      @duration = diff
      @startTime = nil
    end
    def isRunning
      return @startTime != nil
    end
  end
end
