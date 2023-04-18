# TODO: Create a file named `terraform.tfvars` and set the values of the variables defined in `variables.tf`

# terraform init      Initialize a Terraform working directory
# terraform validate  Validates the Terraform files
# terraform fmt       Rewrites config files to canonical format
# terraform plan      Generate and show an execution plan
# terraform apply     Builds or changes infrastructure
# terraform destroy   Destroy Terraform-managed infrastructure

provider "aws" {
  region = "us-east-1"
}

resource "aws_security_group" "student_ami_sg" {
    #id = "${var.security_group_id}"
    name = "load_generator"
    description = "load_generator - 80 only"
  # inbound internet access
  # allowed: only port 22, 80 are open
  # you are NOT allowed to open all the ports to the public
  /*
  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }*/
    
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
}

resource "aws_instance" "cmucc" {
  ami           = var.ami_id1
  instance_type = var.instance_type

  vpc_security_group_ids = [aws_security_group.student_ami_sg.id]

  tags = {
    Project = var.project_tag
  }

  # You may optionally specify a SSH key that exists in your AWS account
  key_name = var.key_name
}

resource "aws_instance" "cmucc2" {
  ami           = var.ami_id2
  instance_type = var.instance_type

  vpc_security_group_ids = [aws_security_group.student_ami_sg.id]

  tags = {
    Project = var.project_tag
  }

  # You may optionally specify a SSH key that exists in your AWS account
  key_name = var.key_name
}
