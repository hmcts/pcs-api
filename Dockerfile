 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.7
FROM hmctspublic.azurecr.io/base/java:21-distroless

USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/pcs-api.jar /opt/app/

ENV JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dscott.test=2 -Dcom.sun.management.jmxremote \
                       -Dcom.sun.management.jmxremote.authenticate=false \
                       -Dcom.sun.management.jmxremote.ssl=false \
                       -Dcom.sun.management.jmxremote.local.only=false \
                       -Dcom.sun.management.jmxremote.port=1099 \
                       -Dcom.sun.management.jmxremote.rmi.port=1099 \
                       -Xms300M -XX:MaxRAMPercentage=80.0 \
                       -Djava.rmi.server.hostname=127.0.0.1"


EXPOSE 3206 1099
CMD [ "pcs-api.jar" ]
