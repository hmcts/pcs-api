#!/usr/bin/env bash
set +x
set -euo pipefail

: "${ORG_ROLE_MAPPING_SERVICE_URL:?Preview ORM URL is required}"
: "${ROLE_ASSIGNMENT_SERVICE_URL:?Preview RAS URL is required}"
: "${PCS_SOLICITOR_AUTOMATION_UID:?Smoke-test solicitor ID is required}"
: "${IDAM_DATA_STORE_SYSTEM_USER_USERNAME:?CCD system username is required}"
: "${IDAM_DATA_STORE_SYSTEM_USER_PASSWORD:?CCD system password is required}"
: "${CCD_API_GATEWAY_IDAM_CLIENT_SECRET:?CCD gateway client secret is required}"

export CCD_API_GATEWAY_IDAM_CLIENT_SECRET
export IDAM_API_BASE_URL=https://idam-api.aat.platform.hmcts.net

BASEDIR=$(dirname "$0")
S2S_TOKEN=$(bash "${BASEDIR}/s2s-token.sh" "am_org_role_mapping_service")
IDAM_TOKEN=$(bash "${BASEDIR}/idam-user-token.sh" "$IDAM_DATA_STORE_SYSTEM_USER_USERNAME" "$IDAM_DATA_STORE_SYSTEM_USER_PASSWORD")

function send_curl_request() {
  curl --silent --show-error --fail --connect-timeout 10 --max-time 60 \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${IDAM_TOKEN}" \
  -H "ServiceAuthorization: ${S2S_TOKEN}" "$@"
}

echo 'Loading preview professional access types'
if ! send_curl_request "${ORG_ROLE_MAPPING_SERVICE_URL}/am/testing-support/prm/findAndUpdateCaseDefinitionChanges" \
  | jq -e '.endStatus == "SUCCESS"' >/dev/null; then
    echo 'Unable to load professional access types.' >&2
    exit 1
fi

USER_ID=$(jq -rn --arg id "$PCS_SOLICITOR_AUTOMATION_UID" '$id | @uri')
echo 'Refreshing the smoke-test solicitor professional role assignments'
send_curl_request -X POST "${ORG_ROLE_MAPPING_SERVICE_URL}/am/role-mapping/professional/refresh?userId=${USER_ID}" >/dev/null

if ! send_curl_request "${ROLE_ASSIGNMENT_SERVICE_URL}/am/role-assignments/actors/${USER_ID}" \
  | jq -e '
      any(.roleAssignmentResponse[]?;
        .roleType == "ORGANISATION" and
        .roleName == "claimant-solicitor" and
        .grantType == "STANDARD" and
        .attributes.jurisdiction == "PCS" and
        .attributes.caseType == "PCS")
    ' >/dev/null; then
  echo 'PCS claimant-solicitor role not found'
  exit 1
fi

echo 'PCS claimant-solicitor verified'
