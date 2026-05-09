#!/usr/bin/env bash
# Latest IdAM test notification for the given email (testing-support API).
# Set from Playwright: HMCTS_ENV, ORG_EMAIL_ADDRESS, IDAM_ACCESS_TOKEN (Bearer = access_token from idamToken.sh).
# Response body discarded. To automate the activation link + set password, use
# waitForLatestIdamNotificationLink + completeIdamPasswordActivation in ../idam-testing-support-notification.ts
set -euo pipefail

: "${HMCTS_ENV:?Set HMCTS_ENV (e.g. aat)}"
IDAM_API_BASE="https://idam-testing-support-api.${HMCTS_ENV}.platform.hmcts.net"

: "${ORG_EMAIL_ADDRESS:?Set ORG_EMAIL_ADDRESS}"
: "${IDAM_ACCESS_TOKEN:?Set IDAM_ACCESS_TOKEN}"

EMAIL_ENC=$(node -p "encodeURIComponent(process.env.ORG_EMAIL_ADDRESS || '')")

curl -sS -o /dev/null --globoff \
  --location "${IDAM_API_BASE}/test/idam/notifications/latest/${EMAIL_ENC}" \
  --header 'accept: */*' \
  --header 'Content-Type: application/json' \
  --header "Authorization: Bearer ${IDAM_ACCESS_TOKEN}"
