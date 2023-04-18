provider "azurerm" {
  version = "=2.85.0"
}

# Create a resource group if it doesn’t exist
resource "azurerm_resource_group" "terraform_rg" {
  name     = "terraform_primer_rg"
  location = "eastus"

  tags = {
    environment = "terraform_primer"
    project     = "getting-started-with-cloud-computing"
  }
}

# Create virtual network
resource "azurerm_virtual_network" "terraform_network" {
  name                = "terraform_primer_vnet"
  address_space       = ["10.0.0.0/16"]
  location            = "eastus"
  resource_group_name = azurerm_resource_group.terraform_rg.name

  tags = {
    environment = "terraform_primer"
    project     = "getting-started-with-cloud-computing"
  }
}

# Create subnet
resource "azurerm_subnet" "terraform_subnet" {
  name                 = "terraform_primer_subnet"
  resource_group_name  = azurerm_resource_group.terraform_rg.name
  virtual_network_name = azurerm_virtual_network.terraform_network.name
  address_prefix       = "10.0.1.0/24"
}

# Create public IPs
resource "azurerm_public_ip" "terraform_ip" {
  name                = "terraform_primer_ip"
  location            = "eastus"
  resource_group_name = azurerm_resource_group.terraform_rg.name
  allocation_method   = "Static"

  tags = {
    environment = "terraform_primer"
    project     = "getting-started-with-cloud-computing"
  }
}

# Create Network Security Group and rule
resource "azurerm_network_security_group" "terraform_sg" {
  name                = "terraform_primer_sg"
  location            = "eastus"
  resource_group_name = azurerm_resource_group.terraform_rg.name

  security_rule {
    name                       = "SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  tags = {
    environment = "terraform_primer"
    project     = "getting-started-with-cloud-computing"
  }
}

# Create network interface
resource "azurerm_network_interface" "terraform_nic" {
  name                      = "terraform_primer_nic"
  location                  = "eastus"
  resource_group_name       = azurerm_resource_group.terraform_rg.name
  network_security_group_id = azurerm_network_security_group.terraform_sg.id

  ip_configuration {
    name                          = "terraform_nic_conf"
    subnet_id                     = azurerm_subnet.terraform_subnet.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.terraform_ip.id
  }

  tags = {
    environment = "terraform_primer"
    project     = "getting-started-with-cloud-computing"
  }
}

# Create virtual machine
resource "azurerm_virtual_machine" "terraform-vm" {
  name                  = "terraform_primer_vm"
  location              = "eastus"
  resource_group_name   = azurerm_resource_group.terraform_rg.name
  network_interface_ids = [azurerm_network_interface.terraform_nic.id]
  vm_size               = "Standard_DS1_v2"

  storage_os_disk {
    name              = "terraform_os_disk"
    caching           = "ReadWrite"
    create_option     = "FromImage"
    managed_disk_type = "Premium_LRS"
  }

  storage_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "16.04.0-LTS"
    version   = "latest"
  }

  os_profile {
    computer_name  = "terraform-vm"
    admin_username = "azureuser"
    # TODO: Create a terraform.tfvars file and a variables.tf file to set the password
    admin_password = var.azure_password
  }

  os_profile_linux_config {
    disable_password_authentication = false
  }

  tags = {
    environment = "terraform_primer"
    project     = "getting-started-with-cloud-computing"
  }
}
