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
EXPOSE 8080
ADD remet-gui /opt/remet-gui
ADD config /opt/remet-gui/config
WORKDIR /opt/remet-gui
ENV APPLICATION_CONF application.conf
ENV APPLICATION_PORT 8080
ENV LOGGER_XML /opt/remet-gui/config/logger.xml
CMD /opt/remet-gui/bin/remet-gui -J-server -J-Xmn150m -J-XX:+UseG1GC -J-XX:MaxGCPauseMillis=20 -Dhttp.port=$APPLICATION_PORT -Dpidfile.path=/opt/remet-gui/REMET_GUI_PID -Dconfig.trace=loads -Dconfig.resource=$APPLICATION_CONF -Dlogger.file=$LOGGER_XML
