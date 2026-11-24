variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "aks_subscription_id" {}

variable "pgsql_version" {
  description = "The version of PostgreSQL Flexible Server to use."
  type        = string
  default     = "16"
}


variable "apim_suffix" {
  description = "Suffix of the shared CFT API Management instance (cft-api-mgmt-<suffix>). Defaults to the environment name when empty."
  type        = string
  default     = ""
}

variable "apim_rate_limit_calls" {
  description = "Calls allowed per bulk claims client username in each rate limit period, counting failed authentication attempts."
  type        = number
  default     = 60
}

variable "apim_rate_limit_renewal_period" {
  description = "Rate limit period in seconds."
  type        = number
  default     = 60
}

variable "apim_status_rate_limit_calls" {
  description = "Calls allowed per bulk claims client username in each status rate limit period, shared by the status and schema operations."
  type        = number
  default     = 600
}

variable "apim_status_rate_limit_renewal_period" {
  description = "Status rate limit period in seconds."
  type        = number
  default     = 60
}

variable "apim_max_request_bytes" {
  description = "Largest request body APIM accepts, in bytes. Keep in line with bulk-claims.max-request-size in pcs-api."
  type        = number
  default     = 10485760
}

variable "apim_client_cache_seconds" {
  description = "How long APIM caches a bulk claims client's Key Vault record. A disabled client keeps working for this long."
  type        = number
  default     = 300
}

variable "apim_s2s_cache_seconds" {
  description = "How long APIM caches the S2S token it leases for pcs_bulk_claims_apim."
  type        = number
  default     = 300
}

variable "bulk_claims_apim_enabled" {
  description = "Create the bulk claims API Management resources. Leave false until the prerequisites listed in the README exist."
  type        = bool
  default     = false
}
