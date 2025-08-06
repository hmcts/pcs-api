 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.3
FROM hmctspublic.azurecr.io/base/java:21-distroless

USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/pcs-api.jar /opt/app/

EXPOSE 3206
CMD [ "pcs-api.jar" ]
