FROM dockerfile/java
EXPOSE 2551
EXPOSE 7066
ADD cluster-aggregator /opt/cluster-aggregator
ADD config /opt/cluster-aggregator/config
WORKDIR /opt/cluster-aggregator
ENV PARAMS ""
ENV CLUSTER_AGGREGATOR_OPTS -Dlogback.configurationFile=/opt/cluster-aggregator/config/logback.xml -Dconfig.file=/opt/cluster-aggregator/config/application.conf
CMD /opt/cluster-aggregator/bin/cluster-aggregator $PARAMS
