#!/usr/bin/env bash
set -euo pipefail

: "${HMCTS_ENV:?Set HMCTS_ENV (e.g. aat)}"
s2s_BASE="http://rpe-service-auth-provider-${HMCTS_ENV.service.core-compute-${HMCTS_ENV.internal/testing-support/lease"

curl --location "${s2s_BASE}" \
--header 'Content-Type: application/json' \
--data '{
    "microservice": "pcs_api"
}'
