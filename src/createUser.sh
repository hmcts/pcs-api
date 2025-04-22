#!/bin/bash
## Usage: ./createUser.sh roles email token [password] [surname] [forename] [id]
##
## Options:
##    - role: Comma-separated list of roles. Roles must exist in IDAM (i.e `caseworker-probate,caseworker-probate-solicitor`)
##    - email: Email address
##    - token: Authentication token returned from https://idam-web-public.aat.platform.hmcts.net/o/token
##    - password: User's password. Default to `Pa$$w0rd`. Weak passwords that do not match the password criteria by SIDAM will cause use creation to fail, and such failure may not be expressly communicated to the user.
##    - surname: Last name. Default to `Test`.
##    - forename: First name. Default to `User`.
##    - id: Unique identifier (36 characters or more eg: d8d8ad80-84d9-4416-95ea-bce1a6eb1247)
##
## Create a CCD caseworker with the roles `caseworker` and all additional roles
## provided in `roles` options.

rolesStr=$1
email=$2
password=${3:-Pa55w0rd}
surname=${4:-Test}
forename=${5:-User}
token=$6
id=${7:-15ad8d8ad80-84d9-4416-95ea-bce1a6eb124}

token=${token:15:1253}

if [ -z "$rolesStr" ]
  then
    echo "Usage: ./createUser.sh roles email token [password] [surname] [forename] [id]"
    exit 1
fi

IFS=',' read -ra roles <<< "$rolesStr"

# Build roles JSON array
rolesJson="["
firstRole=true
for i in "${roles[@]}"; do
  if [ "$firstRole" = false ] ; then
    rolesJson="${rolesJson},"
  fi
  rolesJson=''${rolesJson}'{"code":"'${i}'"}'
  firstRole=false
done
rolesJson="${rolesJson}]"

echo "Creating caseworker $email"

curl -L -X POST 'https://idam-testing-support-api.aat.platform.hmcts.net/test/idam/users' \
  -H 'Authorization: Bearer '${token} \
  -H "Content-Type: application/json" \
  --data-raw '{
    "password": "'${password}'",
    "user": {
        "id":"'${id}'",
        "email":"'${email}'",
        "forename":"'${forename}'",
        "surname":"'${surname}'",
        "roles": '${rolesJson}'
    }
  }'
