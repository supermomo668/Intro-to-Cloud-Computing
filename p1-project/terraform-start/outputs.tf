output "instance_ip_addr" {
  value       = aws_instance.cmucc.private_ip
  description = "The private IP address of the instance."
}
