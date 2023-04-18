
// General varibles
variable "rg_name" {
  default = "social-network-rg"
}

variable "location" {
  default = "East US"
}

variable "tag" {
  default = "social-network"
}

# Consists of only lowercase and numeric value
# Max length = 16
variable "storage_account_prefix" {
  default = "socialnetwork"
}

variable "container_name" {
  default = "system"
}

variable "storage_tier" {
  default = "Premium"
}

variable "replication_type" {
  default = "LRS"
}

variable "vhd_relative_path" {
  default = "Microsoft.Compute/Images/vhds/Project-Image-v7-osDisk.3342700b-6f1c-4e78-821d-8109d2000cbb.vhd"
}

variable "vhd_absolute_path" {
  default = "https://sail.blob.core.windows.net/system/Microsoft.Compute/Images/vhds/Project-Image-v7-osDisk.3342700b-6f1c-4e78-821d-8109d2000cbb.vhd"
}

variable "image_name" {
  default = "CloudCourseVMImage"
}

variable "os_type" {
  default = "Linux"
}

variable "os_size" {
  default = 250
}

variable "os_caching" {
  default = "ReadWrite"
}

variable "zone_resilient" {
  default = false
}

variable "data_disk_size" {
  default = 20
}


// Variables to build Azure VM
variable "virtual_network_name" {
  default = "cloud-computing-vnet"
}

variable "network_interface_name" {
  default = "cloud-computing-network-interface"
}

variable "sg_name" {
  default = "cloud-computing-network-security-group"
}

variable "public_ip_name" {
  default = "cloud-computing-ip"
}

variable "public_ip_type" {
  default = "Dynamic"
}

variable "private_ip_type" {
  default = "Dynamic"
}

variable "vm_name" {
  default = "CloudComputingProjectVM"
}

variable "vm_size" {
  default = "Standard_B2ms"
}

variable "os_disk_type" {
  default = "Premium_LRS"
}

variable "username" {
  default = "clouduser"
}

variable "vnet_prefix" {
  default = "10.0.1.0/24"
}

variable "os_disk_name" {
  default = "social_network_os_disk"
}

variable "data_disk_name" {
  default = "social_network_data_disk"
}

// Do not expose your password for VM there.
// Instead, use "export TF_VAR_password=" to set it as an environment variable
// Otherwise, you will be prompted to set password after "terraform apply"
variable "password" {}
