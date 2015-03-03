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
EXPOSE 7090
ADD remet-proxy /opt/remet-proxy
ADD config /opt/remet-proxy/config
WORKDIR /opt/remet-proxy
ENV APPLICATION_CONF application.conf
ENV APPLICATION_PORT 7090
ENV LOGGER_XML /opt/remet-proxy/conf/logger.xml
CMD /opt/remet-proxy/bin/remet-proxy -J-server -J-Xmn150m -J-XX:+UseG1GC -J-XX:MaxGCPauseMillis=20 -Dhttp.port=$APPLICATION_PORT -Dpidfile.path=/opt/remet-proxy/REMET_PROXY_PID -Dconfig.trace=loads -Dconfig.resource=$APPLICATION_CONF -Dlogger.file=$LOGGER_XML
