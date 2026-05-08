#!/usr/bin/env bash
# IdAM password grant — JSON (access_token) on stdout for Playwright.
#
# Required: IDAM_TOKEN_USERNAME, IDAM_TOKEN_PASSWORD, IDAM_TOKEN_CLIENT_SECRET
# Optional: HMCTS_ENV (default aat), IDAM_TOKEN_CLIENT_ID, IDAM_TOKEN_SCOPE,
#           IDAM_TOKEN_GRANT_TYPE, IDAM_TOKEN_REDIRECT_URI (default uses HMCTS_ENV for civil-citizen-ui)
set -euo pipefail

: "${HMCTS_ENV:=aat}"

IDAM_TOKEN_URL="${IDAM_TOKEN_URL:-https://idam-api.${HMCTS_ENV}.platform.hmcts.net/o/token}"
: "${IDAM_TOKEN_CLIENT_ID:=pcs-api}"
: "${IDAM_TOKEN_SCOPE:=profile openid roles}"
: "${IDAM_TOKEN_GRANT_TYPE:=password}"
: "${IDAM_TOKEN_REDIRECT_URI:=https://civil-citizen-ui.${HMCTS_ENV}.platform.hmcts.net/oauth2/callback}"

: "${IDAM_TOKEN_USERNAME:?Set IDAM_TOKEN_USERNAME (e.g. in CI or export locally)}"
: "${IDAM_TOKEN_PASSWORD:?Set IDAM_TOKEN_PASSWORD}"
: "${IDAM_TOKEN_CLIENT_SECRET:?Set IDAM_TOKEN_CLIENT_SECRET}"

curl -sS --location "${IDAM_TOKEN_URL}" \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "password=${IDAM_TOKEN_PASSWORD}" \
  --data-urlencode "username=${IDAM_TOKEN_USERNAME}" \
  --data-urlencode "client_secret=${IDAM_TOKEN_CLIENT_SECRET}" \
  --data-urlencode "client_id=${IDAM_TOKEN_CLIENT_ID}" \
  --data-urlencode "redirect_uri=${IDAM_TOKEN_REDIRECT_URI}" \
  --data-urlencode "scope=${IDAM_TOKEN_SCOPE}" \
  --data-urlencode "grant_type=${IDAM_TOKEN_GRANT_TYPE}"
