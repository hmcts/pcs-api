locals {
  apim_count                     = var.bulk_claims_apim_enabled ? 1 : 0
  api_mgmt_suffix                = var.apim_suffix == "" ? var.env : var.apim_suffix
  api_mgmt_name                  = "cft-api-mgmt-${local.api_mgmt_suffix}"
  api_mgmt_rg                    = "cft-${var.env}-network-rg"
  api_mgmt_logger_id             = "/subscriptions/${var.aks_subscription_id}/resourceGroups/${local.api_mgmt_rg}/providers/Microsoft.ApiManagement/service/${local.api_mgmt_name}/loggers/${local.api_mgmt_name}-logger"
  bulk_claims_product            = "${var.product}-bulk-claims"
  bulk_claims_api                = "${var.product}-bulk-claims-api"
  bulk_claims_path               = "pcs-bulk-claims"
  bulk_claims_wsdl               = "${path.module}/apim-wsdl/BulkClaimService.wsdl"
  bulk_claims_wildcard_operation = "${var.product}-bulk-claims-wildcard"

  pcs_api_url = "http://pcs-api-${var.env}.service.core-compute-${var.env}.internal"
  s2s_url     = "http://rpe-service-auth-provider-${var.env}.service.core-compute-${var.env}.internal"

  bulk_claims_s2s_microservice     = "pcs_bulk_claims_apim"
  bulk_claims_s2s_secret_name      = "pcs-bulk-claims-apim-s2s-secret"
  bulk_claims_s2s_named_value_name = "pcs-bulk-claims-s2s-secret"

  bulk_claims_policy_vars = {
    key_vault_name                   = data.azurerm_key_vault.pcs_key_vault.name
    s2s_base_url                     = local.s2s_url
    s2s_client_id                    = local.bulk_claims_s2s_microservice
    s2s_secret_named_value           = local.bulk_claims_s2s_named_value_name
    rate_limit_calls                 = var.apim_rate_limit_calls
    rate_limit_renewal_period        = var.apim_rate_limit_renewal_period
    status_rate_limit_calls          = var.apim_status_rate_limit_calls
    status_rate_limit_renewal_period = var.apim_status_rate_limit_renewal_period
    max_request_bytes                = var.apim_max_request_bytes
    client_cache_seconds             = var.apim_client_cache_seconds
    s2s_cache_seconds                = var.apim_s2s_cache_seconds
  }
}

provider "azurerm" {
  alias           = "aks-cftapps"
  subscription_id = var.aks_subscription_id
  features {}
}

data "azurerm_client_config" "current" {}

data "azurerm_api_management" "cft_api_mgmt" {
  count               = local.apim_count
  provider            = azurerm.aks-cftapps
  name                = local.api_mgmt_name
  resource_group_name = local.api_mgmt_rg
}

# The shared APIM instance's managed identity reads the per-client records and the S2S secret.
# The pcs-<env> vault is created outside this repository, so it is read as a data source.
resource "azurerm_key_vault_access_policy" "apim_bulk_claims" {
  count        = local.apim_count
  key_vault_id = data.azurerm_key_vault.pcs_key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azurerm_api_management.cft_api_mgmt[0].identity[0].principal_id

  secret_permissions = ["Get"]
}

# Key Vault backed, so the S2S secret never enters Terraform state or the policy text.
# The secret must already exist in the vault; APIM fetches it when the named value is created.
resource "azurerm_api_management_named_value" "bulk_claims_s2s_secret" {
  count               = local.apim_count
  provider            = azurerm.aks-cftapps
  name                = local.bulk_claims_s2s_named_value_name
  display_name        = local.bulk_claims_s2s_named_value_name
  api_management_name = local.api_mgmt_name
  resource_group_name = local.api_mgmt_rg
  secret              = true

  value_from_key_vault {
    secret_id = "${data.azurerm_key_vault.pcs_key_vault.vault_uri}secrets/${local.bulk_claims_s2s_secret_name}"
  }

  depends_on = [azurerm_key_vault_access_policy.apim_bulk_claims]
}

# Basic authentication against Key Vault records is the gate, so no APIM subscription key is required.
module "api_mgmt_product" {
  count                 = local.apim_count
  source                = "git@github.com:hmcts/cnp-module-api-mgmt-product?ref=master"
  name                  = local.bulk_claims_product
  approval_required     = "false"
  subscription_required = "false"
  api_mgmt_name         = local.api_mgmt_name
  api_mgmt_rg           = local.api_mgmt_rg
  providers = {
    azurerm = azurerm.aks-cftapps
  }
}

# SOAP pass-through API imported from the service WSDL, as the Civil SDT gateway is.
resource "azurerm_api_management_api" "bulk_claims" {
  count                 = local.apim_count
  provider              = azurerm.aks-cftapps
  name                  = local.bulk_claims_api
  display_name          = "PCS Bulk Claims API"
  api_management_name   = local.api_mgmt_name
  resource_group_name   = local.api_mgmt_rg
  path                  = local.bulk_claims_path
  service_url           = "${local.pcs_api_url}/v2/bulk-claims/services/BulkClaim"
  protocols             = ["https"]
  api_type              = "soap"
  revision              = "1"
  subscription_required = false

  import {
    content_format = "wsdl"
    content_value  = file(local.bulk_claims_wsdl)
    wsdl_selector {
      service_name  = "BulkClaimServiceImplService"
      endpoint_name = "BulkClaim"
    }
  }
}

# Lets requests without a SOAPAction header through; the service selects the operation from the body.
resource "azurerm_api_management_api_operation" "bulk_claims_wildcard" {
  count               = local.apim_count
  provider            = azurerm.aks-cftapps
  operation_id        = local.bulk_claims_wildcard_operation
  display_name        = "wildcard"
  description         = "Allows requests without a soapAction header"
  api_name            = azurerm_api_management_api.bulk_claims[0].name
  api_management_name = local.api_mgmt_name
  resource_group_name = local.api_mgmt_rg
  method              = "POST"
  url_template        = "/?soapAction={any}"

  template_parameter {
    name     = "any"
    required = true
    type     = "string"
  }

  request {
    representation {
      content_type = "text/xml"
    }
  }
}

resource "azurerm_api_management_product_api" "bulk_claims" {
  count               = local.apim_count
  provider            = azurerm.aks-cftapps
  api_name            = azurerm_api_management_api.bulk_claims[0].name
  product_id          = module.api_mgmt_product[0].product_id
  api_management_name = local.api_mgmt_name
  resource_group_name = local.api_mgmt_rg
}

module "api_mgmt_policy" {
  count                  = local.apim_count
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.api_mgmt_name
  api_mgmt_rg            = local.api_mgmt_rg
  api_name               = azurerm_api_management_api.bulk_claims[0].name
  api_policy_xml_content = templatefile("${path.module}/apim-policy/api-policy.xml", local.bulk_claims_policy_vars)
  providers = {
    azurerm = azurerm.aks-cftapps
  }

  depends_on = [
    azurerm_api_management_api_operation.bulk_claims_wildcard,
    azurerm_api_management_named_value.bulk_claims_s2s_secret,
    azurerm_key_vault_access_policy.apim_bulk_claims,
  ]
}

# Request and response bodies carry personal data and are deliberately not logged.
resource "azurerm_api_management_api_diagnostic" "bulk_claims" {
  count                    = local.apim_count
  provider                 = azurerm.aks-cftapps
  identifier               = "applicationinsights"
  api_management_logger_id = local.api_mgmt_logger_id
  api_management_name      = local.api_mgmt_name
  api_name                 = azurerm_api_management_api.bulk_claims[0].name
  resource_group_name      = local.api_mgmt_rg

  sampling_percentage       = 100.0
  always_log_errors         = true
  log_client_ip             = true
  verbosity                 = "information"
  http_correlation_protocol = "W3C"

  frontend_request {
    headers_to_log = ["content-type", "content-length", "accept", "soapaction"]
  }

  frontend_response {
    headers_to_log = ["content-type", "content-length"]
  }

  backend_request {
    headers_to_log = ["content-type", "content-length", "x-organisation-id"]
  }

  backend_response {
    headers_to_log = ["content-type", "content-length"]
  }
}
