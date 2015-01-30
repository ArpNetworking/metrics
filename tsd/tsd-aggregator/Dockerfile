FROM dockerfile/java
EXPOSE 6080
ADD tsd-aggregator /opt/tsd-aggregator
ADD config /opt/tsd-aggregator/config
WORKDIR /opt/tsd-aggregator
ENV PARAMS --config file:/opt/tsd-aggregator/config/config.json
ENV TSD_AGGREGATOR_OPTS -Dlogback.configurationFile=/opt/tsd-aggregator/config/logback.xml
CMD /opt/tsd-aggregator/bin/tsd-aggregator $PARAMS
