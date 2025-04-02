#!/usr/bin/env bash

# This script is used to create the xlsx file for the given version and environment
env=$1

# Get the current directory
run_dir=$(pwd)

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

# Set the tag version based on current Date and Time
tag_version=$(date +"%d%m%Y_%H%M%S")

# Set the CCD definition version based on the environment
case ${env} in
  local)
    ccd_def_version="${tag_version}_local"
    ;;
  preview)
    ccd_def_version="${tag_version}_pr_${CHANGE_ID:-unknown}"
    ;;
  aat)
    ccd_def_version="${tag_version}_aat"
    ;;
  prod)
    ccd_def_version="${tag_version}_prod"
    ;;
  demo)
    ccd_def_version="${tag_version}_demo"
    ;;
  ithc)
    ccd_def_version="${tag_version}_ithc"
    ;;
  perftest)
    ccd_def_version="${tag_version}_perftest"
    ;;
  *)
    echo "Error: Invalid environment '$env'. Valid options are: local, preview, aat, prod, demo, ithc, perftest"
    exit 1
    ;;
esac

# Create the xlsx file name for the CCD definition
ccd_definition_file="CCD_Definition_${ccd_def_version}.xlsx"

# Runs the CCD JSON -> XLSX converter and outputs to the output directory
docker run --rm --name json2xlsx \
  -v "$run_dir/build/definitions/PCS:/build/definitions/PCS" \
  -v "$run_dir/build/definitions:/build/definitions" \
  hmctspublic.azurecr.io/ccd/definition-processor:latest \
  json2xlsx -D /build/definitions/PCS -o "/build/definitions/${ccd_definition_file}"
