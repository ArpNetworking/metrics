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

min_cagg_count=1
max_cagg_count=3
default_cagg_count=1
cagg_count=${default_cagg_count}

mad_jvm_xms="64m"
mad_jvm_xmx="1024m"
cagg_jvm_xms="64m"
cagg_jvm_xmx="1024m"
mportal_jvm_xms="64m"
mportal_jvm_xmx="1024m"

mad_debug_port=9001
mportal_debug_port=9002

# Cluster aggregator debug ports are sequentially opened with an offset from
# the base port below. The first cluster aggregator process opens 9101.
cagg_debug_base_port=9100

function show_help {
  echo "Usage:"
  echo "./build.sh -h|-?"
  echo "./build.sh -s SERVICE [-t COUNT] [-c] [-p] [-n] [-v]"
  echo "./build.sh -a [-c] [-p] [-n] [-v]"
  echo ""
  echo " -h|-? -- print this help message"
  echo " -s service -- specify a service to start "
  echo "               services accepted:"
  echo "                 mad"
  echo "                 cagg"
  echo "                 mportal"
  echo "                 ckg"
  echo " -a -- start all services"
  echo " -t -- the number of cagg instances from ${min_cagg_count} to ${max_cagg_count}; default is ${default_cagg_count}"
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
  if [ -n "$pid_metrics_portal" ]; then
    if is_dead "$pid_metrics_portal"; then
      echo "Exited: metrics-portal ($pid_metrics_portal)"
      pid_metrics_portal=
    fi
  fi
  for i in `seq 1 ${cagg_count}`; do
    var="pid_cluster_agg_${i}"
    pid="${!var}"
    if [ -n "$pid" ]; then
      if is_dead "$pid"; then
        echo "Exited: cluster-aggregator ${i} ($pid)"
        declare "pid_cluster_agg_${i}="
      fi
    fi
  done
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
while getopts ":h?vnpacs:t:" opt; do
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
    t)
      cagg_count="$OPTARG"
      if ! [ "${cagg_count}" -eq "${cagg_count}" ] 2> /dev/null
      then
        echo "The number of cagg instances must be an integer."
        exit 1
      fi
      if [ ${cagg_count} -lt ${min_cagg_count} ]; then
        echo "The number of cagg instances must be at least ${min_cagg_count}."
        exit 1
      fi
      if [ ${cagg_count} -gt ${max_cagg_count} ]; then
        echo "The number of cagg instances must be at most ${max_cagg_count}."
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
  ./jdk-wrapper.sh ./mvnw install -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
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
  akka_seed_nodes=""
  array_index=0
  for i in `seq 1 ${cagg_count}`; do
    cagg_akka_port=$(( 2551 + ${i}))
    akka_seed_nodes+="\"-Dakka.cluster.seed-nodes.${array_index}=akka.tcp://Metrics@127.0.0.1:${cagg_akka_port}\" "
    array_index=$(( ${array_index} + 1 ))
  done
  for i in `seq 1 ${cagg_count}`; do
    double_i=$(( 2 * ${i}))

    cagg_http_port=$(( 7066 + ${double_i}))
    cagg_tcp_port=$(( 7065 + ${double_i}))
    cagg_akka_port=$(( 2551 + ${i}))
    cagg_debug_port=$(( ${cagg_debug_base_port} + ${i}))

    ./jdk-wrapper.sh ./target/appassembler/bin/cluster-aggregator \
        "-Xdebug" \
        "-XX:+HeapDumpOnOutOfMemoryError" \
        "-XX:HeapDumpPath=${dir_cluster_agg}/logs/cagg.${i}.oom.hprof" \
        "-XX:+PrintGCDetails" \
        "-XX:+PrintGCDateStamps" \
        "-Xloggc:${dir_cluster_agg}/logs/cagg.${i}.gc.log" \
        "-XX:NumberOfGCLogFiles=2" \
        "-XX:GCLogFileSize=50M" \
        "-XX:+UseGCLogFileRotation" \
        "-Xms${cagg_jvm_xms}" \
        "-Xmx${cagg_jvm_xmx}" \
        "-XX:+UseStringDeduplication" \
        "-XX:+UseG1GC" \
        ${akka_seed_nodes} \
        "-Duser.timezone=UTC" \
        "-DCAGG_ID=${i}" \
        "-DhttpPort=${cagg_http_port}" \
        "-DaggregationPort=${cagg_tcp_port}" \
        "-DakkaConfiguration.akka.remote.netty.tcp.port=${cagg_akka_port}" \
        "-Dcassandra-snapshot-store.contact-points.0=localhost" \
        "-Dcassandra-journal.contact-points.0=localhost" \
        "-Xrunjdwp:server=y,transport=dt_socket,address=${cagg_debug_port},suspend=n" \
        "-Dlogback.configurationFile=${dir}/config/cagg/logback.xml" \
        -- \
        "${dir}/config/cagg/config.conf" &
    pid=$!
    declare "pid_cluster_agg_${i}=${pid}"
    echo "Started: cluster-aggregator ${i} of ${cagg_count} ($pid)"
    if [ -n "$pinger" ]; then
      ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:${cagg_http_port}/ping      " -n "cagg-${i}" -p "${pid}" &
    fi

  done
  popd &> /dev/null
  pushd ${dir} &> /dev/null
  for i in `seq 1 ${cagg_count}`; do
      double_i=$(( 2 * ${i}))
      cagg_http_port=$(( 7066 + ${double_i}))
      vagrant ssh -- -N -R ${cagg_http_port}:localhost:${cagg_http_port} &
  done
  popd &> /dev/null
fi

if [ $start_mad -gt 0 ]; then
  pushd ${dir_mad} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  mkdir -p "${dir_mad}/logs"
  ./jdk-wrapper.sh ./target/appassembler/bin/mad \
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
  mkdir -p "${dir_metrics_portal}/logs"
  ./jdk-wrapper.sh ./target/appassembler/bin/metrics-portal \
      "-Dconfig.file=${dir}/config/metrics-portal/dev.conf" \
      "-Dlogger.file=${dir}/config/metrics-portal/logback.xml" \
      "-XX:+HeapDumpOnOutOfMemoryError" \
      "-XX:HeapDumpPath=${dir_metrics_portal}/logs/metrics-portal.oom.hprof" \
      "-XX:+PrintGCDetails" \
      "-XX:+PrintGCDateStamps" \
      "-Xloggc:${dir_metrics_portal}/logs/metrics-portal.gc.log" \
      "-XX:NumberOfGCLogFiles=2" \
      "-XX:GCLogFileSize=50M" \
      "-XX:+UseGCLogFileRotation" \
      "-Xms${mportal_jvm_xms}" \
      "-Xmx${mportal_jvm_xmx}" \
      "-XX:+UseStringDeduplication" \
      "-XX:+UseG1GC" \
      "-Duser.timezone=UTC" \
      "-Xdebug" \
      "-Xrunjdwp:server=y,transport=dt_socket,address=${mportal_debug_port},suspend=n" \
      "--" \
      "${dir_metrics_portal}" &
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
