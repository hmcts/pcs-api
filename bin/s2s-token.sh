# S2S_URL_BASE is set per environment in Jenkinsfile_CNP; S2S_HOST is kept for existing callers.
# IDAM_S2S_AUTH_URL is set for every Jenkins build. When running locally, export one of these first.
S2S_HOST=${S2S_URL_BASE:-${S2S_HOST:-${IDAM_S2S_AUTH_URL:?Set S2S_URL_BASE, S2S_HOST or IDAM_S2S_AUTH_URL to the S2S service URL}}}
MICROSERVICE=${1:-ccd_gw}

curl --silent --location "${S2S_HOST}/testing-support/lease" \
--header 'Content-Type: application/json' \
--data "{ \"microservice\": \"${MICROSERVICE}\" }"
