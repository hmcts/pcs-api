#!/usr/bin/env bash
# Trigger PRM refresh for a professional user on a PCS PR preview, then show RAS result.
# Requires HMCTS VPN (the S2S lease URL is internal). jq required.
#
#   export IDAM_CLIENT_SECRET=<pcs-frontend client secret>   # from the Postman env or pcs-aat vault
#   ./refresh.sh <userId> [pr-number]
#   ./refresh.sh 3b18bab9-33a3-420f-b37e-b63e73862828 2179     # johnwoo on pr-2179
set -euo pipefail

USER_ID="${1:?usage: refresh.sh <userId> [pr-number]}"
PR="${2:-2179}"
CLIENT_SECRET="${IDAM_CLIENT_SECRET:?export IDAM_CLIENT_SECRET (pcs-frontend) first}"
IDAM_USER="${IDAM_USER:-johnwoo@johnwoo.com}"
IDAM_PASS="${IDAM_PASS:-Testing12345}"

IDAM=https://idam-api.aat.platform.hmcts.net
S2S=http://rpe-service-auth-provider-aat.service.core-compute-aat.internal
ORM=https://am-org-role-mapping-service-pcs-api-pr-${PR}.preview.platform.hmcts.net
RAS=https://am-role-assignment-service-pcs-api-pr-${PR}.preview.platform.hmcts.net

echo "== auth =="
TOKEN=$(curl -s -X POST "$IDAM/o/token" \
  -d grant_type=password -d "username=$IDAM_USER" -d "password=$IDAM_PASS" \
  -d client_id=pcs-frontend -d "client_secret=$CLIENT_SECRET" -d 'scope=openid profile roles' \
  | jq -r .access_token)
[ -n "$TOKEN" ] && [ "$TOKEN" != null ] || { echo "IDAM login failed"; exit 1; }
S2S_ORM=$(curl -s -X POST "$S2S/testing-support/lease" -H 'Content-Type: application/json' -d '{"microservice":"am_org_role_mapping_service"}')
S2S_CCD=$(curl -s -X POST "$S2S/testing-support/lease" -H 'Content-Type: application/json' -d '{"microservice":"ccd_data"}')
echo "  ok (idam ${#TOKEN}, s2sOrm ${#S2S_ORM}, s2sCcd ${#S2S_CCD})"

echo "== PRM refresh $USER_ID =="
curl -s -o /dev/null -w '  -> HTTP %{http_code}\n' -X POST \
  "$ORM/am/role-mapping/professional/refresh?userId=$USER_ID" \
  -H "Authorization: Bearer $TOKEN" -H "ServiceAuthorization: Bearer $S2S_ORM"

sleep 5
echo "== RAS assignments for $USER_ID =="
curl -s "$RAS/am/role-assignments/actors/$USER_ID" \
  -H "Authorization: Bearer $TOKEN" -H "ServiceAuthorization: Bearer $S2S_CCD" | jq .
# 422 on refresh + empty RAS  = catalogue blocker (AM). Row present = AM added the role -> works.
