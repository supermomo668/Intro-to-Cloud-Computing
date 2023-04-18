output "instance_ip_addr" {
  value       = aws_instance.load_generator.private_ip
  description = "The private IP address of the instance."
}
