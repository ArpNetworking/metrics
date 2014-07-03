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

require_relative 'tsd_metrics/tsd_metric_2c'
require_relative 'tsd_metrics/queue_writer'
require_relative 'tsd_metrics/async_queue_writer'
require_relative 'tsd_metrics/metric_builder_for_single_struct_receiver'
require_relative 'tsd_metrics/json_formatter_receiver'
require 'thread'
require 'logger'

module TsdMetrics
  attr_reader :errorLogger
  @metricBuilder = nil
  @errorLogger = nil

  # +errorLogger+:: Optional: a place to which mis-uses of the library can be logged.
  #   Expects methods +info+, +warn+, +error+
  def self.init(filename="query.log", errorLogger=nil)
    @errorLogger = errorLogger || Logger.new(STDOUT)

    outputFileQueue = Queue.new

    # JSON to queue
    writer = QueueWriter.new(outputFileQueue)

    # Metric to JSON
    formatterStructReveiver = JsonFormatterReceiver.new(writer)
    @metricBuilder = MetricBuilderForSingleStructReceiver.new(formatterStructReveiver)

    # TODO(mhayter): Switch to hourly rolling (will need different log lib)
    # [MAI-173]
    # File writer
    toFileLogger = createHeaderlessLogger(filename, "daily")

    # Queue to file
    @queueToFileWriter = AsyncQueueWriter.new(outputFileQueue, toFileLogger)
    @queueToFileWriter.start
  end

  def self.buildMetric
    init() if @metricBuilder == nil
    @metricBuilder.build
  end

  private

  def self.createHeaderlessLogger(*args)
    # Disable the logger header
    Logger::LogDevice.class_eval do
      alias orig_add_log_header add_log_header

      def add_log_header(file)
      end
    end

    # Quick, create an instance
    logger = Logger.new(*args)

    # Restore the old method:
    Logger::LogDevice.class_eval do
      alias add_log_header orig_add_log_header
    end

    logger
  end

end
