output "instances" {
  description = "Per instance: address, application URL, value for the K3S_SERVER secret, and the SSH command (replace the key path with your private key)."
  value = {
    for name, eip in aws_eip.this : name => {
      public_ip          = eip.public_ip
      application_url    = "http://${eip.public_ip}/"
      kubernetes_api_url = "https://${eip.public_ip}:6443"
      namespaces         = var.instances[name].namespaces
      ssh_command        = "ssh -i ~/.ssh/crm-k3s ubuntu@${eip.public_ip}"
    }
  }
}
