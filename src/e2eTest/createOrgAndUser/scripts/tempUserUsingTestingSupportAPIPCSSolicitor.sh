#!/usr/bin/env bash
# IdAM testing-support: PCS solicitor user via /test/idam/users.
# Required envs: HMCTS_ENV, SOLICITOR_EMAIL_ADDRESS, IDAM_ACCESS_TOKEN
# Optional envs: SOLICITOR_FORENAME, SOLICITOR_SURNAME, SOLICITOR_ACC_PASSWORD
# Response body discarded (-o /dev/null).
set -euo pipefail

: "${HMCTS_ENV:?Set HMCTS_ENV (e.g. aat)}"
IDAM_USERS_URL="https://idam-testing-support-api.${HMCTS_ENV}.platform.hmcts.net/test/idam/users"

: "${SOLICITOR_EMAIL_ADDRESS:?Set SOLICITOR_EMAIL_ADDRESS (e.g. solicitorEmailAddress from test)}"
: "${IDAM_ACCESS_TOKEN:?Set IDAM_ACCESS_TOKEN (Bearer token for idam-testing-support-api)}"
: "${SOLICITOR_FORENAME:=solicitor}"
: "${SOLICITOR_SURNAME:=user}"
if [[ -z "${SOLICITOR_ACC_PASSWORD-}" ]]; then
  SOLICITOR_ACC_PASSWORD='Pa$$w0rd'
fi

JSON_PAYLOAD=$(cat <<EOF
{
  "password": "${SOLICITOR_ACC_PASSWORD}",
  "user": {
    "email": "${SOLICITOR_EMAIL_ADDRESS}",
    "forename": "${SOLICITOR_FORENAME}",
    "surname": "${SOLICITOR_SURNAME}",
    "roleNames": [
      "caseworker",
      "caseworker-pcs",
      "caseworker-pcs-solicitor",
      "pui-case-manager",
      "payments",
      "payments-refund"
    ]
  }
}
EOF
)

curl -sS -o /dev/null --location "${IDAM_USERS_URL}" \
  --header "Authorization: Bearer ${IDAM_ACCESS_TOKEN}" \
  --header 'Content-Type: application/json' \
  --data-raw "${JSON_PAYLOAD}"
