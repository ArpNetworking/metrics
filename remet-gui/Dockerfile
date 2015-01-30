FROM dockerfile/java
EXPOSE 8080
ADD remet-gui /opt/remet-gui
ADD config /opt/remet-gui/config
WORKDIR /opt/remet-gui
ENV APPLICATION_CONF application.conf
ENV APPLICATION_PORT 8080
ENV LOGGER_XML /opt/remet-gui/config/logger.xml
CMD /opt/remet-gui/bin/remet-gui -J-server -J-Xmn150m -J-XX:+UseG1GC -J-XX:MaxGCPauseMillis=20 -Dhttp.port=$APPLICATION_PORT -Dpidfile.path=/opt/remet-gui/REMET_GUI_PID -Dconfig.trace=loads -Dconfig.resource=$APPLICATION_CONF -Dlogger.file=$LOGGER_XML
