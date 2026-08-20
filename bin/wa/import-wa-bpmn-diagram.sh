#!/usr/bin/env bash

set -eu
workspace=${1}

# No default: uploading to localhost would report success against nothing.
: "${CAMUNDA_BASE_URL:?CAMUNDA_BASE_URL must be set}"

# Resolve the s2s-token helper relative to this script so it works regardless of the
# directory the BPMN resources were pulled into.
BASEDIR=$(dirname "$0")
serviceToken=$("${BASEDIR}/../s2s-token.sh" pcs_api)

filepath="$(realpath "$workspace")/resources"

failed=0

for file in $(find "${filepath}" -name '*.bpmn')
do
  # No -v: it echoes the ServiceAuthorization header into the build log.
  uploadResponse=$(curl --insecure --silent -w "\n%{http_code}" --show-error -X POST \
    "${CAMUNDA_BASE_URL}/engine-rest/deployment/create" \
    -H "Accept: application/json" \
    -H "ServiceAuthorization: Bearer ${serviceToken}" \
    -F "deployment-name=$(date +"%Y%m%d-%H%M%S")-$(basename "${file}")" \
    -F "file=@${file}")

upload_http_code=$(echo "$uploadResponse" | tail -n1)
upload_response_content=$(echo "$uploadResponse" | sed '$d')

if [[ "${upload_http_code}" == '200' ]]; then
  echo "$(basename "${file}") diagram uploaded successfully to ${ENVIRONMENT:-unknown} (${upload_response_content})"
  continue;
fi

echo "$(basename "${file}") upload to ${ENVIRONMENT:-unknown} failed with http code ${upload_http_code} and response (${upload_response_content})" >&2
failed=1

done

# Attempt every file, then fail the build if any upload failed.
exit "${failed}"
