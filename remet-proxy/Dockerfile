FROM dockerfile/java
EXPOSE 7090
ADD remet-proxy /opt/remet-proxy
ADD config /opt/remet-proxy/config
WORKDIR /opt/remet-proxy
ENV APPLICATION_CONF application.conf
ENV APPLICATION_PORT 7090
ENV LOGGER_XML /opt/remet-proxy/conf/logger.xml
CMD /opt/remet-proxy/bin/remet-proxy -J-server -J-Xmn150m -J-XX:+UseG1GC -J-XX:MaxGCPauseMillis=20 -Dhttp.port=$APPLICATION_PORT -Dpidfile.path=/opt/remet-proxy/REMET_PROXY_PID -Dconfig.trace=loads -Dconfig.resource=$APPLICATION_CONF -Dlogger.file=$LOGGER_XML
