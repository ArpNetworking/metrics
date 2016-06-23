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
start_mad=0
pid_mad=
start_cluster_agg=0
pid_cluster_agg=
start_metrics_portal=0
pid_metrics_portal=
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
  echo "                 mad"
  echo "                 cluster-aggregator"
  echo "                 metrics-portal"
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
  if [ -n "$pid_mad" ]; then
     if is_dead "$pid_mad"; then
        echo "Exited: mad ($pid_mad)"
        pid_mad=
     fi
  fi
  if [ -n "$pid_cluster_agg" ]; then
     if is_dead "$pid_cluster_agg"; then
        echo "Exited: cluster-aggregator ($pid_cluster_agg)"
        pid_cluster_agg=
     fi
  fi
  if [ -n "$pid_metrics_portal" ]; then
     if is_dead "$pid_metrics_portal"; then
        echo "Exited: metrics-portal ($pid_metrics_portal)"
        pid_metrics_portal=
     fi
  fi
}

function find_path_up {
  local l_name=$1
  local l_path=${dir}
  local l_found=false
  until [[ "${l_path}" == "/" ]] || ${l_found}; do
    if [[ -e "${l_path}/${l_name}" ]]; then
      l_found=true
      l_path="${l_path}/${l_name}"
    else
      l_path=$(dirname "${l_path}")
    fi
  done
  echo ${l_path};
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
      services+=("mad")
      start_mad=1
      services+=("cluster-aggregator")
      start_cluster_agg=1
      services+=("metrics-portal")
      start_metrics_portal=1
      ;;
    s)
      lower_service=$(echo "$OPTARG" | tr '[:upper:]' '[:lower:]')
      if [ ${lower_service} == "mad" ]; then
        services+=("mad")
        start_mad=1
      elif [ ${lower_service} == "cluster-aggregator" ]; then
        services+=("cluster-aggregator")
        start_cluster_agg=1
      elif [ ${lower_service} == "metrics-portal" ]; then
        services+=("metrics-portal")
        start_metrics_portal=1
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

# Find project directories
dir_mad=`find_path_up metrics-aggregator-daemon`
dir_cluster_agg=`find_path_up metrics-cluster-aggregator`
dir_metrics_portal=`find_path_up metrics-portal`

# Verify locations for each requested project
if [ "${start_mad}" -gt 0 ] && [ ! -d "${dir_mad}" ]; then
  echo "No directory found for requested project mad"
  exit 1
fi
if [ "${start_cluster_agg}" -gt 0 ] && [ ! -d "${dir_cluster_agg}" ]; then
  echo "No directory found for requested project cluster-aggregator"
  exit 1
fi
if [ "${start_metrics_portal}" -gt 0 ] && [ ! -d "${dir_metrics_portal}" ]; then
  echo "No directory found for requested project metrics-portal"
  exit 1
fi

# Build verbose argument to child programs
verbose_arg=
if [ -n "$verbose" ]; then
  verbose_arg="-v"
fi

# Build projects
if [ $start_metrics_portal -gt 0 ]; then
  pushd ${dir_metrics_portal} &> /dev/null
  ./activator stage
  if [ "$?" -ne 0 ]; then echo "Build failed: metrics-portal"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_mad -gt 0 ]; then
  pushd ${dir_mad} &> /dev/null
  ./mvnw install -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then echo "Build failed: mad"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_cluster_agg -gt 0 ]; then
  pushd ${dir_cluster_agg} &> /dev/null
  ./mvnw install -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then echo "Build failed: cluster-aggregator"; exit 1; fi
  popd &> /dev/null
fi

# Run projects
if [ $start_cluster_agg -gt 0 ]; then
  pushd ${dir_cluster_agg} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  rm -rf ./target/h2
  rm -rf ./journal
  rm -rf ./query.log
  ./target/appassembler/bin/cluster-aggregator ${dir}/config/clusteragg/config.json &
  pid_cluster_agg=$!
  echo "Started: cluster-aggregator ($pid_cluster_agg)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7066/ping" &
  fi
  popd &> /dev/null
fi

if [ $start_mad -gt 0 ]; then
  pushd ${dir_mad} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  rm -rf ./query.log
  ./target/appassembler/bin/mad ${dir}/config/mad/config.json &
  pid_mad=$!
  echo "Started: mad ($pid_mad)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7090/ping" &
  fi
  popd &> /dev/null
fi

if [ $start_metrics_portal -gt 0 ]; then
  pushd ${dir_metrics_portal} &> /dev/null
  if [ -e "./target/universal/stage/RUNNING_PID" ]; then
    pid=`cat ./target/universal/stage/RUNNING_PID`
    if kill -0 ${pid}; then
      echo "Service still running: metrics-portal"
      exit 1
    else
      echo "Removing pid file for metrics-portal"
      rm "./target/universal/stage/RUNNING_PID"
    fi
  fi
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  rm -rf ./target/h2
  ./target/universal/stage/bin/metrics-portal -Dhttp.port=8080 -Dconfig.resource=portal.application.conf &
  pid_metrics_portal=$!
  echo "Started: metrics-portal ($pid_metrics_portal)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8080/ping" &
  fi
  popd &> /dev/null
fi

popd &> /dev/null

# Wait for all running projects to exit
wait
