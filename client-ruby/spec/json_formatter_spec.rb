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

require "tsd_metrics/json_formatting_sink"
require "json"
require "json-schema"
require 'timecop'
require 'time'

describe "JsonFormattingSink" do
  let(:outputStreamMock) do
    mock = double()
  end
  let(:minimalStruct) do
    {
      gauges: {
        "myGauge" => [{value: 4}]
      },
      timers: {
        "myGauge" => [{value: 427297}]
      },
      counters: {
        "myCounter" => [{value: 16238}, {value: 948}]
      },
      annotations: {
        "myAnnotation" => "Nevermore, quoth the Raven",
        initTimestamp: Time.parse("2014-11-02T03:33:38.000Z"),
        finalTimestamp: Time.parse("2014-11-02T03:33:40.838Z")
      }
    }
  end

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
  let(:receiver) { JsonFormattingSink.new(outputStreamMock) }

  def captureOutput(metricStructMock)
    json = nil
    allow(outputStreamMock).to receive(:write) do |outputJson|
      json = outputJson
    end
    receiver.receive(metricStructMock)
    json
  end

  it "contains all the metric data" do

    jsonString = captureOutput(double(minimalStruct))
    expect(jsonString).to include("427297")
    expect(jsonString).to include("16238")
    expect(jsonString).to include("948")
    expect(jsonString).to include('"Nevermore, quoth the Raven"')
    expect(jsonString).to include('"2014-11-02T03:33:38.000Z"')
    expect(jsonString).to include('"2014-11-02T03:33:40.838Z"')

  end

  it "includes the time as ISO8601 in UTC" do
    timeString = '"2014-11-02T03:33:38.000Z"'
    Timecop.freeze(Time.parse(timeString))

    jsonString = captureOutput(double(minimalStruct))
    expect(jsonString).to include(timeString)
    Timecop.return
  end

  it "conforms to the JSON schema" do
    jsonString = captureOutput(double(minimalStruct))
    JSON::Validator.validate!("../doc/query-log-schema-2e.json", jsonString)
  end

  it "does not output anything when no samples are present" do
    expect(outputStreamMock).to_not receive(:write)
    receiver.receive(double(zeroSampleStruct))
  end
end
