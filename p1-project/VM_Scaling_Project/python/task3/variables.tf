# instance type
variable "instance_type" {
  default = "m5.large"
}
variable "key_name" {
  default = "15619-CC"
}

# Update "project_tag" to match the tagging requirement of the ongoing project
variable "project_tag" {
  default = "vm-scaling"
}

# Update "ami_id"
variable "ami_lg" {
}

# Update "ami_id"
variable "ami_web" {
}

# Launch config
variable "launch_configuration_name" {
  default = "p1-ASG EC2"
}

# ASG stuff

variable "asg_name" {
  default = "p1-ASG"
}

variable "asg_max_inst" {
  default = 3
}

variable "asg_min_inst" {
  default = 1
}

variable "asg_default_capacity" {
  default = 2
}

variable "asg_cooldown" {
  default = 60
}

variable "asg_health_check_grace_period" {
  default = 60
}

# ELB
variable "elb_name" {
  default = "p1-ELB"
}
# health check
variable "lb_healthcheck_interval" {
  default = 10
}

# TG
variable "tg_name" {
  default = "p1-TG-ELB"
}

# Cloudwatch
variable "cw_lower_cpu_alarm_period" {
  default = 30
}
variable "cw_upper_cpu_alarm_period" {
  default = 30
}

variable "cw_lower_cpu_eval_period" {
  default = 6
}

variable "cw_upper_cpu_eval_period" {
  default = 1
}

variable "cw_cpu_upper_threshold" {
  default = 75
}
variable "cw_cpu_lower_threshold" {
  default = 20
}

