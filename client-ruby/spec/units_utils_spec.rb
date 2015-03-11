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

require 'tsd_metrics/units'

describe "UnitsUtils" do
  describe "isValidUnitValue?" do
    it "allows noUnit as a valid value" do
      TsdMetrics::UnitsUtils.isValidUnitValue?(:noUnit).should be_true
    end
    it "validates a valid unit" do
      TsdMetrics::UnitsUtils.isValidUnitValue?(:second).should be_true
    end
    it "invalidates a non-valid unit" do
      TsdMetrics::UnitsUtils.isValidUnitValue?(:parsecs).should be_false
    end
  end
end
