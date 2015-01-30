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
  class CounterSample
    attr_reader :value
    def initialize(metricStatusSupplier)
      @metricStatusSupplier = metricStatusSupplier
      @value = 0
    end
    def increment(magnitude = 1)
      if @metricStatusSupplier.metricIsClosed
        TsdMetrics.errorLogger.warn("Increment or decrement called on Counter after metric has been closed")
        return
      end
      @value += magnitude
    end
    def decrement(magnitude = 1)
      increment(-1*magnitude)
    end
    def sampleRepresentation
      # Always unitless
      {value: @value}
    end
  end
end
