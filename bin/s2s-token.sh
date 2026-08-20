#!/usr/bin/env bash

set -eu

# S2S_URL_BASE is the name the Jenkins library and the environment blocks in Jenkinsfile_CNP use,
# so each environment leases from its own service-auth-provider. S2S_HOST is kept for callers that
# already set it (bin/setup-role-assignments).
S2S_HOST=${S2S_URL_BASE:-${S2S_HOST:-http://rpe-service-auth-provider-aat.service.core-compute-aat.internal}}
MICROSERVICE=${1:-ccd_gw}

curl --fail --silent --show-error --location "${S2S_HOST}/testing-support/lease" \
--header 'Content-Type: application/json' \
--data "{ \"microservice\": \"${MICROSERVICE}\" }"
