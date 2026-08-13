#!/usr/bin/env bash

set -eu
scriptDir=$(dirname "$0")/../..

dmnFilepath="$scriptDir/src/main/resources/dmn"

if [[ ! -d "${dmnFilepath}" ]]; then
  echo "No DMN directory at ${dmnFilepath} — nothing to upload"
  exit 0
fi

for file in $(find "${dmnFilepath}" -name '*.dmn')
do
  uploadResponse=$(curl --insecure -v --silent -w "\n%{http_code}" --show-error -X POST \
    "http://localhost:8097/engine-rest/deployment/create" \
    -H "Accept: application/json" \
    -F "deployment-name=$(basename "${file}")" \
    -F "deploy-changed-only=true" \
    -F "deployment-source=pcs" \
    -F "tenant-id=pcs" \
    -F "file=@${dmnFilepath}/$(basename "${file}")")

upload_http_code=$(echo "$uploadResponse" | tail -n1)
upload_response_content=$(echo "$uploadResponse" | sed '$d')

if [[ "${upload_http_code}" == '200' ]]; then
  echo "$(basename "${file}") diagram uploaded successfully (${upload_response_content})"
  continue;
fi

echo "$(basename "${file}") upload failed with http code ${upload_http_code} and response (${upload_response_content})"
continue;

done
