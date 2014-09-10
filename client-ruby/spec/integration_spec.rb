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

require "tsd_metrics"
require "thread"
require "pry"

describe "creating metrics" do
  before(:each) do
    begin
      File.delete filename
    rescue
    end
  end
  let(:filename) { "my_file.json" }

  def checkFileForNWellFormedLines(n)
    File.open filename, "r" do |file|
      count = 0
      while true
        line = file.readline rescue break
        line[0].should == '{'
        line[-2].should == '}'
        count += 1
      end
      count.should == n
    end
  end

  it "writes three metrics to the file" do
    # init
    TsdMetrics.init(filename)
    # build metric, close
    metric = TsdMetrics.buildMetric
    metric.setGauge("myGauge", 20)
    metric.close
    # metric, close
    metric = TsdMetrics.buildMetric
    metric.setGauge("myGauge", 20)
    metric.close
    # metric, close
    metric = TsdMetrics.buildMetric
    metric.setGauge("myGauge", 20)
    metric.close

    sleep(0.1)

    checkFileForNWellFormedLines(3)
  end

  it "does not write empty metrics to the file" do
    TsdMetrics.init(filename)
    # build metric, close
    metric = TsdMetrics.buildMetric
    metric.setGauge("myGauge", 20)
    metric.close
    # metric, close
    metric = TsdMetrics.buildMetric
    metric.setGauge("myGauge", 20)
    metric.close

    # EMPTY metric; one unstopped timer, close
    metric = TsdMetrics.buildMetric
    metric.startTimer("myUnstoppedTimer")
    metric.close

    # EMPTY metric
    metric = TsdMetrics.buildMetric
    metric.close

    sleep(0.1)

    checkFileForNWellFormedLines(2)
  end
end
