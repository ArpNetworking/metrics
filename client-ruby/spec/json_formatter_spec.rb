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

require "tsd_metrics/json_formatter_receiver"
require "json"

describe "JsonFormatterReceiver" do
  let(:outputStreamMock) do
    mock = double()
  end
  let(:minimalStruct) do
    {
      gauges: {
        "myGauge" => [4]
      },
      timers: {
        "myGauge" => [427297]
      },
      counters: {
        "myCounter" => [16238, 948]
      },
      annotations: {
        "myAnnotation" => "Nevermore, quoth the Raven",
        initTimestamp: 1393037729.0,
        finalTimestamp: 1393037735.0
      }
    }
  end

  # TODO(mhayter): Empty metrics should never get this far [MAI-174]
  let(:zeroSampleStruct) do
    {
      gauges: {
      },
      timers: {
      },
      counters: {
      },
      annotations: {
        initTimestamp: 1393037729.0,
        finalTimestamp: 1393037735.0
      }
    }
  end
  let(:receiver) { JsonFormatterReceiver.new(outputStreamMock) }

  def captureOutput(metricStructMock)
    json = nil
    allow(outputStreamMock).to receive(:write) do |outputJson|
      json = outputJson
    end
    receiver.receive(metricStructMock)
    json
  end

  it "formats a metric as json" do
    expectedJson = '{"gauges":{"myGauge":[4]},"timers":{"myGauge":[427297]},"counters":{"myCounter":[16238,948]},"annotations":{"myAnnotation":"Nevermore, quoth the Raven","initTimestamp":1393037729.0,"finalTimestamp":1393037735.0}}'
    jsonString = captureOutput(double(minimalStruct))
    jsonString.should == expectedJson

  end
  it "outputs the inital and final timestamps in seconds since the epoch, millisecond accuracy" do

    startTime = Time.now.to_f
    finalTime = startTime + 10.567

    minimalStruct[:annotations][:initTimestamp] = startTime
    minimalStruct[:annotations][:finalTimestamp] = finalTime

    jsonString = captureOutput(double(minimalStruct))
    json = JSON.parse(jsonString)
    json["annotations"]["initTimestamp"].should === startTime.to_f
    json["annotations"]["finalTimestamp"].should === finalTime.to_f
  end

  it "does not output anything when no samples are present" do
    expect(outputStreamMock).to_not receive(:write)
    receiver.receive(double(zeroSampleStruct))
  end
end
