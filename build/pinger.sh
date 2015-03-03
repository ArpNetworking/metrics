#!/bin/bash

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
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

uri=

function show_help {
  echo "Usage:"
  echo "./pinger.sh -h|-?"
  echo "./pinger.sh -u URI [-v]"
  echo ""
  echo " -h|-? -- print this help message"
  echo " -u uri -- specify a uri to ping"
  echo " -v -- verbose mode"
}

# Parse Options
OPTIND=1
while getopts ":h?vu:" opt; do
  case "$opt" in
    h|\?)
      show_help
      exit 0
      ;;
    v)
      verbose=1
      ;;
    u)
      uri="$OPTARG"
      ;;
  esac
done

# Ping
while :
do
  result=
  if type curl >/dev/null 2>&1; then
    result=`curl -s -o - ${uri}`
  elif type wget >/dev/null 2>&1; then
    result=`wget -qO- ${uri}`
  else
    echo "ERROR: No supported http query command found!"
    exit 1
  fi
  if [ -n "$verbose" ]; then
    echo "Pinging ${uri}... $result"
  fi
  sleep 0.5
done
