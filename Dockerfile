 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.7
FROM hmctspublic.azurecr.io/base/java:pr-21-distroless-debug

USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/pcs-api.jar /opt/app/

EXPOSE 3206
CMD [ "pcs-api.jar" ]
