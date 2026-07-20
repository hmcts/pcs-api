#!/usr/bin/env bash
set -ex
env=$1
run_dir=$(pwd)

if [ -z "$env" ]; then
  env="local"
  echo "No environment specified, defaulting to the local naming convention."
fi

for case_dir in "$run_dir"/build/definitions/*/; do
  case_type=$(basename "$case_dir")

  echo "Processing case type: $case_type"

  if [ ! -d "$case_dir" ]; then
    echo "Skipping $case_type as it's not a directory."
    continue
  fi

  ccd_definition_file="CCD_Definition_${case_type}_${env}.xlsx"

az login --identity

az acr login --name hmctsprod --subscription 8999dec3-0104-4a27-94ee-6588559729d1

docker run --rm --name "json2xlsx" \
  -v "$run_dir/build/definitions/${case_type}:/tmp/ccd-input" \
  -v "$run_dir/build/definitions:/tmp/ccd-output" \
  hmctsprod.azurecr.io/ccd/definition-processor:latest \
  json2xlsx -D /tmp/ccd-input -o /tmp/ccd-output/"${ccd_definition_file}"

done
