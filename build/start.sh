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
pid_haproxy=
start_metrics_portal=0
pid_metrics_portal=
start_kairosdb=0
pid_kairosdb=
start_cg=0
pid_cassandra=
pid_grafana=
start_telegraf=0
pid_telegraf=
clear_logs=
pinger=
skip_build=

min_cagg_count=1
max_cagg_count=3
default_cagg_count=1
cagg_count=${default_cagg_count}

docker_haproxy_version=2.1.4-alpine
docker_cassandra_version=3.11
docker_grafana_version=6.7.3
docker_telegraf_version=1.14.2

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

host_ip_address=""
if command -v ip > /dev/null; then
  # Linux
  host_ip_address=$(ip route get 8.8.8.8 | sed -n '/src/{s/.*src *\([^ ]*\).*/\1/p;q}')
elif command -v dig > /dev/null; then
  # FreeBSD (e.g. Mac)
  host_ip_address=$(dig +short `hostname -f`)
fi

if [ -z "${host_ip_address}" ]; then
  printf "\e[0;31mError\e[0m: Unable to determine local ip address; please add the argument '-i <address>'\n"
  exit 1
fi

target="${dir}/../target"
mkdir -p "${target}"

function show_help {
  printf "Usage:\n"
  printf "./build.sh -h|-?\n"
  printf "./build.sh -s SERVICE [-t COUNT] [-c] [-p] [-n] [-v] [-i address]\n"
  printf "./build.sh -a [-c] [-p] [-n] [-v] [-i address]\n"
  printf "\n"
  printf " -h|-? -- print this help message\n"
  printf " -s service -- specify a service to start\n"
  printf "               services accepted:\n"
  printf "                 mad\n"
  printf "                 cagg\n"
  printf "                 mportal\n"
  printf "                 kairosdb\n"
  printf "                 cg\n"
  printf "                 telegraf\n"
  printf " -a -- start all services\n"
  printf " -c -- clear logs\n"
  printf " -i address -- specify the ip address\n"
  printf " -n -- don't build\n"
  printf " -p -- run pinger\n"
  printf " -t -- the number of cagg instances from ${min_cagg_count} to ${max_cagg_count}; default is ${default_cagg_count}\n"
  printf " -v -- verbose mode\n"
}

function is_dead() {
  local l_pid=$1
  kill -0 "${l_pid}" &> /dev/null && return 1 || return 0
}

function handle_exit {
  if [ ${start_telegraf} -gt 0 ]; then
    if [ -n "${pid_telegraf}" ]; then
      printf "Stopping telegraf...\n"
      docker kill "${pid_telegraf}" &> /dev/null || true
      pid_telegraf=
    fi
  fi
  if [ ${start_cluster_agg} -gt 0 ]; then
    if [ -n "${pid_haproxy}" ]; then
      printf "Stopping haproxy...\n"
      docker kill "${pid_haproxy}" &> /dev/null || true
      pid_haproxy=
    fi
  fi
  if [ ${start_kairosdb} -gt 0 ]; then
    if [ -n "${pid_kairosdb}" ]; then
      printf "Stopping kairosdb...\n"
      docker kill "${pid_kairosdb}" &> /dev/null || true
      pid_kairosdb=
    fi
  fi
  if [ $start_cg -gt 0 ]; then
    if [ -n "${pid_grafana}" ]; then
      printf "Stopping grafana...\n"
      docker kill "${pid_grafana}" &> /dev/null || true
      pid_grafana=
    fi
    if [ -n "${pid_cassandra}" ]; then
      printf "Stopping cassandra...\n"
      docker kill "${pid_cassandra}" &> /dev/null || true
      pid_cassandra=
    fi
  fi
  job_list=$(jobs -p -r)
  if [ -n "${job_list}" ]; then
    printf "Stopping other jobs...\n"
    kill ${job_list} &> /dev/null
    wait ${job_list} 2>/dev/null
  fi
}

function handle_childexit {
  if [ -n "${pid_mad}" ]; then
    if is_dead "${pid_mad}"; then
      printf "Exited: metrics-aggregator-daemon (${pid_mad})\n"
      pid_mad=
    fi
  fi
  if [ -n "${pid_metrics_portal}" ]; then
    if is_dead "${pid_metrics_portal}"; then
      printf "Exited: metrics-portal (${pid_metrics_portal})\n"
      pid_metrics_portal=
    fi
  fi
  for i in `seq 1 ${cagg_count}`; do
    var="pid_cluster_agg_${i}"
    pid="${!var}"
    if [ -n "${pid}" ]; then
      if is_dead "${pid}"; then
        printf "Exited: cluster-aggregator ${i} (${pid})\n"
        unset "pid_cluster_agg_${i}"
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
    elif [[ -e "${l_path}/ArpNetworking/${l_name}" ]]; then
      l_found=true
      l_path="${l_path}/ArpNetworking/${l_name}"
    elif [[ -e "${l_path}/InscopeMetrics/${l_name}" ]]; then
      l_found=true
      l_path="${l_path}/InscopeMetrics/${l_name}"
    else
      l_path=$(dirname "${l_path}")
    fi
  done
  if [ "${l_found}" == true ]; then
    echo ${l_path};
  else
    # return some path that does not exist
    echo ""
  fi
}

function assert_valid_path {
  local l_name=$1
  local l_required=$2
  local l_path=$3

  if [ "${l_required}" -eq 1 ]; then
    if [ ! -d "${l_path}" ]; then
      printf "\e[0;31mError:\e[0m Required directory not found for ${l_name}\n"
      exit 1
    fi
  fi
  return 0
}

function replace_id_address {
  local l_src=$1
  local l_dest=$2
  sed "s/<HOST_IP_ADDRESS>/${host_ip_address}/g" "${l_src}" > "${l_dest}"
}

function getProjectVersion {
  ./jdk-wrapper.sh ./mvnw org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout 2> /dev/null | tail -n 1
}

# Parse Options
OPTIND=1
while getopts ":h?vnpacs:t:i:" opt; do
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
      services+=("kairosdb")
      start_kairosdb=1
      services+=("cg")
      start_cg=1
      services+=("telegraf")
      start_telegraf=1
      ;;
    s)
      lower_service=$(echo "$OPTARG" | tr '[:upper:]' '[:lower:]')
      if [ ${lower_service} == "mad" ] || [ ${lower_service} == "metrics-aggregator-daemon" ]; then
        services+=("mad")
        start_mad=1
      elif [ ${lower_service} == "cagg" ] || [ ${lower_service} == "cluster-aggregator" ] ; then
        services+=("cluster-aggregator")
        start_cluster_agg=1
      elif [ ${lower_service} == "mportal" ] || [ ${lower_service} == "metrics-portal" ]; then
        services+=("metrics-portal")
        start_metrics_portal=1
      elif [ ${lower_service} == "kdb" ] || [ ${lower_service} == "kairosdb" ]; then
        services+=("kairosdb")
        start_kairosdb=1
      elif [ ${lower_service} == "cg" ]; then
        services+=("cg")
        start_cg=1
      elif [ ${lower_service} == "telegraf" ]; then
        services+=("telegraf")
        start_telegraf=1
      else
        printf "\e[0;31mError\e[0m: Unknown service ${OPTARG}\n"
        exit 1
      fi
      ;;
    i)
      host_ip_address="$OPTARG"
      ;;
    t)
      cagg_count="$OPTARG"
      if ! [ "${cagg_count}" -eq "${cagg_count}" ] 2> /dev/null
      then
        printf "\e[0;31mError\e[0m: The number of cagg instances must be an integer.\n"
        exit 1
      fi
      if [ ${cagg_count} -lt ${min_cagg_count} ]; then
        printf "\e[0;31mError\e[0m: The number of cagg instances must be at least ${min_cagg_count}.\n"
        exit 1
      fi
      if [ ${cagg_count} -gt ${max_cagg_count} ]; then
        printf "\e[0;31mError\e[0m: The number of cagg instances must be at most ${max_cagg_count}.\n"
        exit 1
      fi
      ;;
    ?)
      printf "\e[0;31mError\e[0m: Invalid option -$OPTARG\n"
      show_help
      exit 1
      ;;
    :)
      printf "\e[0;31mError\e[0m: Option -$OPTARG requires value\n"
      show_help
      exit 1
      ;;
  esac
done

if [ -n "${verbose}" ]; then
  printf "services = ${services[@]}\n"
fi
if [ -z ${services} ]; then
  printf "No services to start\n"
  exit 0
fi

# Root directory
pushd $dir &> /dev/null
cd ..

# Setup handlers
set -m
set -e
trap handle_exit EXIT
trap handle_childexit SIGCHLD

# Find project directories
dir_mad=`find_path_up metrics-aggregator-daemon $start_mad`
dir_cluster_agg=`find_path_up metrics-cluster-aggregator $start_cluster_agg`
dir_metrics_portal=`find_path_up metrics-portal $start_metrics_portal`
dir_kairosdb=`find_path_up kairosdb-extensions $start_kairosdb`
dir_kairosdb_datasource=`find_path_up kairosdb-datasource $start_cg`

# Verify locations for each requested project
assert_valid_path "metrics-aggregator" "${start_mad}" "${dir_mad}"
assert_valid_path "cluster-aggregator" "${start_cluster_agg}" "${dir_cluster_agg}"
assert_valid_path "metrics-portal" "${start_metrics_portal}" "${dir_metrics_portal}"
assert_valid_path "kairosdb" "${start_kairosdb}" "${dir_kairosdb}"
assert_valid_path "kairosdb-datasource" "${start_cg}" "${dir_kairosdb_datasource}"

# KairosDb should usually be run with Cassandra
if [ ${start_kairosdb} -gt 0 ] && [ ${start_cg} -eq 0 ]; then
  printf "\e[0;31mWarning\e[0m: Launching Kairosdb without Cassandra!\n"
fi

# Build verbose argument to child programs
verbose_arg=
if [ -n "$verbose" ]; then
  verbose_arg="-v"
fi

# Build projects
if [ $start_cg -gt 0 ] && [ -z "$skip_build" ]; then
  # Build KairosDb Datasource (for Grafana)
  printf "Building kairosdb-datasource...\n"
  pushd ${dir_kairosdb_datasource} &> /dev/null
  npm install
  grunt
  if [ "$?" -ne 0 ]; then printf "\e[0;31mError\e[0m: Build failed of kairosdb-datasource\n"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_kairosdb -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_kairosdb} &> /dev/null
  printf "Building kairosdb...\n"
  ./jdk-wrapper.sh ./mvnw package -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then printf "\e[0;31mError\e[0m: Build failed of kairosdb\n"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_metrics_portal -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_metrics_portal} &> /dev/null
  printf "Building metrics-portal...\n"
  ./jdk-wrapper.sh ./mvnw package -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then printf "\e[0;31mError\e[0m: Build failed of metrics-portal\n"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_mad -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_mad} &> /dev/null
  printf "Building metrics-aggregator-daemon...\n"
  ./jdk-wrapper.sh ./mvnw package -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then printf "\e[0;31mError\e[0m: Build failed of metrics-aggregator-daemon\n"; exit 1; fi
  popd &> /dev/null
fi

if [ $start_cluster_agg -gt 0 ] && [ -z "$skip_build" ]; then
  pushd ${dir_cluster_agg} &> /dev/null
  printf "Building metrics-cluster-aggregator...\n"
  ./jdk-wrapper.sh ./mvnw package -DskipAllVerification=true -DskipSources=true -DskipJavaDoc=true
  if [ "$?" -ne 0 ]; then printf "\e[0;31mError\e[0m: Build failed of cluster-aggregator\n"; exit 1; fi
  popd &> /dev/null
fi

# Run projects
if [ ${start_cg} -gt 0 ]; then
  # Cassandra
  pid_cassandra=$(docker run -d -p 7000:7000 -p 9042:9042 cassandra:${docker_cassandra_version})
  printf "Started: cassandra (${pid_cassandra:0:12})\n"
  if [ -n "${pinger}" ]; then
    "${dir}/pinger.sh" ${verbose_arg} -c "${pid_cassandra}" -n "cassandra" -p "${pid_cassandra:0:12}" &
  fi

  # Grafana
  pid_grafana=$(docker run -d -p 8081:3000 -v "${dir_kairosdb_datasource}:/var/lib/grafana/plugins/kairosdb-datasource:ro" grafana/grafana:${docker_grafana_version})
  printf "Started: grafana (${pid_grafana:0:12})\n"
  if [ -n "${pinger}" ]; then
    "${dir}/pinger.sh" ${verbose_arg} -u "http://localhost:8081/api/health" -n "grafana" -p "${pid_grafana:0:12}" &
  fi
fi

if [ ${start_kairosdb} -gt 0 ]; then
  rm -f "${target}/kairosdb.properties"
  replace_id_address "${dir}/config/kairosdb/kairosdb.properties" "${target}/kairosdb.properties"

  pushd ${dir_kairosdb} &> /dev/null
  kairosdb_version=$(getProjectVersion)
  pid_kairosdb=$(docker run -d -p 8082:8080 --restart unless-stopped \
      -v "${dir_kairosdb}/logs/docker:/opt/kairosdb/log" \
      -v "${target}/kairosdb.properties:/opt/kairosdb/conf/kairosdb.properties:ro" \
      inscopemetrics/kairosdb-extensions:${kairosdb_version})
  printf "Started: kairosdb (${pid_kairosdb:0:12})\n"
  if [ -n "${pinger}" ]; then
    "${dir}/pinger.sh" ${verbose_arg} -u "http://localhost:8082/api/v1/health/status" -n "kairosdb" -p "${pid_kairosdb:0:12}" &
  fi
  popd &> /dev/null
fi

if [ ${start_cluster_agg} -gt 0 ]; then
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
    eval "pid_cluster_agg_${i}=\"\${pid}\""
    printf "Started: cluster-aggregator ${i} of ${cagg_count} ($pid)\n"
    if [ -n "$pinger" ]; then
      ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:${cagg_http_port}/ping" -n "cagg-${i}" -p "${pid}" &
    fi
  done
  popd &> /dev/null

  # Start haproxy over cluster aggregator hosts
  pushd ${dir} &> /dev/null
  replace_id_address "${dir}/config/haproxy/haproxy.cfg" "${target}/haproxy.cfg"
  pid_haproxy=$(docker run -d -p 7066:7066 \
      -v "${target}/haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg:ro" \
      haproxy:${docker_haproxy_version})
  printf "Started: haproxy (${pid_haproxy:0:12})\n"
  if [ -n "${pinger}" ]; then
    "${dir}/pinger.sh" ${verbose_arg} -c "${pid_haproxy}" -n "haproxy" -p "${pid_haproxy:0:12}" &
  fi
  popd &> /dev/null
fi

if [ ${start_mad} -gt 0 ]; then
  pushd ${dir_mad} &> /dev/null
  if [ -n "$clear_logs" ]; then
    rm -rf ./logs
  fi
  mkdir -p "${dir_mad}/logs"
  # NOTE: To switch from mad 1.0 (branch: master) to 2.0 (branch: master-2.0) change:
  # FROM: "${dir}/config/mad/config.conf"
  # TO:   "${dir}/config/mad-2.0/config.conf"
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
  printf "Started: mad ($pid_mad)\n"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:7090/ping" -n "mad" -p "${pid_mad}" &
  fi
  popd &> /dev/null
fi

if [ ${start_metrics_portal} -gt 0 ]; then
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
  printf "Started: metrics-portal ($pid_metrics_portal)\n"
  if [ -n "$pinger" ]; then
    ${dir}/pinger.sh ${verbose_arg} -u "http://localhost:8080/ping" -n "mportal" -p "${pid_metrics_portal}" &
  fi
  popd &> /dev/null
fi

if [ ${start_telegraf} -gt 0 ]; then
  replace_id_address "${dir}/config/telegraf/telegraf.conf" "${target}/telegraf.conf"
  pid_telegraf=$(docker run -d --net=host --restart unless-stopped \
      -e HOST_PROC=/host/proc -v /proc:/host/proc:ro \
      -v "${target}/telegraf.conf:/etc/telegraf/telegraf.conf:ro" \
      telegraf:${docker_telegraf_version})
  printf "Started: telegraf (${pid_telegraf:0:12})\n"
  if [ -n "${pinger}" ]; then
    "${dir}/pinger.sh" ${verbose_arg} -c "${pid_telegraf}" -n "telegraf" -p "${pid_telegraf:0:12}" &
  fi
fi

# Upload data
if [ ${start_cg} -gt 0 ]; then
  # Wait for grafana to be healthy
  grafana_healthy=0
  while [ ${grafana_healthy} -eq 0 ]
    do
    printf "Waiting for grafana to become healthy...\n"
    sleep 2
    if curl http://localhost:8081/api/health &> /dev/null ; then
      grafana_healthy=1
    fi
  done
  printf "Grafana is healthy!\n"

  # Upload Grafana assets
  mkdir -p "${target}/grafana/data-sources"
  for file in ${dir}/data/grafana/data-sources/*.json; do
    [ -e "${file}" ] || continue
    replace_id_address "${file}" "${target}/grafana/data-sources/$(basename ${file})"
  done
  for file in ${target}/grafana/data-sources/*.json; do
    [ -e "${file}" ] || continue
    curl -X POST \
      --user admin:admin \
      -H 'content-type: application/json;charset=UTF-8' \
      --data "@${file}" \
      http://${host_ip_address}:8081/api/datasources
  done
  for file in ${dir}/data/grafana/dashboards/*.json; do
    [ -e "${file}" ] || continue
    curl -X POST \
      --user admin:admin \
      -H 'content-type: application/json' \
      --data "@${file}" \
      http://${host_ip_address}:8081/api/dashboards/db
  done
fi

popd &> /dev/null

# Wait for all running projects to exit
wait 2>/dev/null
