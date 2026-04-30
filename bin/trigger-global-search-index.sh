#!/usr/bin/env bash
set -ex

S2S_TOKEN=$(curl -s -X POST "${IDAM_S2S_AUTH_URL}/testing-support/lease" \
  -H 'Content-Type: application/json' \
  -d '{"microservice": "pcs_api"}')

IDAM_TOKEN=$(curl -s -X POST "${IDAM_API_URL}/o/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode "username=${IDAM_SYSTEM_USERNAME}" \
  --data-urlencode "password=${IDAM_SYSTEM_USER_PASSWORD}" \
  --data-urlencode 'client_id=pcs-api' \
  --data-urlencode "client_secret=${PCS_API_IDAM_SECRET}" \
  --data-urlencode 'scope=openid profile roles' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${DEFINITION_STORE_URL_BASE}/elastic-support/global-search/index" \
  -H "Authorization: Bearer ${IDAM_TOKEN}" \
  -H "ServiceAuthorization: Bearer ${S2S_TOKEN}")

if [ "$STATUS" != "201" ]; then
  echo "ERROR: Global search index returned status ${STATUS}"
  exit 1
fi

echo "Global search index created successfully"
