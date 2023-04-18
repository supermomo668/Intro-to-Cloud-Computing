##############################################3 VPC/subnets



resource "aws_subnet" "p1_1" {
  vpc_id     = aws_default_vpc.default.id
  cidr_block = "172.31.64.0/20"

  tags = {
    Name = "p1"
  }
}

resource "aws_subnet" "p1_2" {
  vpc_id     = aws_default_vpc.default.id
  cidr_block = "172.31.48.0/20"

  tags = {
    Name = "p1"
  }
}

/*
resource "aws_subnet" "custom1" {
  vpc_id            = aws_vpc.custom_vpc.id
  cidr_block        = "172.31.2.0/24"
  availability_zone = "us-east-1a"
  tags = {
    Name = "Custom_subnet-1"
  }
}
resource "aws_subnet" "custom2" {
  vpc_id            = aws_vpc.custom_vpc.id
  cidr_block        = "172.31.3.0/24"
  availability_zone = "us-east-1c"
  tags = {
    Name = "Custom_subnet2"
  }
}
*/

data "aws_subnet_ids" "p1" {
  vpc_id = aws_default_vpc.default.id
  /*
  tags = {
    Tier = "Private"
  }
  */
}

/*
## Iterator for subnet
data "aws_subnet" "p1" {
  for_each = data.aws_subnet_ids.p1.ids
  id       = each.value
}

resource "aws_default_subnet" "default_east" {
  availability_zone = "us-east-1c"
  #vpc_id = aws_default_vpc.default.id
  tags = {
    Name = "Default subnet for us-east-1c"
    #Tier = "Private"
  }
}
*/