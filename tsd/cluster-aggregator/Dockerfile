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

FROM dockerfile/java
EXPOSE 2551
EXPOSE 7066
ADD cluster-aggregator /opt/cluster-aggregator
ADD config /opt/cluster-aggregator/config
WORKDIR /opt/cluster-aggregator
ENV PARAMS ""
ENV CLUSTER_AGGREGATOR_OPTS -Dlogback.configurationFile=/opt/cluster-aggregator/config/logback.xml -Dconfig.file=/opt/cluster-aggregator/config/application.conf
CMD /opt/cluster-aggregator/bin/cluster-aggregator $PARAMS
