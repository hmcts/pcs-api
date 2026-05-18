#!/usr/bin/env bash
set -euo pipefail

: "${HMCTS_ENV:?Set HMCTS_ENV (e.g. aat)}"
s2s_BASE="http://rpe-service-auth-provider-${HMCTS_ENV.service.core-compute-${HMCTS_ENV.internal/testing-support/lease"

curl --location "${s2s_BASE}" \
--header 'Content-Type: application/json' \
--data '{
    "microservice": "am_org_role_mapping_service"
}'


curl --location 'https://idam-api.aat.platform.hmcts.net/o/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'redirect_uri=http://localhost:3451/oauth2redirect' \
--data-urlencode 'client_id=ccd_gateway' \
--data-urlencode 'client_secret=vUstam6brAsT38ranuwRut65rakec4u6' \
--data-urlencode 'username=data.store.idam.system.user@mailinator.com' \
--data-urlencode 'password=QUFUUGE1NXdvcmQxMQ==' \
--data-urlencode 'scope=profile openid roles'



curl --location 'http://am-org-role-mapping-service-aat.service.core-compute-aat.internal/am/testing-support/createOrgMapping?userType=CASEWORKER' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiI3SndwS29NZDBZZ2UvZ3ZMbFdoL1U0QVN2WXc9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJkYXRhLnN0b3JlLmlkYW0uc3lzdGVtLnVzZXJAbWFpbGluYXRvci5jb20iLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiMTdjNjQ5MmQtMDJjOS00NzgwLTlkOTMtYmVkMzEzOWEzNzM4LTM4ODA3OSIsInN1Ym5hbWUiOiJkYXRhLnN0b3JlLmlkYW0uc3lzdGVtLnVzZXJAbWFpbGluYXRvci5jb20iLCJpc3MiOiJodHRwczovL2Zvcmdlcm9jay1hbS5zZXJ2aWNlLmNvcmUtY29tcHV0ZS1pZGFtLWFhdDIuaW50ZXJuYWw6ODQ0My9vcGVuYW0vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9obWN0cyIsInRva2VuTmFtZSI6ImFjY2Vzc190b2tlbiIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJhdXRoR3JhbnRJZCI6ImpWblR0cVdTVm02VUZpcmdxSmRIc01YYXRUNCIsImNsaWVudF9pZCI6ImNjZF9nYXRld2F5IiwiYXVkIjoiY2NkX2dhdGV3YXkiLCJuYmYiOjE3NzkwOTI5ODcsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJyb2xlcyJdLCJhdXRoX3RpbWUiOjE3NzkwOTI5ODcsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNzc5MTIxNzg3LCJpYXQiOjE3NzkwOTI5ODcsImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiJwRjZ0Vk9hZHhoZi1nYWtmMVEtWnl6TnU4Z1EifQ.ec5clYhQAQ_HPt2zUooSsbCDCf-demHtvNUPvjAaim9J4fhpWXXjTfOIVQSLSrpPuJgSmxv8-PuwdhnNsIYSB4Dja9lC_vSDcNYxq0Ka11YSnGxuwtDoHhXTCty1Zfw4wYLE23y-cPiCVoqznyLihraixYyS5238FgB0vArUId150IaX_BhtC6fBLsPq8Qj02cQAxDTOF7VYVK3plCL2HPOIJRE3LRyGbpSeasZvAItC-RA0j2_Icsubxzx7JVxsDRwtlPK5svK0cT0c328d5ewnBvIwiMx1SvaBKjNz2NdxJTMPep6GhIlxrH8CXGVERKypl9-3AFSLs7ystv4Bfw' \
--header 'ServiceAuthorization: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhbV9vcmdfcm9sZV9tYXBwaW5nX3NlcnZpY2UiLCJleHAiOjE3Nzg4NzUyNjR9.L4jaJgwztTv7CBKTW6MqbEV_qzM5CdczH554UZQwV-HDfUFCf8psRdz2mWOEx51JX8dGJgyie0OXuWeLxmkIZQ' \
--data '{
  "userIds": [
    "84840d4f-3354-4c9e-9064-30c3450e9e95","dea305ad-2dc0-4f14-a0b6-43c59fc7124f"
  ]
}'
