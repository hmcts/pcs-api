#!/usr/bin/env bash
# Onboards, disables and checks bulk claims API clients held in the PCS Key Vault.
#
# The APIM policy in infrastructure/apim-policy/api-policy.xml authenticates HTTP Basic
# credentials against the secret bulk-claims-client-<username>, whose value is
#   {"organisationId":"ABC1234","salt":"<base64>","passwordHash":"<base64>","enabled":true}
# where passwordHash = base64(HMAC-SHA256(key = salt bytes, message = UTF-8 password)).
#
# Usage:
#   bulk-claims-apim-client.sh <env> <username> <organisation-id> [--vault NAME] [--subscription ID]
#   bulk-claims-apim-client.sh <env> <username> --disable [--vault NAME] [--subscription ID]
#   bulk-claims-apim-client.sh <env> <username> --check [--vault NAME] [--subscription ID]
#   bulk-claims-apim-client.sh --self-test
#
# The first form creates the client, or rotates its password, and prints the new password
# once on stdout. --check reads the password from BULK_CLAIMS_PASSWORD or prompts for it.
# OPENSSL may point at an OpenSSL 1.1+ binary where the system one lacks HMAC support.
set -euo pipefail

OPENSSL="${OPENSSL:-openssl}"
USERNAME_PATTERN='^[a-z0-9-]{3,40}$'
ORGANISATION_PATTERN='^[A-Za-z0-9]{3,32}$'

TEST_SALT='ABEiM0RVZneImaq7zN3u/wARIjNEVWZ3iJmqu8zd7v8='
TEST_PASSWORD='correct-horse-battery-staple-9'
TEST_HASH='Q5TxWcix3X6mFn6tqiIvEumZout15D21xyhWBIeUU2o='

die() {
  echo "error: $*" >&2
  exit 1
}

usage() {
  sed -n '2,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//' >&2
  exit 2
}

hash_password() {
  local salt_b64="$1" password="$2" salt_hex
  salt_hex="$(printf '%s' "$salt_b64" | "$OPENSSL" base64 -d -A | od -v -An -tx1 | tr -d ' \n')"
  printf '%s' "$password" \
    | "$OPENSSL" dgst -sha256 -mac HMAC -macopt "hexkey:${salt_hex}" -binary \
    | "$OPENSSL" base64 -A
}

constant_time_equals() {
  local a="$1" b="$2" i diff=0
  [ "${#a}" -eq "${#b}" ] || return 1
  for ((i = 0; i < ${#a}; i++)); do
    diff=$((diff | $(printf '%d' "'${a:i:1}") ^ $(printf '%d' "'${b:i:1}")))
  done
  [ "$diff" -eq 0 ]
}

self_test() {
  local actual
  actual="$(hash_password "$TEST_SALT" "$TEST_PASSWORD" 2>/dev/null)" \
    || die "$OPENSSL cannot compute HMAC-SHA256 with -mac HMAC; set OPENSSL to an OpenSSL 1.1+ binary"
  [ "$actual" = "$TEST_HASH" ] || die "hash mismatch: expected $TEST_HASH, got $actual"
  constant_time_equals "$actual" "$TEST_HASH" || die "constant_time_equals rejected equal values"
  if constant_time_equals "$actual" "${TEST_HASH%=}A"; then
    die "constant_time_equals accepted different values"
  fi
  echo "self-test passed"
}

json_field() {
  printf '%s' "$1" | sed -n "s/.*\"$2\":\"\\([^\"]*\\)\".*/\\1/p"
}

require_az() {
  command -v az >/dev/null 2>&1 || die "az CLI not found"
  az account show >/dev/null 2>&1 || die "not logged in; run az login"
}

az_args=()
vault=""
subscription=""

parse_options() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --vault) vault="${2:-}"; shift 2 ;;
      --subscription) subscription="${2:-}"; shift 2 ;;
      *) die "unexpected argument: $1" ;;
    esac
  done
  [ -n "$vault" ] || vault="pcs-${env}"
  az_args=(--vault-name "$vault" --only-show-errors)
  if [ -n "$subscription" ]; then
    az_args+=(--subscription "$subscription")
  fi
}

fetch_secret() {
  az keyvault secret show "${az_args[@]}" --name "$secret_name" --query value -o tsv
}

store_secret() {
  local tmp
  tmp="$(mktemp)"
  chmod 600 "$tmp"
  printf '%s' "$1" > "$tmp"
  if ! az keyvault secret set "${az_args[@]}" --name "$secret_name" --file "$tmp" \
    --content-type application/json -o none; then
    rm -f "$tmp"
    die "cannot write $secret_name to $vault"
  fi
  rm -f "$tmp"
}

[ $# -ge 1 ] || usage
if [ "$1" = "--self-test" ]; then
  self_test
  exit 0
fi
[ $# -ge 3 ] || usage

env="$1"
username="$2"
action="$3"
shift 3

[[ "$username" =~ $USERNAME_PATTERN ]] || die "username must match [a-z0-9-]{3,40}"
[[ "$env" =~ ^[a-z0-9]+$ ]] || die "invalid environment"
secret_name="bulk-claims-client-${username}"

self_test >/dev/null

case "$action" in
  --disable)
    parse_options "$@"
    require_az
    current="$(fetch_secret)" || die "cannot read $secret_name from $vault"
    store_secret "$(printf '%s' "$current" | sed 's/"enabled":true/"enabled":false/')"
    echo "disabled $username in $vault; APIM may keep accepting it until its client cache expires" >&2
    ;;
  --check)
    parse_options "$@"
    require_az
    password="${BULK_CLAIMS_PASSWORD:-}"
    if [ -z "$password" ]; then
      read -r -s -p "Password for $username: " password
      echo >&2
    fi
    current="$(fetch_secret)" || die "cannot read $secret_name from $vault"
    salt="$(json_field "$current" salt)"
    expected="$(json_field "$current" passwordHash)"
    [ -n "$salt" ] && [ -n "$expected" ] || die "secret is not in the expected format"
    actual="$(hash_password "$salt" "$password")"
    enabled=false
    case "$current" in *'"enabled":true'*) enabled=true ;; esac
    if constant_time_equals "$actual" "$expected" && [ "$enabled" = true ]; then
      echo "ok: credentials accepted for organisation $(json_field "$current" organisationId)"
    else
      die "credentials rejected (enabled=$enabled)"
    fi
    ;;
  --*)
    usage
    ;;
  *)
    organisation_id="$action"
    [[ "$organisation_id" =~ $ORGANISATION_PATTERN ]] || die "organisation id must match [A-Za-z0-9]{3,32}"
    parse_options "$@"
    require_az
    password="$("$OPENSSL" rand -base64 36 | tr -d '\n=' | tr '+/' '-_')"
    salt="$("$OPENSSL" rand -base64 32 | tr -d '\n')"
    password_hash="$(hash_password "$salt" "$password")"
    store_secret "{\"organisationId\":\"${organisation_id}\",\"salt\":\"${salt}\",\"passwordHash\":\"${password_hash}\",\"enabled\":true}"
    echo "stored $secret_name in $vault for organisation $organisation_id" >&2
    echo "password (shown once, hand over securely):" >&2
    echo "$password"
    ;;
esac
