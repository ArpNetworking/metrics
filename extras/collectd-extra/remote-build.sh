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


function safe_command {
  local l_command=$1
  local l_prefix=`date  +'%H:%M:%S'`
  echo "[$l_prefix] executing: $l_command";
  eval $1
  local l_result=$?
  if [ "$l_result" -ne "0" ]; then
    echo "ERROR: $l_command failed with $l_result"
    exit $l_result;
  fi
}

pwd=`pwd`
collectd_archive=$1
collectd_dir=$2
plugins=$3
collectd_install=$pwd/collectd-dotci/$collectd_dir/install

enable_plugins=""
for plugin in $plugins; do
  enable_plugins="$enable_plugins --enable-$plugin=yes"
done

safe_command "rm -rf collectd-dotci"
safe_command "tar -xzf collectd-dotci.tar.gz"
safe_command "pushd collectd-dotci"

safe_command "tar -xzf $collectd_archive"

safe_command "patch $collectd_dir/version-gen.sh < version-gen.sh.patch"

safe_command "pushd $collectd_dir"
safe_command "LDFLAGS=-Wl,-R/usr/local/lib ./configure --with-libgcrypt=no --prefix=$collectd_install --enable-notify-desktop=no --enable-all-plugins=no $enable_plugins"
safe_command "popd"

safe_command "cp tsd.c $collectd_dir/src/"

safe_command "patch $collectd_dir/configure.ac < configure.ac.patch"
safe_command "patch $collectd_dir/src/Makefile.am < src.Makefile.am.patch"
safe_command "patch $collectd_dir/src/plugin.c < src.plugin.c.patch"

safe_command "pushd $collectd_dir"
safe_command "/usr/local/bin/make all install"
safe_command "tar -czf install.tgz install"
safe_command "popd"

safe_command "mv $collectd_dir/install.tgz ./"
safe_command "popd"

for plugin in $plugins; do
  plugin_so="$collectd_install/lib/collectd/$plugin.so"
  plugin_l="$collectd_install/lib/collectd/$plugin.a"
  plugin_la="$collectd_install/lib/collectd/$plugin.la"
  if [ ! -f $plugin_so ]; then
    echo "Missing: $plugin_so"
    echo "ERROR: Required plugin not found: $plugin"
    exit 1
  fi
  if [ ! -f $plugin_l ]; then
    echo "Missing: $plugin_l"
    echo "ERROR: Required plugin not found: $plugin"
    exit 1
  fi
  if [ ! -f $plugin_la ]; then
    echo "Missing: $plugin_la"
    echo "ERROR: Required plugin not found: $plugin"
    exit 1
  fi
  echo "Found required plugin: $plugin"
done

exit 0
