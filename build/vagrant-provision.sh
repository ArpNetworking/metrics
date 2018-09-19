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

# Install epel and dnf
yum -y install epel-release 2>&1
yum -y install git java vim wget dnf jq net-tools lsof nmap-ncat

# Install HAProxy
yum -y install haproxy
setsebool -P haproxy_connect_any=1

# Install Cassandra
echo '[cassandra]' > /etc/yum.repos.d/cassandra.repo
echo 'name=Apache Cassandra' >> /etc/yum.repos.d/cassandra.repo
echo 'baseurl=https://www.apache.org/dist/cassandra/redhat/311x/' >> /etc/yum.repos.d/cassandra.repo
echo 'gpgcheck=0' >> /etc/yum.repos.d/cassandra.repo
echo 'repo_gpgcheck=0' >> /etc/yum.repos.d/cassandra.repo
yum -y install cassandra
LOCAL_IP_ADDRESS=$(ip route get 1 | awk '{print $NF;exit}')
sed -i "s/start_rpc: false/start_rpc: true/" /etc/cassandra/conf/cassandra.yaml
sed -i "s/listen_address: localhost/listen_address: \"${LOCAL_IP_ADDRESS}\"/" /etc/cassandra/conf/cassandra.yaml
sed -i "s/rpc_address: localhost/rpc_address: \"${LOCAL_IP_ADDRESS}\"/" /etc/cassandra/conf/cassandra.yaml
sed -i "s/# broadcast_address: 1.2.3.4/broadcast_address: \"${LOCAL_IP_ADDRESS}\"/" /etc/cassandra/conf/cassandra.yaml
sed -i "s/# broadcast_rpc_address: 1.2.3.4/broadcast_rpc_address: \"${LOCAL_IP_ADDRESS}\"/" /etc/cassandra/conf/cassandra.yaml
sed -i "s/          - seeds: \"127.0.0.1\"/          - seeds: \"${LOCAL_IP_ADDRESS}\"/" /etc/cassandra/conf/cassandra.yaml

# Install Scylla
# Note: This is not compatible with Akka Persistence
# See: https://github.com/akka/akka-persistence-cassandra/issues/366
#wget -O /etc/yum.repos.d/scylla.repo http://downloads.scylladb.com/rpm/centos/scylla-1.7.repo 2>&1
#yum -y remove abrt; true
#yum -y install scylla
#sed -i 's/# start_rpc: true/start_rpc: true/' /etc/scylla/scylla.yaml
#sed -i 's/listen_address: localhost/listen_address: "0.0.0.0"/' /etc/scylla/scylla.yaml
#sed -i 's/rpc_address: localhost/rpc_address: "0.0.0.0"/' /etc/scylla/scylla.yaml
#sed -i 's/# broadcast_address: 1.2.3.4/broadcast_address: "127.0.0.1"/' /etc/scylla/scylla.yaml
#sed -i 's/# broadcast_rpc_address: 1.2.3.4/broadcast_rpc_address: "127.0.0.1"/' /etc/scylla/scylla.yaml
#sed -i 's/^SCYLLA_ARGS=\"/SCYLLA_ARGS=\"--developer-mode 1 /' /etc/sysconfig/scylla-server

# Install KairosDb
yum -y install https://github.com/kairosdb/kairosdb/releases/download/v1.2.1/kairosdb-1.2.1-1.rpm
service kairosdb stop
sed -i 's/^\\(kairosdb.service.datastore=org.kairosdb.datastore.h2.H2Module\\)$/#\\1/' /opt/kairosdb/conf/kairosdb.properties
sed -i 's/^#\\(kairosdb.service.datastore=org.kairosdb.datastore.cassandra.CassandraModule\\)$/\\1/' /opt/kairosdb/conf/kairosdb.properties

# Install Grafana
yum -y install https://s3-us-west-2.amazonaws.com/grafana-releases/release/grafana-5.1.4-1.x86_64.rpm

# Install Kairos Grafana plugin
yum -y install npm
mkdir /var/lib/grafana/plugins
pushd /var/lib/grafana/plugins
git clone https://github.com/BrandonArp/kairosdb-datasource.git
pushd kairosdb-datasource
npm install -g grunt
npm install
grunt
popd
popd
chown -R grafana:grafana /var/lib/grafana/plugins

# Install Kairos Histogram
yum install -y https://github.com/ArpNetworking/kairosdb-histograms/releases/download/kairosdb-histograms-2.0.0/kairosdb-histograms-2.0.0-1.noarch.rpm
yum install -y kairosdb-histograms*.rpm

# Install Telegraf
cat <<EOF | tee /etc/yum.repos.d/influxdb.repo
[influxdb]
name = InfluxDB Repository - RHEL \$releasever
baseurl = https://repos.influxdata.com/centos/\$releasever/\$basearch/stable
enabled = 1
gpgcheck = 1
gpgkey = https://repos.influxdata.com/influxdb.key
EOF
yum -y install telegraf

# Enable services
/usr/bin/systemctl enable haproxy
#/usr/bin/systemctl enable scylla-server
/usr/bin/systemctl enable cassandra
/usr/bin/systemctl enable kairosdb
/usr/bin/systemctl enable grafana-server
/usr/bin/systemctl enable telegraf

exit 0
