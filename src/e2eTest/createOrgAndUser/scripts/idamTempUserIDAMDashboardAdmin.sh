#!/usr/bin/env bash
# IdAM testing-support create user. Set HMCTS_ENV from the Playwright test (e.g. aat). Default: aat.
# Response body is discarded (-o /dev/null); errors still go to stderr.
set -euo pipefail

: "${HMCTS_ENV:=aat}"
IDAM_API_BASE="https://idam-api.${HMCTS_ENV}.platform.hmcts.net"

curl -sS -o /dev/null --location "${IDAM_API_BASE}/testing-support/accounts" \
  --header 'accept: */*' \
  --header 'Content-Type: application/json' \
  --data-raw '{
 "email": "pcs-idam-adminstrator@test.com",
 "forename": "IDAM",
 "surname": "admin",
 "password": "Pa$$w0rd",
"roles": [
  {
    "code": "citizen"
  },
  {
    "code": "USER_DASHBOARD_ASSIGNABLE_ROLE"
  },
  {
    "code": "Ccd-admin"
  },
  {
    "code": "Cwd-admin"
  },
  {
    "code": "idam-user-dashboard--access"
  },
  {
    "code": "prd-admin"
  },
  {
    "code": "Systemupdate"
  }
]
}'
