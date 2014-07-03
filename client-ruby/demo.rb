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

require_relative 'lib/tsd_metrics'
filename = "my_logs.json"

TsdMetrics.init(filename)
# build metric, close
while true
  metric = TsdMetrics.buildMetric
  metric.setGauge("myGauge", 1 / ((Time.now.to_f ** 2) + 1))
  metric.setGauge("myGauge2", 20)
  metric.annotate("myAnnotation", "Hi there")
  metric.close
  sleep (0.5)
end
