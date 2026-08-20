#!/usr/bin/env bash

set -eu

branchName=${1:-master}

# Run from ${WORKSPACE}/bin. Clean first: on master this runs twice (aat then prod), and a second
# `cp -r src/main/resources/ .` over an existing ./resources nests it as ./resources/resources,
# which uploads every BPMN twice.
rm -rf ./wa-standalone-task-bpmn ./resources

#Checkout specific branch camunda bpmn definition
echo "Pull wa-standalone-task-bpmn"
git clone https://github.com/hmcts/wa-standalone-task-bpmn.git
cd wa-standalone-task-bpmn

echo "Switch to ${branchName} branch on wa-standalone-task-bpmn"
git checkout "${branchName}"
cd ..

#Copy camunda folder which contains bpmn files
cp -r ./wa-standalone-task-bpmn/src/main/resources ./resources
rm -rf ./wa-standalone-task-bpmn
