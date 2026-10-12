variable "region" {
  description = "AWS region. Must be one the organisation's policies allow."
  type        = string
  default     = "us-west-2"
}

variable "name" {
  description = "Name prefix for all resources."
  type        = string
  default     = "northstar-crm"
}

variable "root_volume_gb" {
  description = "Size of the encrypted root volume. PostgreSQL and Kafka data live on it (k3s local-path)."
  type        = number
  default     = 30
}

variable "ssh_public_key" {
  description = "Contents of the public key (for example ~/.ssh/crm-k3s.pub). The private key never goes into Terraform."
  type        = string
}

variable "ssh_allowed_cidrs" {
  description = "Who may reach SSH (port 22), for example [\"203.0.113.7/32\"]. Empty closes SSH."
  type        = list(string)
  default     = []
}

variable "api_allowed_cidrs" {
  description = "Who may reach the Kubernetes API (6443). GitHub-hosted runners have no fixed addresses, so this is open by default; access is protected by TLS and a namespace-scoped token."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "web_allowed_cidrs" {
  description = "Who may reach the application (80, and 443 if TLS is added later)."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "k3s_version" {
  description = "Pinned k3s release (see https://github.com/k3s-io/k3s/releases)."
  type        = string
  default     = "v1.36.5+k3s1"
}

variable "instances" {
  description = <<-EOT
    One k3s instance per entry: the key names the instance, namespaces lists the application namespaces created on it.
    Default: one instance per deployed environment, so dev and stage are fully separate clusters with their own address.
    Budget fallback: one shared instance, for example { shared = { instance_type = "t3.xlarge", namespaces = ["crm-dev", "crm-stage"] } };
    then both environments share one address and the Ingress needs host rules (see infra/README.md).
    PostgreSQL, Kafka, the API, the web app and k3s need about 4 GB at rest per environment; 8 GB leaves room for rollouts.
  EOT
  type = map(object({
    instance_type = string
    namespaces    = list(string)
  }))
  default = {
    dev = {
      instance_type = "t3.large"
      namespaces    = ["crm-dev"]
    }
    stage = {
      instance_type = "t3.large"
      namespaces    = ["crm-stage"]
    }
  }
}
