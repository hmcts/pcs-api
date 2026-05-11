#!/usr/bin/env bash
# IdAM testing-support: Citizen user via /test/idam/users.
# Required envs: HMCTS_ENV, IDAM_ACCESS_TOKEN, CITIZEN_EMAIL_ADDRESS
# Optional envs: CITIZEN_FORENAME, CITIZEN_SURNAME, CITIZEN_ACC_PASSWORD
# Response body discarded (-o /dev/null).
set -euo pipefail

: "${HMCTS_ENV:?Set HMCTS_ENV (e.g. aat)}"
IDAM_USERS_URL="https://idam-testing-support-api.${HMCTS_ENV}.platform.hmcts.net/test/idam/users"

: "${IDAM_ACCESS_TOKEN:?Set IDAM_ACCESS_TOKEN (Bearer token for idam-testing-support-api)}"
: "${CITIZEN_EMAIL_ADDRESS:?Set CITIZEN_EMAIL_ADDRESS}"
: "${CITIZEN_FORENAME:=Citizen}"
: "${CITIZEN_SURNAME:=User}"
if [[ -z "${CITIZEN_ACC_PASSWORD-}" ]]; then
  CITIZEN_ACC_PASSWORD='Pa$$w0rd'
fi

JSON_PAYLOAD=$(cat <<EOF
{
  "password": "${CITIZEN_ACC_PASSWORD}",
  "user": {
    "email": "${CITIZEN_EMAIL_ADDRESS}",
    "forename": "${CITIZEN_FORENAME}",
    "surname": "${CITIZEN_SURNAME}",
    "roleNames": [
      "citizen"
    ]
  }
}
EOF
)

curl -sS -o /dev/null --location "${IDAM_USERS_URL}" \
  --header "Authorization: Bearer ${IDAM_ACCESS_TOKEN}" \
  --header 'Content-Type: application/json' \
  --data-raw "${JSON_PAYLOAD}"
