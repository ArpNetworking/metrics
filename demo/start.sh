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

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

verbose=
clear_logs=
pinger=
skip_build=

function show_help {
  echo "Usage:"
  echo "./build.sh -h|-?"
  echo "./build.sh [-p] [-v]"
  echo ""
  echo " -h|-? -- print this help message"
  echo " -p -- run pinger"
  echo " -v -- verbose mode"
}

function is_dead() {
  local l_pid=$1
  kill -0 $l_pid &> /dev/null && return 1 || return 0
}

function handle_interrupt {
  kill $(jobs -p -r)
  pushd ${dir} &> /dev/null
  vagrant ssh -c 'sudo /vagrant/vagrant-stop.sh'
  vagrant suspend
  popd &> /dev/null
}

# Parse Options
OPTIND=1
while getopts ":h?vnpacs:" opt; do
  case "$opt" in
    h|\?)
      show_help
      exit 0
      ;;
    v)
      verbose=1
      ;;
    p)
      pinger=1
      ;;
    ?)
      echo "ERROR: invalid option -$OPTARG"
      show_help
      exit 1
      ;;
    :)
      echo "ERROR: option -$OPTARG requires value"
      show_help
      exit 1
      ;;
  esac
done

# Root directory
pushd $dir &> /dev/null
cd ..

# Setup handlers
set -m
set -e
trap handle_interrupt SIGINT SIGTERM

# Build verbose argument to child programs
verbose_arg=
if [ -n "$verbose" ]; then
  verbose_arg="-v"
fi

# Build projects
pushd ${dir} &> /dev/null
vagrant up
if [ "$?" -ne 0 ]; then echo "Build failed: ckg"; exit 1; fi
popd &> /dev/null

# Run projects
pushd ${dir} &> /dev/null
if [ -n "$pinger" ]; then
  ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8082           " -n "kairos" &
  ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8081/api/health" -n "graphana" &
  ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7066/ping      " -n "cagg" &
  ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7090/ping      " -n "mad" &
  ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8080/ping      " -n "mportal" &
fi
popd &> /dev/null

popd &> /dev/null

# Wait for all running projects to exit
wait
