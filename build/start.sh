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
services=()
start_tsd_agg=0
pid_tsd_agg=
start_cluster_agg=0
pid_cluster_agg=
start_remet_proxy=0
pid_remet_proxy=
start_remet_gui=0
pid_remet_gui=
clear_logs=
pinger=

function show_help {
  echo "Usage:"
  echo "./build.sh -h|-?"
  echo "./build.sh -s SERVICE [-c] [-p] [-v]"
  echo "./build.sh -a [-c] [-p] [-v]"
  echo ""
  echo " -h|-? -- print this help message"
  echo " -s service -- specify a service to start "
  echo "               services accepted:"
  echo "                 tsd-aggregator"
  echo "                 cluster-aggregator"
  echo "                 remet-proxy"
  echo "                 remet-gui"
  echo " -a -- start all services"
  echo " -c -- clear logs"
  echo " -p -- run pinger"
  echo " -v -- verbose mode"
}

function is_dead() {
  local l_pid=$1
  kill -0 $l_pid &> /dev/null && return 1 || return 0
}

function handle_interrupt {
  kill $(jobs -p -r)
}

function handle_childexit {
  if [ -n "$pid_tsd_agg" ]; then
     if is_dead "$pid_tsd_agg"; then
        echo "Exited: tsd-aggregator ($pid_tsd_agg)"
        pid_tsd_agg=
     fi
  fi
  if [ -n "$pid_cluster_agg" ]; then
     if is_dead "$pid_cluster_agg"; then
        echo "Exited: cluster-aggregator ($pid_cluster_agg)"
        pid_cluster_agg=
     fi
  fi
  if [ -n "$pid_remet_proxy" ]; then
     if is_dead "$pid_remet_proxy"; then
        echo "Exited: remet-proxy ($pid_remet_proxy)"
        pid_remet_proxy=
     fi
  fi
  if [ -n "$pid_remet_gui" ]; then
     if is_dead "$pid_remet_gui"; then
        echo "Exited: remet-gui ($pid_remet_gui)"
        pid_remet_gui=
     fi
  fi
}

# Parse Options
OPTIND=1
while getopts ":h?vpacs:" opt; do
  case "$opt" in
    h|\?)
      show_help
      exit 0
      ;;
    v)
      verbose=1
      ;;
    c)
      clear_logs=1
      ;;
    p)
      pinger=1
      ;;
    a)
      services+=("tsd-aggregator")
      start_tsd_agg=1
      services+=("cluster-aggregator")
      start_cluster_agg=1
      services+=("remet-proxy")
      start_remet_proxy=1
      services+=("remet-gui")
      start_remet_gui=1
      ;;
    s)
      lower_service=$(echo "$OPTARG" | tr '[:upper:]' '[:lower:]')
      if [ ${lower_service} == "tsd-aggregator" ]; then
        services+=("tsd-aggregator")
        start_tsd_agg=1
      elif [ ${lower_service} == "cluster-aggregator" ]; then
        services+=("cluster-aggregator")
        start_cluster_agg=1
      elif [ ${lower_service} == "remet-proxy" ]; then
        services+=("remet-proxy")
        start_remet_proxy=1
      elif [ ${lower_service} == "remet-gui" ]; then
        services+=("remet-gui")
        start_remet_gui=1
      else
        echo "Unknown service ${OPTARG}"
        exit 1
      fi
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

echo "services = ${services[@]}"
if [ -z ${services} ]; then
  echo "no services to start"
  exit 1
fi

# Root directory
pushd $dir &> /dev/null
cd ..

# Setup handlers
set -m
set -e
trap handle_interrupt SIGINT SIGTERM
trap handle_childexit SIGCHLD

# Build verbose argument to child programs
verbose_arg=
if [ -n "$verbose" ]; then
  verbose_arg="-v"
fi

# Generate Code
# TODO(barp): Formalize this into standard build processes
if [ $start_tsd_agg -gt 0 ]; then
  pushd tsd/tsd-aggregator &> /dev/null
  version=`grep "version = '[^']*'" ../build.gradle | grep -o -P "'[^']+'" | grep -o -P "[^']+"`
  sha=`git log -n 1 --pretty=%H master -- "./"`
  mkdir -p src/main/resources
  echo "{\"name\":\"$project\",\"version\":\"$version\",\"sha\":\"$sha\"}" > "src/main/resources/status.json"
  popd &> /dev/null
fi

if [ $start_cluster_agg -gt 0 ]; then
  pushd tsd/cluster-aggregator &> /dev/null
  version=`grep "version = '[^']*'" ../build.gradle | grep -o -P "'[^']+'" | grep -o -P "[^']+"`
  sha=`git log -n 1 --pretty=%H master -- "./"`
  mkdir -p src/main/resources
  echo "{\"name\":\"$project\",\"version\":\"$version\",\"sha\":\"$sha\"}" > "src/main/resources/status.json"
  popd &> /dev/null
fi

# Build projects
if [ $start_remet_proxy -gt 0 ]; then
  pushd remet-proxy &> /dev/null
  activator stage
  popd &> /dev/null
fi

if [ $start_remet_gui -gt 0 ]; then
  pushd remet-gui &> /dev/null
  activator stage
  popd &> /dev/null
fi

if [ $start_tsd_agg -gt 0 -o $start_cluster_agg -gt 0 ]; then
  pushd tsd &> /dev/null
  ./gradlew installApp
  popd &> /dev/null
fi

# Run projects
if [ $start_remet_proxy -gt 0 ]; then
  pushd remet-proxy &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  ./target/universal/stage/bin/remet-proxy -Dhttp.port=7090 &
  pid_remet_proxy=$!
  echo "Started: remet-proxy ($pid_remet_proxy)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7090/ping" &
  fi
  popd &> /dev/null
fi

if [ $start_remet_gui -gt 0 ]; then
  pushd remet-gui &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  ./target/universal/stage/bin/remet-gui -Dhttp.port=8080 &
  pid_remet_gui=$!
  echo "Started: remet-gui ($pid_remet_gui)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8080/ping" &
  fi
  popd &> /dev/null
fi

if [ $start_cluster_agg -gt 0 ]; then
  pushd tsd/cluster-aggregator &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  ./build/install/cluster-aggregator/bin/cluster-aggregator ${dir}/start/clusteragg/config.json &
  pid_cluster_agg=$!
  echo "Started: cluster-aggregator ($pid_cluster_agg)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7066/ping" &
  fi
  popd &> /dev/null
fi


if [ $start_tsd_agg -gt 0 ]; then
  pushd tsd/tsd-aggregator &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  ./build/install/tsd-aggregator/bin/tsd-aggregator ${dir}/start/tsdagg/config.json &
  pid_tsd_agg=$!
  echo "Started: tsd-aggregator ($pid_tsd_agg)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:6080/ping" &
  fi
  popd &> /dev/null
fi

popd &> /dev/null

# Wait for all running projects to exit
wait
