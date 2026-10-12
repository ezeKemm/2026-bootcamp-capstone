output "public_ip" {
  description = "Elastic IP of the instance. The application is at http://<this address>/"
  value       = aws_eip.this.public_ip
}

output "application_url" {
  value = "http://${aws_eip.this.public_ip}/"
}

output "kubernetes_api_url" {
  description = "Value for the K3S_SERVER secret in the GitHub Environments."
  value       = "https://${aws_eip.this.public_ip}:6443"
}

output "ssh_command" {
  description = "Replace the key path with your private key."
  value       = "ssh -i ~/.ssh/crm-k3s ubuntu@${aws_eip.this.public_ip}"
}
