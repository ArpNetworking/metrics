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

Gem::Specification.new do |s|
  s.name        = 'tsd_metrics'
  s.version     = '0.2.5'
  s.date        = '2015-01-15'
  s.summary     = "TSD Metrics client for Ruby"
  s.description = "A client for logging to TSD-Aggregation-logging-compatible JSON files"
  s.authors     = ["Matthew Hayter"]
  s.email       = 'matthewhayter@gmail.com'
  s.licenses    = ['Apache License, Version 2.0']

  s.files         = `git ls-files`.split($/)
  s.executables   = s.files.grep(%r{^bin/}) { |f| File.basename(f) }
  s.test_files    = s.files.grep(%r{^spec/})
  s.require_paths = ["lib", "resources"]


  s.add_development_dependency 'rspec', '2.14.1'
  s.add_development_dependency 'timecop', '0.7.1'
  s.add_development_dependency 'pry', '0.9.12.6'
  s.add_development_dependency 'json-schema', '2.4.1'
end
