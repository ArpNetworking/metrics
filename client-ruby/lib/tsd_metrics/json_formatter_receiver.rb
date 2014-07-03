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

require 'json'

module TsdMetrics
  class JsonFormatterReceiver # Implements metricStructReceiver
    def initialize(outputStream)
      @outputStream = outputStream
    end
    def receive(metricStruct)
      hash = {
        gauges: metricStruct.gauges,
        timers: metricStruct.timers,
        counters: metricStruct.counters,
        annotations: metricStruct.annotations
      }
      haveMetrics = false
      for metricType in [:gauges, :timers, :counters]
        haveMetrics = true if hash[metricType].length > 0
      end
      # The timestamp annotations are always present, but we're looking for
      # any further annotations.
      haveMetrics = true if hash[:annotations].length > 2

      return unless haveMetrics

      hash[:annotations][:initTimestamp] = hash[:annotations][:initTimestamp].to_f
      hash[:annotations][:finalTimestamp] = hash[:annotations][:finalTimestamp].to_f
      @outputStream.write(hash.to_json)
    end
  end
end
