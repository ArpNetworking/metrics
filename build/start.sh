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
start_ckg=0
clear_logs=
pinger=
skip_build=

mad_jvm_xms="64m"
mad_jvm_xmx="1024m"
cagg_jvm_xms="64m"
cagg_jvm_xmx="1024m"
mportal_jvm_xms="64m"
mportal_jvm_xmx="1024m"

mad_debug_port=9001
cagg_debug_port=9002
mportal_debug_port=9003

function show_help {
  echo "Usage:"
  echo "./build.sh -h|-?"
  echo "./build.sh -s SERVICE [-c] [-p] [-n] [-v]"
  echo "./build.sh -a [-c] [-p] [n-] [-v]"
  echo ""
  echo " -h|-? -- print this help message"
  echo " -s service -- specify a service to start "
  echo "               services accepted:"
  echo "                 mad"
  echo "                 cagg"
  echo "                 mportal"
  echo "                 ckg"
  echo " -a -- start all services"
  echo " -c -- clear logs"
  echo " -p -- run pinger"
  echo " -n -- don't build"
  echo " -v -- verbose mode"
}

function is_dead() {
  local l_pid=$1
  kill -0 $l_pid &> /dev/null && return 1 || return 0
}

function handle_interrupt {
  if [ $start_ckg -gt 0 ]; then
    pushd ${dir} &> /dev/null
    vagrant ssh -c 'sudo /vagrant/vagrant-stop.sh'
    vagrant suspend
    popd &> /dev/null
  fi
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
while getopts ":h?vnpacs:" opt; do
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
    n)
      skip_build=1
      ;;
    a)
      services+=("mad")
      start_mad=1
      services+=("cagg")
      start_cluster_agg=1
      services+=("mportal")
      start_metrics_portal=1
      services+=("ckg")
      start_ckg=1
      ;;
    s)
      lower_service=$(echo "$OPTARG" | tr '[:upper:]' '[:lower:]')
      if [ ${lower_service} == "mad" ]; then
        services+=("mad")
        start_mad=1
      elif [ ${lower_service} == "cagg" ]; then
        services+=("cluster-aggregator")
        start_cluster_agg=1
      elif [ ${lower_service} == "mportal" ]; then
        services+=("metrics-portal")
        start_metrics_portal=1
      elif [ ${lower_service} == "ckg" ]; then
        services+=("ckg")
        start_ckg=1
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
if [ $start_metrics_portal -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_metrics_portal} &> /dev/null
  ./activator stage
  if [ "$?" -ne 0 ]; then echo "Build failed: metrics-portal"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_mad -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_mad} &> /dev/null
  ./jdk-wrapper.sh ./mvnw install -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then echo "Build failed: mad"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_cluster_agg -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_cluster_agg} &> /dev/null
  ./jdk-wrapper.sh ./mvnw install -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then echo "Build failed: cluster-aggregator"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_ckg -gt 0 ]; then
  # IMPORTANT: Do not skip "build" for Vagrant!
  pushd ${dir} &> /dev/null
  vagrant up
  if [ "$?" -ne 0 ]; then echo "Build failed: ckg"; exit 1; fi
  popd &> /dev/null
fi

# Run projects
if [ $start_ckg -gt 0 ]; then
  pushd ${dir} &> /dev/null
  vagrant ssh -- -N -R 7090:localhost:7090 &
  vagrant ssh -- -N -R 8094:localhost:8094 &
  vagrant ssh -- -N -R 2003:localhost:2003 &
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8082           " -n "kairos" &
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8081/api/health" -n "graphana" &
  fi
  popd &> /dev/null
fi

if [ $start_cluster_agg -gt 0 ]; then
  pushd ${dir_cluster_agg} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  rm -rf ./target/h2
  rm -rf ./journal
  mkdir -p "${dir_cluster_agg}/logs"
  ./target/appassembler/bin/cluster-aggregator \
      "-Xdebug" \
      "-XX:+HeapDumpOnOutOfMemoryError" \
      "-XX:HeapDumpPath=${dir_cluster_agg}/logs/cagg.oom.hprof" \
      "-XX:+PrintGCDetails" \
      "-XX:+PrintGCDateStamps" \
      "-Xloggc:${dir_cluster_agg}/logs/cagg.gc.log" \
      "-XX:NumberOfGCLogFiles=2" \
      "-XX:GCLogFileSize=50M" \
      "-XX:+UseGCLogFileRotation" \
      "-Xms${cagg_jvm_xms}" \
      "-Xmx${cagg_jvm_xmx}" \
      "-XX:+UseStringDeduplication" \
      "-XX:+UseG1GC" \
      "-Duser.timezone=UTC" \
      "-Xrunjdwp:server=y,transport=dt_socket,address=${cagg_debug_port},suspend=n" \
      "-Dlogback.configurationFile=${dir}/config/cagg/logback.xml" \
      -- \
      "${dir}/config/cagg/config.conf" &
  pid_cluster_agg=$!
  echo "Started: cluster-aggregator ($pid_cluster_agg)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7066/ping      " -n "cagg" -p "${pid_cluster_agg}" &
  fi
  popd &> /dev/null
fi

if [ $start_mad -gt 0 ]; then
  pushd ${dir_mad} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  mkdir -p "${dir_mad}/logs"
  ./target/appassembler/bin/mad \
      "-Dlogback.configurationFile=${dir}/config/mad/logback.xml" \
      "-XX:+HeapDumpOnOutOfMemoryError" \
      "-XX:HeapDumpPath=${dir_mad}/logs/mad.oom.hprof" \
      "-XX:+PrintGCDetails" \
      "-XX:+PrintGCDateStamps" \
      "-Xloggc:${dir_mad}/logs/mad.gc.log" \
      "-XX:NumberOfGCLogFiles=2" \
      "-XX:GCLogFileSize=50M" \
      "-XX:+UseGCLogFileRotation" \
      "-Xms${mad_jvm_xms}" \
      "-Xmx${mad_jvm_xmx}" \
      "-XX:+UseStringDeduplication" \
      "-XX:+UseG1GC" \
      "-Duser.timezone=UTC" \
      "-Xdebug" \
      "-Xrunjdwp:server=y,transport=dt_socket,address=${mad_debug_port},suspend=n" \
      -- \
      "${dir}/config/mad/config.conf" &
  pid_mad=$!
  echo "Started: mad ($pid_mad)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7090/ping      " -n "mad" -p "${pid_mad}" &
  fi
  popd &> /dev/null
fi

if [ $start_metrics_portal -gt 0 ]; then
  pushd ${dir_metrics_portal} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  rm -rf ./target/h2
  mkdir -p "${dir_metrics_portal}/logs"
  ./target/universal/stage/bin/metrics-portal \
      "-Dconfig.file=${dir}/config/metrics-portal/dev.conf" \
      "-Dlogger.file=${dir}/config/metrics-portal/logback.xml" \
      "-J-XX:+HeapDumpOnOutOfMemoryError" \
      "-J-XX:HeapDumpPath=${dir_metrics_portal}/logs/metrics-portal.oom.hprof" \
      "-J-XX:+PrintGCDetails" \
      "-J-XX:+PrintGCDateStamps" \
      "-J-Xloggc:${dir_metrics_portal}/logs/metrics-portal.gc.log" \
      "-J-XX:NumberOfGCLogFiles=2" \
      "-J-XX:GCLogFileSize=50M" \
      "-J-XX:+UseGCLogFileRotation" \
      "-J-Xms${mportal_jvm_xms}" \
      "-J-Xmx${mportal_jvm_xmx}" \
      "-J-XX:+UseStringDeduplication" \
      "-J-XX:+UseG1GC" \
      "-J-Duser.timezone=UTC" \
      "-J-Xdebug" \
      "-J-Xrunjdwp:server=y,transport=dt_socket,address=${mportal_debug_port},suspend=n" &
  pid_metrics_portal=$!
  echo "Started: metrics-portal ($pid_metrics_portal)"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8080/ping      " -n "mportal" -p "${pid_metrics_portal}" &
  fi
  popd &> /dev/null
fi

popd &> /dev/null

# Wait for all running projects to exit
wait
