#!/usr/bin/env bash
# Gives every e2e user their roles in a PR preview's own role assignment service (RAS), then checks
# nobody is missing one. Run by Jenkins before the preview smoke tests when the preview deploys its
# own RAS and ORM (labels pr-values:ccd + pr-values:wa).
#
# A preview RAS starts empty, and a helm uninstall/retry wipes it again, so this runs on every build.
# Without it the first case creation fails ("Organisation must be provided to create a case") and
# staff, judicial and specific-access tests fail on missing roles.
#
#   1. Load this PR's access types into the preview ORM (PRM P1).
#   2. Professional users: ORM refresh; if that grants nothing, seed the group-access role directly
#      from the user's PRD organisation.
#   3. Staff and judicial users: copy their ORGANISATION roles from AAT RAS (read-only).
#   4. Check each specific-access category has an approver.
#   5. Fail the build if any e2e user has no role.
#
# Reads AAT IDAM, S2S, PRD and RAS. Writes only to this preview's RAS and ORM. Safe to re-run:
# every write uses replaceExisting with a fixed reference.
set -uo pipefail

: "${CHANGE_ID:?CHANGE_ID is required}"
: "${IDAM_PCS_USER_PASSWORD:?IDAM_PCS_USER_PASSWORD is required}"
: "${PCS_FRONTEND_IDAM_SECRET:?PCS_FRONTEND_IDAM_SECRET is required}"
: "${IDAM_DATA_STORE_SYSTEM_USER_USERNAME:?IDAM_DATA_STORE_SYSTEM_USER_USERNAME is required}"
: "${IDAM_DATA_STORE_SYSTEM_USER_PASSWORD:?IDAM_DATA_STORE_SYSTEM_USER_PASSWORD is required}"
: "${CCD_API_GATEWAY_IDAM_CLIENT_SECRET:?CCD_API_GATEWAY_IDAM_CLIENT_SECRET is required}"

BASEDIR=$(cd "$(dirname "$0")" && pwd)
DATA="$BASEDIR/../src/e2eTest/data/user-data"

PREVIEW="pcs-api-pr-${CHANGE_ID}.preview.platform.hmcts.net"
RAS="${ROLE_ASSIGNMENT_SERVICE_URL:-https://am-role-assignment-service-${PREVIEW}}"
ORM="${ORG_ROLE_MAPPING_SERVICE_URL:-https://am-org-role-mapping-service-${PREVIEW}}"
AAT_RAS=http://am-role-assignment-service-aat.service.core-compute-aat.internal
PRD=http://rd-professional-api-aat.service.core-compute-aat.internal
IDAM=https://idam-api.aat.platform.hmcts.net
FRONTEND_USERS_URL=https://raw.githubusercontent.com/hmcts/pcs-frontend/master/src/test/ui/data/user-data/permanent.user.data.ts

export IDAM_API_BASE_URL="$IDAM" CCD_API_GATEWAY_IDAM_CLIENT_SECRET

S2S_ORM=$(bash "$BASEDIR/s2s-token.sh" am_org_role_mapping_service)
S2S_PRD=$(bash "$BASEDIR/s2s-token.sh" rd_professional_api)
SYSTEM_TOKEN=$(bash "$BASEDIR/idam-user-token.sh" \
  "$IDAM_DATA_STORE_SYSTEM_USER_USERNAME" "$IDAM_DATA_STORE_SYSTEM_USER_PASSWORD")
[ -n "$S2S_ORM" ] && [ -n "$S2S_PRD" ] && [ -n "$SYSTEM_TOKEN" ] \
  || { echo "Could not get S2S or IDAM tokens" >&2; exit 1; }

as_system() {
  curl --silent --show-error --connect-timeout 10 --max-time 60 \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $SYSTEM_TOKEN" -H "ServiceAuthorization: Bearer $S2S_ORM" "$@"
}

user_token() {
  curl --silent --max-time 30 -X POST "$IDAM/o/token" -d grant_type=password \
    --data-urlencode "username=$1" --data-urlencode "password=$IDAM_PCS_USER_PASSWORD" \
    -d client_id=pcs-frontend --data-urlencode "client_secret=$PCS_FRONTEND_IDAM_SECRET" \
    -d 'scope=openid profile roles' | jq -r '.access_token // empty'
}

uid_of() {
  curl --silent --max-time 30 "$IDAM/o/userinfo" -H "Authorization: Bearer $1" | jq -r '.uid // .sub // empty'
}

pcs_org_roles() {  # $1 uid -> PCS ORGANISATION role names in the preview RAS, comma separated
  as_system "$RAS/am/role-assignments/actors/$1" | jq -r '
    [.roleAssignmentResponse[]? | select(.roleType == "ORGANISATION") | .roleName] | unique | join(",")'
}

# --- 0. wait for the preview RAS and ORM ------------------------------------------------------------
wait_healthy() {
  local i
  for i in $(seq 1 30); do
    if [ "$(curl --silent --max-time 15 "$1/health" | jq -r '.components.db.status // empty' 2>/dev/null)" = "UP" ]; then
      return 0
    fi
    sleep 10
  done
  echo "$1 is not healthy with its database UP after 5 minutes. The stack is broken, not the seeding." >&2
  exit 2
}
wait_healthy "$RAS"
wait_healthy "$ORM"

# --- 1. load this PR's access types into ORM --------------------------------------------------------
echo "Loading access types into the preview ORM"
loaded=false
for i in 1 2 3 4 5; do
  # GET: this testing-support endpoint rejects POST
  p1=$(as_system "$ORM/am/testing-support/prm/findAndUpdateCaseDefinitionChanges" 2>&1)
  if jq -e '.endStatus == "SUCCESS"' >/dev/null 2>&1 <<<"$p1"; then
    loaded=true; break
  fi
  echo "Attempt $i: ${p1:0:300}"
  sleep 30
done
[ "$loaded" = true ] || { echo "ORM could not load access types from the preview definition store" >&2; exit 1; }

# --- 2. professional users ---------------------------------------------------------------------------
FRONTEND_USERS=$(curl --silent --max-time 30 "$FRONTEND_USERS_URL" || true)
[ -n "$FRONTEND_USERS" ] || echo "WARNING: could not fetch pcs-frontend's user data, its extra users are not seeded"

# Logged in as by the smoke and API tests but not listed in the user data files.
EXTRA_PROFESSIONAL_EMAILS="pcs-solicitor-automation@test.com ${IDAM_PCS_USER_EMAIL:-}"

professional_emails() {
  { cat "$DATA"/*.ts; printf '%s\n' "$FRONTEND_USERS" $EXTRA_PROFESSIONAL_EMAILS; } \
    | grep -v '^\s*//' | grep -ohE "[A-Za-z0-9._%+-]+@test\.com" | tr 'A-Z' 'a-z' | sort -u
}

# Seeds group-access roles from the user's PRD organisation, as ORM would.
# SOLICITOR_PROFILE orgs get claimant-solicitor and defendant-solicitor; every other org gets claimant.
seed_from_prd() {  # $1 uid  $2 user token
  local org profiles body
  org=$(curl --silent --max-time 30 "$PRD/refdata/external/v1/organisations" \
          -H "Authorization: Bearer $2" -H "ServiceAuthorization: Bearer $S2S_PRD")
  local org_id
  org_id=$(jq -r '.organisationIdentifier // empty' <<<"$org" 2>/dev/null)
  [ -n "$org_id" ] || return 2   # not in a PRD organisation, e.g. a caseworker
  profiles=$(jq -r '(.organisationProfileIds // []) | join(",")' <<<"$org")
  body=$(jq -n --arg uid "$1" --arg org "$org_id" --arg profiles "$profiles" '
    def role($name; $access): {
      actorIdType: "IDAM", actorId: $uid, roleType: "ORGANISATION", roleName: $name,
      roleCategory: "PROFESSIONAL", classification: "RESTRICTED", grantType: "STANDARD", readOnly: false,
      attributes: {jurisdiction: "PCS", caseType: "PCS", substantive: "Y",
                   caseAccessGroupId: ("PCS:PCS:" + $access + ":" + $name + ":" + $org)}};
    {
      roleRequest: {assignerId: $uid, process: "professional-organisational-role-mapping",
                    reference: ($uid + "-preview-seed"), replaceExisting: true},
      requestedRoles: (if ($profiles | split(",") | index("SOLICITOR_PROFILE"))
        then [role("claimant-solicitor"; "solicitor-org-claimant-access"),
              role("defendant-solicitor"; "solicitor-org-defendant-access")]
        else [role("claimant"; "prof-org-claimant-access")] end)
    }')
  as_system -X POST "$RAS/am/role-assignments" -d "$body" \
    | jq -e '.roleAssignmentResponse.roleRequest.status == "APPROVED"' >/dev/null 2>&1
}

RESULTS=""   # lines of: email|uid|roles|note
record() { RESULTS+="$1|$2|$3|$4"$'\n'; }

refresh_professional() {  # $1 label  $2 uid  [$3 user token]
  local roles note=orm
  as_system -X POST "$ORM/am/role-mapping/professional/refresh?userId=$2" >/dev/null 2>&1
  roles=$(pcs_org_roles "$2")
  if [ -z "$roles" ] && [ -n "${3:-}" ]; then
    seed_from_prd "$2" "$3"
    case $? in
      0) note=prd ;;
      2) note="no organisation" ;;
      *) note="prd seed failed" ;;
    esac
    roles=$(pcs_org_roles "$2")
  fi
  record "$1" "$2" "$roles" "$note"
}

echo "Seeding professional users"
while read -r email; do
  [ -z "$email" ] && continue
  token=$(user_token "$email")
  [ -n "$token" ] || { record "$email" "-" "" "no IDAM login"; continue; }
  uid=$(uid_of "$token")
  refresh_professional "$email" "$uid" "$token"
done < <(professional_emails)

# Accounts the smoke tests use by id only. ORM refresh is their only route.
[ -n "${PCS_SOLICITOR_AUTOMATION_UID:-}" ] && refresh_professional "solicitor-automation (uid)" "$PCS_SOLICITOR_AUTOMATION_UID"

# --- 3. staff and judicial users: copy from AAT -----------------------------------------------------
staff_emails() {
  cat "$DATA"/*.ts | grep -v '^\s*//' \
    | grep -ohE "[A-Za-z0-9._%+-]+@(justice\.gov\.uk|hmcts\.net|ejudiciary\.net|judicialofficeelinkssw\.onmicrosoft\.com)" \
    | tr 'A-Z' 'a-z' | sort -u
}

echo "Copying staff and judicial roles from AAT"
while read -r email; do
  [ -z "$email" ] && continue
  token=$(user_token "$email")
  [ -n "$token" ] || { record "$email" "-" "" "no IDAM login"; continue; }
  uid=$(uid_of "$token")
  body=$(as_system "$AAT_RAS/am/role-assignments/actors/$uid" | jq --arg uid "$uid" '{
    roleRequest: {assignerId: $uid, process: "preview-seed", reference: ("preview-seed-" + $uid),
                  replaceExisting: true},
    requestedRoles: [.roleAssignmentResponse[]? | select(.roleType == "ORGANISATION")
        | select((.attributes.jurisdiction // "PCS") == "PCS") | {
      actorIdType, actorId, roleType, roleName, classification, grantType, roleCategory, readOnly,
      attributes: (.attributes // {}), authorisations: (.authorisations // [])}]}')
  if [ "$(jq '.requestedRoles | length' <<<"$body")" -eq 0 ]; then
    record "$email" "$uid" "$(pcs_org_roles "$uid")" "no roles in AAT"
    continue
  fi
  status=$(as_system -o /dev/null -w '%{http_code}' -X POST "$RAS/am/role-assignments" -d "$body")
  record "$email" "$uid" "$(pcs_org_roles "$uid")" "aat copy $status"
done < <(staff_emails)

# --- 4 and 5. check -----------------------------------------------------------------------------------
echo
printf '%-58s %-22s %s\n' "USER" "SOURCE" "ROLES IN PREVIEW RAS"
missing=0; unresolved=0
while IFS='|' read -r email uid roles note; do
  [ -z "$email" ] && continue
  printf '%-58s %-22s %s\n' "$email" "$note" "${roles:-NONE}"
  if [ "$note" = "no IDAM login" ] || [ "$note" = "no organisation" ]; then unresolved=$((unresolved + 1))
  elif [ -z "$roles" ]; then missing=$((missing + 1)); fi
done <<<"$RESULTS"

has_role() { grep -E "^$1\|" <<<"$RESULTS" | cut -d'|' -f3 | tr ',' '\n' | grep -qx "$2"; }

problems=()
[ "$missing" -eq 0 ] || problems+=("$missing user(s) have no role")
has_role "pcs\.local\.auth1user1@test\.com" claimant \
  || problems+=("pcs.local.auth1user1@test.com has no claimant role (frontend smoke creates cases as this user)")
[ -z "${PCS_SOLICITOR_AUTOMATION_UID:-}" ] || has_role "solicitor-automation \(uid\)" claimant-solicitor \
  || problems+=("the solicitor automation user has no claimant-solicitor role")
for category in judiciary legal-ops admin ctsc; do
  cut -d'|' -f3 <<<"$RESULTS" | tr ',' '\n' | grep -qx "specific-access-approver-$category" \
    || problems+=("no user holds specific-access-approver-$category")
done

echo
echo "users=$(grep -c . <<<"$RESULTS") missing=$missing unchecked=$unresolved (accounts that can't log in or have no organisation)"
if [ "${#problems[@]}" -gt 0 ]; then
  printf 'FAILED: %s\n' "${problems[@]}" >&2
  exit 1
fi
echo "Every e2e user has a role in the preview RAS."
