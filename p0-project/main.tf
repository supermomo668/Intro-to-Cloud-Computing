# Provides a resource to manage EC2 Fleets.
#
# Usage:
# Configure the credentials first with `aws configure`
# Create a file named `terraform.tfvars` and set the values of the variables defined in `variables.tf`
#
# terraform init      Initialize a Terraform working directory
# terraform validate  Validates the Terraform files
# terraform fmt       Rewrites config files to canonical format
# terraform plan      Generate and show an execution plan
# terraform apply     Builds or changes infrastructure
# terraform destroy   Destroy Terraform-managed infrastructure

provider "aws" {
  region = var.region
}

locals {
  common_tags = {
    Name    = "cloud-computing-workspace-machine"
    project = var.project_tag
  }
}

resource "aws_security_group" "student_ami_sg" {
  # inbound internet access
  # allowed: only port 22, 80 are open
  # you are NOT allowed to open all the ports to the public
  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  ingress {
    from_port = 80
    to_port   = 80
    protocol  = "tcp"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  # outbound internet access
  # allowed: any egress traffic to anywhere
  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  tags = local.common_tags
}

# Provides an EC2 launch template resource.
# Can be used to create EC2 instances or auto scaling groups.
resource "aws_launch_template" "student_ami_lt" {
  name_prefix = "student_image_launch_template"
  image_id    = var.ami_id
  key_name    = var.key_name

  vpc_security_group_ids = [
    aws_security_group.student_ami_sg.id,
  ]

  tag_specifications {
    # Tags of EC2 instances
    resource_type = "instance"

    tags = local.common_tags
  }

  tag_specifications {
    # Tags of EBS volumes
    resource_type = "volume"

    tags = local.common_tags
  }
}

resource "aws_ec2_fleet" "student_ec2_fleet" {
  launch_template_config {
    launch_template_specification {
      launch_template_id = aws_launch_template.student_ami_lt.id
      version            = "1"
    }

    override {
      availability_zone = var.zone
      instance_type     = var.instance_type

      # Maximum price per unit hour that you are willing to pay for a Spot Instance.
      max_price = var.bid_price
    }
  }

  target_capacity_specification {
    # Valid values: on-demand, spot.
    default_target_capacity_type = var.target_capacity_type
    total_target_capacity        = 1
  }

  # Terminate instances for an EC2 Fleet if it is deleted successfully.
  terminate_instances = true

  # Terminate running instances when the EC2 Fleet request expires or gets
  # cancelled.
  terminate_instances_with_expiration = true

  # The tags of the Fleet resource itself.
  # To tag instances at launch, specify the tags in the Launch Template.
  tags = local.common_tags
}
