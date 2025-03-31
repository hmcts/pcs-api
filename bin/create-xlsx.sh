#!/usr/bin/env bash

# This script is used to create the xlsx file for the given version and environment
ENV=$1

# Get the current directory
RUN_DIR=$(pwd)

# Check if the directory exists
if [ ! -d "$RUN_DIR/build/ccd-definition/PCS" ]; then
  echo "Error: Directory $RUN_DIR/build/ccd-definition/PCS does not exist"
  exit 1
fi

# Check if the version and environment are provided
if [ -z "$ENV" ]; then
  echo "Usage: $(basename "$0") <env>"
  exit 1
fi

# Get the tag version from the Chart.yaml file
TAG_VERSION=$(date +"%d%m%Y-%H%M%S")

# Set the CCD definition version based on the environment
case ${ENV} in
  local)
    CCD_DEF_VERSION="${TAG_VERSION}_local"
    ;;
  pr)
    CCD_DEF_VERSION="${TAG_VERSION}_pr_${CHANGE_ID:-unknown}"
    ;;
  aat)
    CCD_DEF_VERSION="${TAG_VERSION}_aat"
    ;;
  prod)
    CCD_DEF_VERSION="${TAG_VERSION}_prod"
    ;;
  demo)
    CCD_DEF_VERSION="${TAG_VERSION}_demo"
    ;;
  ithc)
    CCD_DEF_VERSION="${TAG_VERSION}_ithc"
    ;;
  perftest)
    CCD_DEF_VERSION="${TAG_VERSION}_perftest"
    ;;
  *)
    echo "Invalid environment"
    exit 1
    ;;
esac

echo "CCD definition version: $CCD_DEF_VERSION"

# Create the xlsx file for the CCD definition
CcdDefinitionFile="CCD_Definition_${CCD_DEF_VERSION}.xlsx"
echo "Creating xlsx file for the CCD definition: $CcdDefinitionFile"

# Runs the CCD JSON -> XLSX converter
docker run --rm --name json2xlsx \
  -v "$RUN_DIR/build/ccd-definition/PCS:/build/ccd-definition/PCS" \
  -v "$RUN_DIR/output:/tmp/output" \
  hmctspublic.azurecr.io/ccd/definition-processor:latest \
  json2xlsx -D /build/ccd-definition/PCS -o "/tmp/output/${CcdDefinitionFile}"
