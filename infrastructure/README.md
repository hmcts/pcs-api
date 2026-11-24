# App infrastructure

Add any application specific infrastructure to the terraform files in this folder

This could be things like:
* a database
* redis
* vault
* application insights

If you don't need any application infrastructure here, you can delete the whole folder (it will speed up your Jenkins build)

## Bulk claims API Management

All of the resources below are created only when `bulk_claims_apim_enabled` is true. It defaults to false and is not set in any tfvars file. Set it to true per environment once these exist:

- S2S microservice `pcs_bulk_claims_apim` is registered with rpe-service-auth-provider and its key is in the `s2s-<env>` vault;
- Key Vault secret `pcs-bulk-claims-apim-s2s-secret` exists in the `pcs-<env>` vault, because APIM reads it when the named value is created;
- the shared APIM instance has a system-assigned managed identity and an `<apim name>-logger` Application Insights logger.

`apim.tf` publishes the bulk claims v2 API on the shared CFT API Management instance (`cft-api-mgmt-<env>`, or `cft-api-mgmt-<apim_suffix>`) at the path `pcs-bulk-claims`, forwarding to the SOAP endpoint `/v2/bulk-claims/services/BulkClaim` of `pcs-api`. It is a SOAP pass-through API (`api_type = "soap"`) imported from `src/main/resources/wsdl/bulkclaim/v2/BulkClaimService.wsdl`, service `BulkClaimServiceImplService`, port `BulkClaim`, plus a wildcard operation that admits requests without a `SOAPAction` header. The WSDL declares an empty `SOAPAction` for every operation, as PCOL's does, so the service chooses the operation from the first element of the SOAP body and the policy does the same. No subscription key is required.

The API policy in `apim-policy/api-policy.xml`:

- rejects bodies whose `Content-Length` exceeds `apim_max_request_bytes` (413);
- requires HTTP Basic credentials; the username must match `[a-z0-9-]{3,40}`, otherwise 401 with `WWW-Authenticate: Basic realm="pcs-bulk-claims"`;
- rate limits by username, counting failed attempts, with two separate counters. The SOAP operation is read from the first element of the body; the status operations `getClaimStatus` and `getClaimStatuses` share a counter keyed `<username>:status`, allowing `apim_status_rate_limit_calls` (default 600) per `apim_status_rate_limit_renewal_period` seconds (default 60). Every other request, the two process operations and anything unrecognised, uses a counter keyed on the username, allowing `apim_rate_limit_calls` (default 60) per `apim_rate_limit_renewal_period` seconds (default 60);
- reads Key Vault secret `bulk-claims-client-<username>` in the `pcs-<env>` vault using the APIM managed identity, cached for `apim_client_cache_seconds`;
- accepts the request only if the secret is enabled and `HMAC-SHA256(salt, password)` equals `passwordHash` (constant-time comparison); unknown user, wrong password and disabled client all return 401;
- strips `Authorization`, sets `X-Organisation-Id` from the secret's `organisationId`, and sets `ServiceAuthorization` from an S2S lease for `pcs_bulk_claims_apim`, cached for `apim_s2s_cache_seconds`;
- returns 503 if Key Vault or the S2S lease fails.

The S2S secret is read from Key Vault secret `pcs-bulk-claims-apim-s2s-secret` through the APIM named value `pcs-bulk-claims-s2s-secret`. The secret must exist before the first apply. Terraform also grants the APIM managed identity `Get` on secrets in the `pcs-<env>` vault.

Request and response bodies are not logged by the API diagnostic.

### Client records

`bin/bulk-claims-apim-client.sh` manages the per-client secrets:

```
bin/bulk-claims-apim-client.sh <env> <username> <organisation-id>   # create or rotate; prints the password once
bin/bulk-claims-apim-client.sh <env> <username> --disable
bin/bulk-claims-apim-client.sh <env> <username> --check             # verifies a password against the stored record
bin/bulk-claims-apim-client.sh --self-test                          # checks the hashing against a fixed vector
```

The secret value is `{"organisationId":"…","salt":"<base64>","passwordHash":"<base64>","enabled":true}` with `passwordHash = base64(HMAC-SHA256(key = salt, message = password))`, the same computation the policy performs.
