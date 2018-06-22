#!/bin/bash
set -x

# Copyright 2018 Inscope Metrics
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

sleep 5
/usr/bin/systemctl start scylla-server
sleep 15
/usr/bin/systemctl start kairosdb
sleep 5
/usr/bin/systemctl start grafana-server
sleep 5

# Setup Grafana KairosDb data source
for file in ./data/grafana/data-sources/*.json; do
  [ -e "${file}" ] || continue
  curl -X POST \
    --user admin:admin \
    -H 'content-type: application/json;charset=UTF-8' \
    --data "@${file}" \
    http://localhost:3000/api/datasources
done

# Setup Grafana dashboards
for file in ./data/grafana/dashboards/*.json; do
  [ -e "${file}" ] || continue
  curl -X POST \
    --user admin:admin \
    -H 'content-type: application/json' \
    --data "@${file}" \
    http://localhost:3000/api/dashboards/db
done

exit 0
