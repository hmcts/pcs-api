# Docmosis access key for bulk print document conversion (office formats to PDF), copied from
# Evidence Management's shared vault, where the platform's Docmosis key lives. em-stitching-api
# copies the same secret the same way.
data "azurerm_key_vault" "rpa_key_vault" {
  name                = "rpa-${var.env}"
  resource_group_name = "rpa-${var.env}"
}

data "azurerm_key_vault_secret" "docmosis_access_key" {
  name         = "docmosis-access-key"
  key_vault_id = data.azurerm_key_vault.rpa_key_vault.id
}

resource "azurerm_key_vault_secret" "docmosis_access_key" {
  name         = "docmosis-access-key"
  value        = data.azurerm_key_vault_secret.docmosis_access_key.value
  key_vault_id = data.azurerm_key_vault.pcs_key_vault.id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "Vault ${data.azurerm_key_vault.rpa_key_vault.name}"
  })
}
