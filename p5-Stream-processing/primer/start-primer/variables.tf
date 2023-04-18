variable "project_tag" {
  default = "intro-to-stream"
}

variable "emr_cluster_name" {
  default = "p43-primer-emr-cluster"
}

variable "region" {
  default = "us-east-1"
}

variable "zone" {
  default = "us-east-1c"
}

variable "cluster_release_label" {
  default = "emr-5.29.0"
}

variable "master_node_instance_type" {
  default = "m4.large"
}

variable "master_node_instance_count" {
  default = "1"
}

variable "master_node_bid_price" {
  default = "0.1"
}

variable "core_node_instance_type" {
  default = "m4.large"
}

variable "core_node_instance_count" {
  default = "2"
}

variable "core_node_bid_price" {
  default = "0.1"
}

# Update "key_name" with the key pair name
variable "key_name" {}

# Update "config" with the json file which defines EMR configurations
# You can update the configurations in the emr_configuration.json file
variable "config" {
  default = "emr_configurations.json"
}
