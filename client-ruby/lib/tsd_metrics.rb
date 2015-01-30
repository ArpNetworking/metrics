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

require_relative 'tsd_metrics/tsd_metric'
require_relative 'tsd_metrics/queue_writer'
require_relative 'tsd_metrics/async_queue_writer'
require_relative 'tsd_metrics/metric_builder_for_single_struct_receiver'
require_relative 'tsd_metrics/json_formatting_sink'
require 'thread'
require 'logger'

module TsdMetrics
  attr_reader :errorLogger
  @metricBuilder = nil
  @@errorLogger = nil

  # +filename+:: Optional: the relative or absolute path to output metrics. Default: 'query.log' in the working directory
  # +errorLogger+:: Optional: a place to which mis-uses of the library can be logged.
  #   Expects methods +info+, +warn+, +error+
  # +rollLogs+:: Optional: Have the library do log rolling; not thread-safe! Setting to false means the library-user should have external log-rolling in place. Default: true
  def self.init(providedOpts={})
    defaultOpts = {filename: "query.log", rollLogs: true}
    opts = defaultOpts.merge providedOpts
    @errorLogger = opts[:errorLogger] || Logger.new(STDOUT)

    outputFileQueue = Queue.new

    # JSON to queue
    writer = QueueWriter.new(outputFileQueue)

    # Metric to JSON
    formatterStructReveiver = JsonFormattingSink.new(writer)
    @metricBuilder = MetricBuilderForSingleStructReceiver.new(formatterStructReveiver)

    loggerOptions = [opts[:filename]]
    # Set the ':daily' option on the logger if we want to roll the logs
    loggerOptions.push(:daily) if opts[:rollLogs]
    # TODO(mhayter): Switch to hourly rolling (will need different log lib)
    # [MAI-173]
    # File writer
    toFileLogger = createHeaderlessLogger(*loggerOptions)

    # Queue to file
    @queueToFileWriter = AsyncQueueWriter.new(outputFileQueue, toFileLogger)
    @queueToFileWriter.start

    # Phusion Passenger loses all threads on a fork, this is added
    # so that the writing thread is recreated.
    # This code was found at
    # https://www.phusionpassenger.com/documentation/Users%20guide%20Nginx.html#_smart_spawning_caveat_2_the_need_to_revive_threads
    if defined?(PhusionPassenger)
      PhusionPassenger.on_event(:starting_worker_process) do |forked|
        if forked
          outputFileQueue.clear
          @queueToFileWriter.start
        else
          # We're in direct spawning mode. We don't need to do anything.
        end
      end
    end

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
