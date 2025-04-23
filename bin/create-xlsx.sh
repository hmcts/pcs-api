#!/usr/bin/env bash

# This script is used to create the xlsx file for the given version and environment
env=$1

# Get the current directory
run_dir=$(pwd)
ls "$run_dir/build/definitions/"
# Check if the directory exists
if [ ! -d "$run_dir/build/definitions/PCS" ]; then
  echo "Error: Directory $run_dir/build/definitions/PCS does not exist."
  exit 1
fi

# Check if the environment is provided, if not default to local
if [ -z "$env" ]; then
  env="local"
  echo "No environment specified, defaulting to the local naming convention."
fi


# Set the CCD definition version based on the environment

case "$env" in
  local|aat|prod|demo|ithc|perftest)
    ccd_def_env="$env"
    ;;
  preview)
    ccd_def_env="pr_${CHANGE_ID:-unknown}"
    ;;
  *)
    echo "Error: Invalid environment '$env'. Valid options: local, preview, aat, prod, demo, ithc, perftest"
    exit 1
    ;;
esac

# Create the xlsx file name for the CCD definition
ccd_definition_file="CCD_Definition_${ccd_def_env}.xlsx"

# Runs the CCD JSON -> XLSX converter and outputs to the output directory
docker run --rm --name json2xlsx \
  -v "$run_dir/build/definitions/PCS:/build/definitions/PCS" \
  -v "$run_dir/build/definitions:/build/definitions" \
  hmctspublic.azurecr.io/ccd/definition-processor:latest \
  json2xlsx -D /build/definitions/PCS -o "/build/definitions/${ccd_definition_file}"
