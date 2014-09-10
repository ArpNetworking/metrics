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
  # Not threadsafe
  class AsyncQueueWriter
    def initialize(queue, logger)
      @queue = queue
      @logger = logger
    end

    def start
      Thread.new do
        while true
          tryPopQueueToFile
        end
      end
    end

    private

    def tryPopQueueToFile
      line = @queue.pop
      return if line == nil
      @logger << line
      @logger << "\n"
    end
  end
end
