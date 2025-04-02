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

# Define allowed environments
allowed_envs=("local" "preview" "aat" "prod" "demo" "perftest" "ithc")

# Check if the environment is valid
valid_env=false
for valid in "${allowed_envs[@]}"; do
  if [ "$env" == "$valid" ]; then
    valid_env=true
    break
  fi
done

# Exit if environment is not valid
if [ "$valid_env" == false ]; then
  echo "Error: Unknown environment '$env'. Allowed environments are: ${allowed_envs[*]}"
  exit 1
fi

# Set the CCD definition version based on the environment
if [ "${env}" == "preview" ]; then
  ccd_def_version="${tag_version}_pr_${CHANGE_ID:-unknown}"
else
  ccd_def_version="${tag_version}_${env}"
fi

# Create the xlsx file name for the CCD definition
ccd_definition_file="CCD_Definition_${ccd_def_version}.xlsx"

# Runs the CCD JSON -> XLSX converter and outputs to the output directory
docker run --rm --name json2xlsx \
  -v "$run_dir/build/definitions/PCS:/build/definitions/PCS" \
  -v "$run_dir/build/definitions:/build/definitions" \
  hmctspublic.azurecr.io/ccd/definition-processor:latest \
  json2xlsx -D /build/definitions/PCS -o "/build/definitions/${ccd_definition_file}"
