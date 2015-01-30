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
require 'time'

module TsdMetrics
  class JsonFormattingSink # Implements metricSink
    def initialize(outputStream)
      @outputStream = outputStream
    end
    def receive(tsdMetricEvent)
      hash = {
        time: Time.now.utc.iso8601(3),
        name: "aint.metrics",
        level: "info",
        data: {
          version: "2e",
          gauges: proxyValuesProperty(tsdMetricEvent.gauges),
          timers: proxyValuesProperty(tsdMetricEvent.timers),
          counters: proxyValuesProperty(tsdMetricEvent.counters),
          annotations: tsdMetricEvent.annotations
        }
      }
      haveMetrics = [:gauges, :timers, :counters].any? do |metricType|
        hash[:data][metricType].length > 0
      end
      # The timestamp annotations are always present, but we're looking for
      # any further annotations.
      haveMetrics = true if hash[:data][:annotations].length > 2

      return unless haveMetrics

      hash[:data][:annotations][:initTimestamp] = hash[:data][:annotations][:initTimestamp].utc.iso8601(3)
      hash[:data][:annotations][:finalTimestamp] = hash[:data][:annotations][:finalTimestamp].utc.iso8601(3)
      @outputStream.write(hash.to_json)
    end

    def record(tsdMetricEvent)
      receive(tsdMetricEvent)
    end

    private

    def proxyValuesProperty(hash)
      hash.merge(hash) do |key, val, _|
        {values: val}
      end
    end

  end
end
