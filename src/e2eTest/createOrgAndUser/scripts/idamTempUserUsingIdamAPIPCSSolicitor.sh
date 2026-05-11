#!/usr/bin/env bash
# IdAM testing-support: PCS solicitor user. Pass HMCTS_ENV, SOLICITOR_EMAIL_ADDRESS, SOLICITOR_FORENAME, SOLICITOR_SURNAME from Playwright.
# Response body discarded (-o /dev/null).
set -euo pipefail

: "${HMCTS_ENV:?Set HMCTS_ENV (e.g. aat)}"
IDAM_API_BASE="https://idam-api.${HMCTS_ENV}.platform.hmcts.net"

: "${SOLICITOR_EMAIL_ADDRESS:?Set SOLICITOR_EMAIL_ADDRESS (e.g. solicitorEmailAddress from test)}"
: "${SOLICITOR_FORENAME:=solicitor}"
: "${SOLICITOR_SURNAME:=user}"
if [[ -z "${SOLICITOR_ACC_PASSWORD-}" ]]; then
  SOLICITOR_ACC_PASSWORD='Pa$$w0rd'
fi

JSON_PAYLOAD=$(cat <<EOF
{
  "email": "${SOLICITOR_EMAIL_ADDRESS}",
  "forename": "${SOLICITOR_FORENAME}",
  "surname": "${SOLICITOR_SURNAME}",
  "password": "${SOLICITOR_ACC_PASSWORD}",
  "roles": [
    { "code": "caseworker" },
    { "code": "caseworker-pcs" },
    { "code": "caseworker-pcs-solicitor" },
    { "code": "pui-case-manager" },
    { "code": "payments" },
    { "code": "payments-refund" }
  ]
}
EOF
)

curl -sS -o /dev/null --location "${IDAM_API_BASE}/testing-support/accounts" \
  --header 'accept: */*' \
  --header 'Content-Type: application/json' \
  --data-raw "${JSON_PAYLOAD}"
