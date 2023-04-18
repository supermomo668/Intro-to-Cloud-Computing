# Provides resources for student workspace VM

# terraform init      Initialize a Terraform working directory
# terraform validate  Validates the Terraform files
# terraform fmt       Rewrites config files to canonical format
# terraform plan      Generate and show an execution plan
# terraform apply     Builds or changes infrastructure
# terraform destroy   Destroy Terraform-managed infrastructure

provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "project" {
  name     = var.rg_name
  location = var.location
  tags = {
    "project": "${var.tag}"
  }
}

# This will randomly generate a unique string within the system,
# which is used for the storage account's name
resource "random_string" "project" {
  length = 8
  special = false
  upper = false
  min_lower = 1
  min_numeric = 1
}

# Storage account, container, and blob
resource "azurerm_storage_account" "project" {
  name                     = format("%s%s","${var.storage_account_prefix}","${random_string.project.result}")
  resource_group_name      = azurerm_resource_group.project.name
  location                 = azurerm_resource_group.project.location
  account_tier             = "Premium"
  account_replication_type = "LRS"

  tags = {
    "project": "${var.tag}"
  }
}

resource "azurerm_storage_container" "project" {
  name                  = var.container_name
  storage_account_name  = azurerm_storage_account.project.name
  container_access_type = "private"
}

resource "azurerm_storage_blob" "project" {
  name                   = var.vhd_relative_path
  storage_account_name   = azurerm_storage_account.project.name
  storage_container_name = azurerm_storage_container.project.name
  type                   = "Block"
  source_uri             = var.vhd_absolute_path
}


# Image for student workspace VM
resource "azurerm_image" "project" {
  name                = var.image_name
  location            = azurerm_resource_group.project.location
  resource_group_name = azurerm_resource_group.project.name

  os_disk {
    os_type  = var.os_type
    os_state = "Generalized"
    blob_uri = "https://${azurerm_storage_account.project.name}.blob.core.windows.net/${azurerm_storage_container.project.name}/${azurerm_storage_blob.project.name}"
    size_gb  = var.os_size
    caching = var.os_caching
  }

  zone_resilient = var.zone_resilient

  tags = {
    "project": "${var.tag}"
  }
}


// Security group for VM
resource "azurerm_network_security_group" "project" {
  name                = var.sg_name
  location            = azurerm_resource_group.project.location
  resource_group_name = azurerm_resource_group.project.name
}

// Security rule for SSH
resource "azurerm_network_security_rule" "ssh" {
  name                        = "SSH"
  priority                    = 300
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "22"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

// Security rule for HTTP
resource "azurerm_network_security_rule" "http" {
  name                        = "HTTP"
  priority                    = 320
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "80"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

resource "azurerm_network_security_rule" "frontend" {
  name                        = "Frontend"
  priority                    = 340
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "3000"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

resource "azurerm_network_security_rule" "backend" {
  name                        = "Backend"
  priority                    = 330
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "8080"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

resource "azurerm_network_security_rule" "mongo" {
  name                        = "MongoDB"
  priority                    = 350
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "27017"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

resource "azurerm_network_security_rule" "https" {
  name                        = "Neo4j"
  priority                    = 360
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "7474"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

resource "azurerm_network_security_rule" "bolt" {
  name                        = "Bolt"
  priority                    = 370
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "7687"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  resource_group_name         = azurerm_resource_group.project.name
  network_security_group_name = azurerm_network_security_group.project.name
}

# Network infra for the VM
resource "azurerm_virtual_network" "project" {
  name                = var.virtual_network_name
  address_space       = ["${var.vnet_prefix}"]
  location            = azurerm_resource_group.project.location
  resource_group_name = azurerm_resource_group.project.name
}

resource "azurerm_subnet" "project" {
  name                 = "default"
  resource_group_name  = azurerm_resource_group.project.name
  virtual_network_name = azurerm_virtual_network.project.name
  address_prefixes     = ["${var.vnet_prefix}"]
}

resource "azurerm_public_ip" "project" {
  name                = var.public_ip_name
  resource_group_name = azurerm_resource_group.project.name
  location            = azurerm_resource_group.project.location
  allocation_method   = var.public_ip_type
}

resource "azurerm_network_interface" "project" {
  name                = var.network_interface_name
  location            = azurerm_resource_group.project.location
  resource_group_name = azurerm_resource_group.project.name

  ip_configuration {
    name                          = "ipconfig1"
    subnet_id                     = azurerm_subnet.project.id
    private_ip_address_allocation = var.private_ip_type
    public_ip_address_id = azurerm_public_ip.project.id
  }
}

resource "azurerm_network_interface_security_group_association" "project" {
    network_interface_id      = azurerm_network_interface.project.id
    network_security_group_id = azurerm_network_security_group.project.id
}


# VM that created from the image
resource "azurerm_virtual_machine" "project" {
  name                = var.vm_name
  resource_group_name = azurerm_resource_group.project.name
  location            = azurerm_resource_group.project.location
  vm_size             = var.vm_size
  network_interface_ids = [
    azurerm_network_interface.project.id,
  ]

  os_profile {
    computer_name = var.vm_name
    admin_username = var.username
    admin_password = var.password
  }

  os_profile_linux_config {
    disable_password_authentication = false
  }

  storage_image_reference {
    id = azurerm_image.project.id
  }

  storage_os_disk {
    name = var.os_disk_name
    create_option = "FromImage"
    disk_size_gb = var.os_size
    managed_disk_type = var.os_disk_type
  }

  storage_data_disk {
    name = var.data_disk_name
    create_option = "Empty"
    disk_size_gb = var.data_disk_size
    lun = 0
  }

  tags = {
    "project": "${var.tag}"
  }
}

# Wait until the public IP address is available
data "azurerm_public_ip" "project" {
  name                = azurerm_public_ip.project.name
  resource_group_name = azurerm_virtual_machine.project.resource_group_name
}
