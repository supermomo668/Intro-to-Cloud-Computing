###########################################################################
# Template for P2 AWS Autoscaling Test                                  #
# Do not edit the first section                                           #
# Only edit the second section to configure appropriate scaling policies  #
###########################################################################

############################
# FIRST SECTION BEGINS     #
# DO NOT EDIT THIS SECTION #
############################
locals {
  common_tags = {
    Project = "vm-scaling"
  }
}

provider "aws" {
  region = "us-east-1"
}

resource "aws_default_vpc" "default" {
  tags = {
    Name = "Default VPC"
  }
}

resource "aws_security_group" "lg" {
  # HTTP access from anywhere
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

resource "aws_security_group" "elb_asg" {
  # HTTP access from anywhere
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # outbound internet access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  vpc_id = aws_default_vpc.default.id 
  tags = local.common_tags
}

######################
# FIRST SECTION ENDS #
######################

############################
# SECOND SECTION BEGINS    #
# PLEASE EDIT THIS SECTION #
############################

# Step 1              =
# TODO              = Add missing values below
# ================================
resource "aws_launch_configuration" "lc" {
  name            = var.launch_configuration_name
  image_id        = var.ami_lg
  instance_type   = var.instance_type
  security_groups = [aws_security_group.elb_asg.id]
}

# Create an auto scaling group with appropriate parameters
# TODO              = fill the missing values per the placeholders
resource "aws_autoscaling_group" "p1" {
  name                      = var.asg_name
  max_size                  = var.asg_max_inst
  min_size                  = var.asg_min_inst
  desired_capacity          = var.asg_default_capacity
  default_cooldown          = var.asg_cooldown
  force_delete              = true
  health_check_grace_period = var.asg_health_check_grace_period
  health_check_type         = "EC2"
  launch_configuration      = aws_launch_configuration.lc.name
  target_group_arns         = [aws_lb_target_group.ec2_instances.arn]
  tag {
    key                 = "Project"
    value               = "vm-scaling"
    propagate_at_launch = true
  }
  vpc_zone_identifier = [aws_default_vpc.default.id]
  /*{
    ResourceId          = aws_autoscaling_group.p1.name
    ResourceType        = "auto-scaling-group"
  }*/
}


# TODO              = Create a Load Generator AWS instance with proper tags
resource "aws_instance" "load_generator" {
  ami           = var.ami_lg
  instance_type = var.instance_type

  vpc_security_group_ids = [aws_security_group.lg.id]

  tags = {
    Project = var.project_tag
  }

  # You may optionally specify a SSH key that exists in your AWS account
  key_name = var.key_name
}
# Step 2              =
# TODO              = Create an Application Load Balancer with appropriate listeners and target groups
# The lb_listener documentation demonstrates how to connect these resources
# Create and attach your subnet to the Application Load Balancer 

resource "aws_lb_target_group" "ec2_instances" {
  name     = "p1-TG-ELB"
  port     = 80
  protocol = "HTTP"
  vpc_id   = aws_default_vpc.default.id 
  health_check {
    healthy_threshold = 4
    interval          = var.lb_healthcheck_interval
    path              = "/"
    timeout           = 3
    port              = 80
  }
}


resource "aws_lb" "lb" {
  name               = var.elb_name
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.elb_asg.id]
  #for_each                   = data.aws_subnet_ids.p1.ids
  subnets                    = [aws_subnet.p1_1.id, aws_subnet.p1_2.id] #data.aws_subnet_ids.p1.ids #[for subnet in data.aws_subnet_ids.p1.ids : subnet]  # ${element(var.target_subnet_id, count.index)}
  enable_deletion_protection = false
  tags = {
    Project = var.project_tag
  }
}

resource "aws_lb_listener" "p1" {
  load_balancer_arn = aws_lb.lb.arn
  port              = "80"
  protocol          = "HTTP"
  default_action {
    target_group_arn = aws_lb_target_group.ec2_instances.arn
    type             = "forward"
  }
}

output "lb_dns" {
  value = aws_lb.lb.dns_name
}

/*
resource "aws_lb_listener" "front_end" {
  load_balancer_arn = aws_lb.lb.arn
  port              = "80"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = "arn:aws:iam::187416307283:server-certificate/test_cert_rab3wuqwgja25ct3n4jdj2tzu4"
  tags = { "key" = "Project"
    "value" = var.project_tag
  }
  default_action {
    target_group_arn = aws_lb_target_group.ec2_instances.arn
    type             = "forward"
  }
*/
# Create a new ALB Target Group attachment
resource "aws_autoscaling_attachment" "asg_attachment_bar" {
  autoscaling_group_name = aws_autoscaling_group.p1.id
  alb_target_group_arn   = aws_lb_target_group.ec2_instances.arn
}


# https             =//www.terraform.io/docs/providers/aws/r/lb.html
# https             =//registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/subnet
# https             =//www.terraform.io/docs/providers/aws/r/lb_listener.html
# https             =//www.terraform.io/docs/providers/aws/r/lb_target_group.html

# Step 3              =
# TODO              = Create 2 policies              = 1 for scaling out and another for scaling in
# Link it to the autoscaling group you created above
# https             =//www.terraform.io/docs/providers/aws/r/autoscaling_policy.html  
resource "aws_autoscaling_policy" "scaleout" {
  name                   = "scaling_out"
  scaling_adjustment     = 1
  policy_type            = "SimpleScaling"
  adjustment_type        = "ChangeInCapacity"
  cooldown               = var.asg_cooldown
  autoscaling_group_name = aws_autoscaling_group.p1.name
}
resource "aws_autoscaling_policy" "scalein" {
  name                   = "scaling_in"
  scaling_adjustment     = -1
  policy_type            = "SimpleScaling"
  adjustment_type        = "ChangeInCapacity"
  cooldown               = var.asg_cooldown
  autoscaling_group_name = aws_autoscaling_group.p1.name
}

# Step 4              =
# TODO              = Create 2 cloudwatch alarms             = 1 for scaling out and another for scaling in
# Link it to the autoscaling group you created above
# Dont forget to trigger the appropriate policy you created above when alarm is raised
# https             =//www.terraform.io/docs/providers/aws/r/cloudwatch_metric_alarm.html

resource "aws_cloudwatch_metric_alarm" "scaleup" {
  alarm_name          = "Upper CPU"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.cw_upper_cpu_eval_period
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = var.cw_upper_cpu_alarm_period
  statistic           = "Average"
  threshold           = var.cw_cpu_upper_threshold
  actions_enabled     = true
  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.p1.name
  }
  alarm_description = "Upper CPU utilization monitor"
  alarm_actions     = [aws_autoscaling_policy.scaleout.arn]
}

resource "aws_cloudwatch_metric_alarm" "scaledown" {
  alarm_name          = "Lower CPU"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = var.cw_upper_cpu_eval_period
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = var.cw_lower_cpu_alarm_period
  statistic           = "Average"
  threshold           = var.cw_cpu_lower_threshold
  actions_enabled     = true
  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.p1.name
  }
  alarm_description = "This metric monitors ec2 cpu utilization"
  alarm_actions     = [aws_autoscaling_policy.scalein.arn]
}
######################################
# SECOND SECTION ENDS                #
# MAKE SURE YOU COMPLETE ALL 4 STEPS #
######################################
