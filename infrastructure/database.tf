provider "azurerm" {
  features {}
  resource_provider_registrations = ""
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}

data "azurerm_key_vault" "pcs_key_vault" {
  name                = "pcs-${var.env}"
  resource_group_name = "pcs-${var.env}"
}

module "postgresql" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env    = var.env

  product       = var.product
  component     = var.component
  name        = "${var.product}"
  business_area = "cft"

  subnet_suffix = "expanded"
  common_tags = var.common_tags
  pgsql_databases = [
    {
      name : var.product
    }
  ]

  pgsql_version = var.pgsql_version
  admin_user_object_id = var.jenkins_AAD_objectId
}

# FlexibleServer v14 creds
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.postgresql.username
  key_vault_id = data.azurerm_key_vault.pcs_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.postgresql.password
  key_vault_id = data.azurerm_key_vault.pcs_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.postgresql.fqdn
  key_vault_id = data.azurerm_key_vault.pcs_key_vault.id
}
