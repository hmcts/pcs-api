# S2S_URL_BASE is set per environment in Jenkinsfile_CNP; S2S_HOST is kept for existing callers.
S2S_HOST=${S2S_URL_BASE:-${S2S_HOST:-http://rpe-service-auth-provider-aat.service.core-compute-aat.internal}}
MICROSERVICE=${1:-ccd_gw}

curl --silent --location "${S2S_HOST}/testing-support/lease" \
--header 'Content-Type: application/json' \
--data "{ \"microservice\": \"${MICROSERVICE}\" }"
