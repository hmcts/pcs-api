 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.7
FROM hmctspublic.azurecr.io/base/java:21-distroless

USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/pcs-api.jar /opt/app/

# The base image sets Xms to 128M for DEV_MODE, (Preview env), which somehow also
# sets the max heap size to the same. This change overrides it with a more suitable
# value for this service, and also enables JMX
ENV JVM_ADDITIONAL_ARGS=${DEV_MODE:+'-Xms300M \
        -Dcom.sun.management.jmxremote \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.local.only=false \
        -Dcom.sun.management.jmxremote.port=1099 \
        -Dcom.sun.management.jmxremote.rmi.port=1099 \
        -Djava.rmi.server.hostname=127.0.0.1'}

ENV JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} ${JVM_ADDITIONAL_ARGS}"

EXPOSE 3206
CMD [ "pcs-api.jar" ]
